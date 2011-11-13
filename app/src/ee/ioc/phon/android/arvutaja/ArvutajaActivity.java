package ee.ioc.phon.android.arvutaja;

/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView.OnEditorActionListener;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.ioc.phon.android.arvutaja.provider.Qeval;
import ee.ioc.phon.android.arvutaja.provider.Query;


public class ArvutajaActivity extends AbstractRecognizerActivity {

	public static final String EXTRA_LAUNCH_RECOGNIZER = "ee.ioc.phon.android.extra.LAUNCH_RECOGNIZER";

	// Set of non-standard extras that RecognizerIntentActivity supports
	public static final String EXTRA_GRAMMAR_URL = "ee.ioc.phon.android.extra.GRAMMAR_URL";
	public static final String EXTRA_GRAMMAR_TARGET_LANG = "ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG";

	private static final String LOG_TAG = ArvutajaActivity.class.getName();

	private static final Uri QUERY_CONTENT_URI = Query.Columns.CONTENT_URI;
	private static final Uri QEVAL_CONTENT_URI = Qeval.Columns.CONTENT_URI;

	private SharedPreferences mPrefs;

	private ExpandableListView mListView;
	private EditText mEt;
	private Intent mIntent;
	private ImageButton mBMicrophone;

	private MyExpandableListAdapter mAdapter;
	private QueryHandler mQueryHandler;

	private Bundle mExtras;

	private boolean mUseInternalTranslator = false;

	private static final String[] QUERY_PROJECTION = new String[] {
		Query.Columns._ID,
		Query.Columns.TIMESTAMP,
		Query.Columns.UTTERANCE,
		Query.Columns.TRANSLATION,
		Query.Columns.EVALUATION,
		Query.Columns.VIEW,
		Query.Columns.MESSAGE
	};

	private static final String[] QEVAL_PROJECTION = new String[] {
		Qeval.Columns._ID,
		Qeval.Columns.TIMESTAMP,
		Qeval.Columns.TRANSLATION,
		Qeval.Columns.EVALUATION,
		Qeval.Columns.VIEW,
		Qeval.Columns.MESSAGE
	};

	private static final int GROUP_TIMESTAMP_COLUMN_INDEX = 1;

	// Query identifiers for onQueryComplete
	private static final int TOKEN_GROUP = 0;
	private static final int TOKEN_CHILD = 1;

	private static final class QueryHandler extends AsyncQueryHandler {
		private CursorTreeAdapter mAdapter;

		public QueryHandler(Context context, CursorTreeAdapter adapter) {
			super(context.getContentResolver());
			this.mAdapter = adapter;
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			switch (token) {
			case TOKEN_GROUP:
				mAdapter.setGroupCursor(cursor);
				break;

			case TOKEN_CHILD:
				int groupPosition = (Integer) cookie;
				mAdapter.setChildrenCursor(groupPosition, cursor);
				break;
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mUseInternalTranslator = mPrefs.getBoolean("keyUseInternalTranslator", false);

		mExtras = getIntent().getExtras();
		if (mExtras == null) {
			// Sometimes getExtras() returns null, we map it
			// to an empty Bundle if this occurs.
			Log.e(LOG_TAG, "getExtras() == null");
			mExtras = new Bundle();
		} else {
			Log.e(LOG_TAG, "getExtras() == " + mExtras.keySet().toString());
		}

		mEt = (EditText) findViewById(R.id.edittext);

		mBMicrophone = (ImageButton) findViewById(R.id.buttonMicrophone);

		String nameRecognizerPkg = getString(R.string.nameRecognizerPkg);
		String nameRecognizerCls = getString(R.string.nameRecognizerCls);

		mIntent = createRecognizerIntent(getString(R.string.defaultGrammar), getString(R.string.nameLangLinearize), mUseInternalTranslator);
		mIntent.setComponent(new ComponentName(nameRecognizerPkg, nameRecognizerCls));

		if (getRecognizers(mIntent).size() == 0) {
			mBMicrophone.setEnabled(false);
			toast(String.format(getString(R.string.errorRecognizerNotPresent), nameRecognizerCls));
		}

		mListView = (ExpandableListView) findViewById(R.id.list);
		mListView.setGroupIndicator(getResources().getDrawable(R.drawable.list_selector_expandable));

		mEt.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					List<String> inputs = new ArrayList<String>();
					inputs.add(mEt.getText().toString());
					new TranslateTask().execute(inputs);
				}
				return true;
			}
		});

		mBMicrophone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchRecognizerIntent(mIntent);
			}
		});

		mListView.setClickable(true);

		mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Cursor cursor = (Cursor) parent.getItemAtPosition(groupPosition);
				launchIntent(cursor, Query.Columns.VIEW, Query.Columns.TRANSLATION);
				return false;
			}
		});


		// TODO: make this work, it currently does not
		mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Cursor cursor = (Cursor) parent.getItemAtPosition(childPosition);
				launchIntent(cursor, Qeval.Columns.VIEW, Qeval.Columns.TRANSLATION);
				return false;
			}
		});


		mAdapter = new MyExpandableListAdapter(
				this,
				R.layout.list_item_arvutaja_result,
				R.layout.list_item_arvutaja_result,
				new String[] { Query.Columns.TRANSLATION, Query.Columns.EVALUATION, Query.Columns.VIEW, Query.Columns.MESSAGE },
				new int[] { R.id.list_item_translation, R.id.list_item_evaluation, R.id.list_item_view, R.id.list_item_message },
				new String[] { Qeval.Columns.TRANSLATION, Qeval.Columns.EVALUATION, Qeval.Columns.VIEW, Qeval.Columns.MESSAGE },
				new int[] { R.id.list_item_translation, R.id.list_item_evaluation, R.id.list_item_view, R.id.list_item_message }
		);

		mListView.setAdapter(mAdapter);

		registerForContextMenu(mListView);

		mQueryHandler = new QueryHandler(this, mAdapter);

		startQuery(Query.Columns.TIMESTAMP + " DESC");
	}


	@Override
	public void onStart() {
		super.onStart();
		if (mExtras.getBoolean(ArvutajaActivity.EXTRA_LAUNCH_RECOGNIZER)) {
			launchRecognizerIntent(mIntent);
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		LinearLayout ll = (LinearLayout) findViewById(R.id.llEditor);

		if (mPrefs.getBoolean("keyShowEditor", false)) {
			ll.setVisibility(View.VISIBLE);
		} else {
			ll.setVisibility(View.GONE);
		}
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mAdapter.changeCursor(null);
		mAdapter = null;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuMainSortByTimestamp:
			startQuery(Query.Columns.TIMESTAMP + " DESC");
			return true;
		case R.id.menuMainSortByTranslation:
			startQuery(Query.Columns.TRANSLATION + " ASC");
			return true;
		case R.id.menuMainSortByEvaluation:
			startQuery(Query.Columns.EVALUATION + " DESC");
			return true;
			/*
		case R.id.menuMainSettings:
			startActivity(new Intent(this, Preferences.class));
			return true;
			 */
		case R.id.menuMainAbout:
			toast(getString(R.string.labelApp) + " v" + getVersionName());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cm_main, menu);
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			//int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			//int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
			toast("TODO: deleting children currently not supported");
			return true;
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getItemAtPosition(groupPos);
			final long key = cursor.getLong(cursor.getColumnIndex(Query.Columns._ID));
			String fname = cursor.getString(cursor.getColumnIndex(Query.Columns.TRANSLATION));
			switch (item.getItemId()) {
			case R.id.cmMainDelete:
				Utils.getYesNoDialog(
						this,
						String.format(getString(R.string.confirmDeleteEntry), fname),
						new Executable() {
							public void execute() {
								delete(QUERY_CONTENT_URI, key);
							}
						}
				).show();
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		}
		return false;
	}


	@Override
	protected void onSuccess(List<String> matches) {
		if (matches.isEmpty()) {
			toast("ERROR: empty list was returned, not an error message.");
		} else {
			String result = matches.iterator().next();
			mEt.setText(result);
			new TranslateTask().execute(matches);
		}
	}

	private void launchIntent(Cursor cursor, String view, String translation) {
		String v = cursor.getString(cursor.getColumnIndex(view));
		String t = cursor.getString(cursor.getColumnIndex(translation));
		if (v != null && v.startsWith("http://")) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(v)));
		} else if (t != null) {
			Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
			search.putExtra(SearchManager.QUERY, t);
			startActivity(search);
		}
	}


	private static Intent createRecognizerIntent(String grammar, String langLinearize, boolean useInternalTranslator) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(EXTRA_GRAMMAR_URL, grammar);
		if (! useInternalTranslator) {
			intent.putExtra(EXTRA_GRAMMAR_TARGET_LANG, langLinearize);
		}
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		return intent;
	}


	private class TranslateTask extends AsyncTask<List<String>, Void, List<Map<String, String>>> {

		private ProgressDialog mProgress;

		protected void onPreExecute() {
		}

		protected List<Map<String, String>> doInBackground(List<String>... s) {
			return getResultsWithExternalTranslator(s);
		}


		private List<Map<String, String>> getResultsWithExternalTranslator(List<String>... s) {
			try {
				List<Map<String, String>> translations = new ArrayList<Map<String, String>>();
				for (String lin : s[0]) {
					Map<String, String> map = new HashMap<String, String>();
					Converter conv = null;
					try {
						conv = new Converter(lin);
						map.put("in", conv.getIn());
						map.put("out", conv.getOut());
						map.put("view", conv.getView());
					} catch (Exception e) {
						if (conv == null) {
							map.put("in", e.getMessage());
						} else {
							map.put("in", conv.getIn());
							map.put("message", e.getMessage());
						}
					}
					translations.add(map);
				}
				return translations;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}


		protected void onPostExecute(List<Map<String, String>> results) {
			if (mProgress != null) {
				mProgress.dismiss();
			}
			if (results.isEmpty()) {
				toast(getString(R.string.warningParserInputNotSupported));
			} else {
				Time now = new Time();
				now.setToNow();
				long timestamp = now.toMillis(false);
				ContentValues values1 = new ContentValues();
				values1.put(Query.Columns.TIMESTAMP, timestamp);
				values1.put(Query.Columns.UTTERANCE, "[TODO: utterance]");
				if (results.size() == 1) {
					values1.put(Query.Columns.TRANSLATION, results.get(0).get("in"));
					values1.put(Query.Columns.EVALUATION, results.get(0).get("out"));
					values1.put(Query.Columns.VIEW, results.get(0).get("view"));
					values1.put(Query.Columns.MESSAGE, results.get(0).get("message"));
				} else {
					// TRANSLATION must remain NULL here
					values1.put(Query.Columns.EVALUATION, getString(R.string.ambiguous));
				}
				insert(QUERY_CONTENT_URI, values1);
				if (results.size() > 1) {
					for (Map<String, String> r : results) {
						ContentValues values2 = new ContentValues();
						values2.put(Qeval.Columns.TIMESTAMP, timestamp);
						values2.put(Qeval.Columns.TRANSLATION, r.get("in"));
						values2.put(Qeval.Columns.EVALUATION, r.get("out"));
						values2.put(Qeval.Columns.VIEW, r.get("view"));
						values2.put(Qeval.Columns.MESSAGE, r.get("message"));
						insert(QEVAL_CONTENT_URI, values2);
					}
				}
			}
		}
	}


	private void startQuery(String sortOrder) {
		mQueryHandler.startQuery(
				TOKEN_GROUP,
				null,
				QUERY_CONTENT_URI,
				QUERY_PROJECTION,
				null,
				null,
				sortOrder
		);
	}


	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

		// Note that the constructor does not take a Cursor. This is done to avoid querying the
		// database on the main thread.
		public MyExpandableListAdapter(Context context, int groupLayout,
				int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
				int[] childrenTo) {

			super(context, null, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			mQueryHandler.startQuery(
					TOKEN_CHILD,
					groupCursor.getPosition(),
					QEVAL_CONTENT_URI,
					QEVAL_PROJECTION,
					Qeval.Columns.TIMESTAMP + "=?",
					new String[] { "" + groupCursor.getLong(GROUP_TIMESTAMP_COLUMN_INDEX) },
					null
			);

			return null;
		}
	}
}
