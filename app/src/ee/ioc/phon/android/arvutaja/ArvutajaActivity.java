package ee.ioc.phon.android.arvutaja;

/*
 * Copyright 2011-2012, Institute of Cybernetics at Tallinn University of Technology
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
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import ee.ioc.phon.android.arvutaja.command.Command;
import ee.ioc.phon.android.arvutaja.command.CommandParseException;
import ee.ioc.phon.android.arvutaja.command.CommandParser;
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

	private static String mCurrentSortOrder;

	private ExpandableListView mListView;
	private Intent mIntent;

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

		String nameRecognizerPkg = getString(R.string.nameRecognizerPkg);
		String nameRecognizerCls = getString(R.string.nameRecognizerCls);

		mIntent = createRecognizerIntent(getString(R.string.defaultGrammar), getString(R.string.nameLangLinearize), mUseInternalTranslator);
		mIntent.setComponent(new ComponentName(nameRecognizerPkg, nameRecognizerCls));


		final LinearLayout mLlMicrophone = (LinearLayout) findViewById(R.id.llMicrophone);
		if (getIntentActivities(mIntent).size() == 0) {
			mLlMicrophone.setVisibility(View.GONE);
			toast(String.format(getString(R.string.errorRecognizerNotPresent), nameRecognizerCls));
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.urlSpeakDownload))));
			finish();
		} else {
			mLlMicrophone.setVisibility(View.VISIBLE);
		}

		mListView = (ExpandableListView) findViewById(R.id.list);
		mListView.setGroupIndicator(getResources().getDrawable(R.drawable.list_selector_expandable));

		mListView.setFastScrollEnabled(true);
		mListView.setClickable(true);

		mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Cursor cursor = (Cursor) parent.getExpandableListAdapter().getGroup(groupPosition);
				launchIntent(cursor, Query.Columns.TRANSLATION);
				return false;
			}
		});

		mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Cursor cursor = (Cursor) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
				launchIntent(cursor, Qeval.Columns.TRANSLATION);
				return false;
			}
		});

		mListView.setOnScrollListener(new OnScrollListener() {
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					mLlMicrophone.setVisibility(View.VISIBLE);
				} else {
					mLlMicrophone.setVisibility(View.GONE);
				}
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

		startQuery(mPrefs.getString(getString(R.string.prefCurrentSortOrder), Query.Columns.TIMESTAMP + " DESC"));
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
		// We assume that the microphone button triggers an activity that "pauses" Arvutaja.
		// 1. The button appears when Arvutaja is fully in focus.
		// 2. As soon as the button is tapped it becomes disabled.
		// 3. It becomes enabled again when Arvutaja gets the focus back.
		ImageButton mic = (ImageButton) findViewById(R.id.buttonMicrophone);
		mic.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.setOnClickListener(null);
				launchRecognizerIntent(mIntent);
			}
		});
	}


	@Override
	public void onStop() {
		super.onStop();
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
		editor.commit();
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
		case R.id.menuMainSettings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.menuMainAbout:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.urlArvutajaHelp))));
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
		final long key;
		String fname;
		final Uri uri;

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getExpandableListAdapter().getChild(groupPos, childPos);
			key = cursor.getLong(cursor.getColumnIndex(Qeval.Columns._ID));
			fname = cursor.getString(cursor.getColumnIndex(Qeval.Columns.TRANSLATION));
			uri = QEVAL_CONTENT_URI;
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getExpandableListAdapter().getGroup(groupPos);
			key = cursor.getLong(cursor.getColumnIndex(Query.Columns._ID));
			fname = cursor.getString(cursor.getColumnIndex(Query.Columns.TRANSLATION));
			uri = QUERY_CONTENT_URI;
		} else {
			return false;
		}

		if (fname == null) {
			fname = getString(R.string.ambiguous);
		}

		switch (item.getItemId()) {
		case R.id.cmMainDelete:
			Utils.getYesNoDialog(
					this,
					String.format(getString(R.string.confirmDeleteEntry), fname),
					new Executable() {
						public void execute() {
							delete(uri, key);
						}
					}
					).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	@Override
	protected void onSuccess(List<String> matches) {
		new TranslateTask().execute(matches);
	}


	private void launchIntent(Cursor cursor, String translation) {
		launchIntent(cursor.getString(cursor.getColumnIndex(translation)));
	}


	private void launchIntent(String commandAsString) {
		try {
			Command command = CommandParser.getCommand(this, commandAsString);
			Intent intent = command.getIntent();
			if (getIntentActivities(intent).size() == 0) {
				AlertDialog d = Utils.getDialog(this, command.getSuggestion());
				d.show();
				// Make the textview clickable. Must be called after show()
				((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			} else {
				startActivity(intent);
			}
		} catch (CommandParseException e) {
			toast(getString(R.string.errorCommandNotSupported));
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
					map.put("in", lin);
					Converter conv = new Converter(lin);
					try {
						map.put("out", conv.getOut());
					} catch (Exception e) {
						map.put("message", e.getMessage());
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
						values2.put(Qeval.Columns.MESSAGE, r.get("message"));
						insert(QEVAL_CONTENT_URI, values2);
					}
				}

				if (results.size() == 1 && mPrefs.getBoolean("keyUseExternalEvaluator", false)) {
					launchIntent(results.get(0).get("in"));
				}
			}
		}
	}


	private void startQuery(String sortOrder) {
		mCurrentSortOrder = sortOrder;
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
