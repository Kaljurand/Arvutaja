package ee.ioc.phon.android.unitconv;

import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grammaticalframework.Linearizer;
import org.grammaticalframework.PGF;
import org.grammaticalframework.PGFBuilder;
import org.grammaticalframework.Parser;
import org.grammaticalframework.parser.ParseState;
import org.grammaticalframework.Trees.Absyn.Tree;

import ee.ioc.phon.android.unitconv.provider.Qeval;
import ee.ioc.phon.android.unitconv.provider.Query;


public class Unitconv extends AbstractRecognizerActivity {

	// Set of non-standard extras that RecognizerIntentActivity supports
	public static final String EXTRA_GRAMMAR_URL = "EXTRA_GRAMMAR_URL";
	public static final String EXTRA_GRAMMAR_LANG = "EXTRA_GRAMMAR_LANG";

	private static final Uri QUERY_CONTENT_URI = Query.Columns.CONTENT_URI;
	private static final Uri QEVAL_CONTENT_URI = Qeval.Columns.CONTENT_URI;

	private SharedPreferences mPrefs;

	private String mLangParse;
	private String mLangLinearize;

	private ExpandableListView mListView;
	private EditText mEt;
	private PGF mPGF;
	private Intent mIntent;
	private ImageButton mBMicrophone;

	private MyExpandableListAdapter mAdapter;
	private QueryHandler mQueryHandler;

	private boolean mUseInternalTranslator = true;

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

		mEt = (EditText) findViewById(R.id.edittext);

		mBMicrophone = (ImageButton) findViewById(R.id.buttonMicrophone);

		String nameRecognizerPkg = getString(R.string.nameRecognizerPkg);
		String nameRecognizerCls = getString(R.string.nameRecognizerCls);

		mLangParse = getString(R.string.nameGrammar) + getString(R.string.nameLangParse);
		mLangLinearize = getString(R.string.nameGrammar) + getString(R.string.nameLangLinearize);

		mIntent = createRecognizerIntent(getString(R.string.defaultGrammar), getString(R.string.nameLangLinearize), mUseInternalTranslator);
		mIntent.setComponent(new ComponentName(nameRecognizerPkg, nameRecognizerCls));

		if (getRecognizers(mIntent).size() == 0) {
			mBMicrophone.setEnabled(false);
			toast(String.format(getString(R.string.errorRecognizerNotPresent), nameRecognizerCls));
		}

		if (mUseInternalTranslator) {
			new LoadPGFTask().execute();
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
				String view = cursor.getString(cursor.getColumnIndex(Query.Columns.VIEW));
				String translation = cursor.getString(cursor.getColumnIndex(Query.Columns.TRANSLATION));
				launchIntent(view, translation);
				return false;
			}
		});


		// TODO: make this work, it currently does not
		mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Cursor cursor = (Cursor) parent.getItemAtPosition(childPosition);
				String view = cursor.getString(cursor.getColumnIndex(Qeval.Columns.VIEW));
				String translation = cursor.getString(cursor.getColumnIndex(Qeval.Columns.TRANSLATION));
				launchIntent(view, translation);
				return false;
			}
		});


		mAdapter = new MyExpandableListAdapter(
				this,
				R.layout.list_item_unitconv_result,
				R.layout.list_item_unitconv_result,
				new String[] { Query.Columns.TRANSLATION, Query.Columns.EVALUATION, Query.Columns.VIEW, Query.Columns.MESSAGE },
				new int[] { R.id.list_item_translation, R.id.list_item_evaluation, R.id.list_item_view, R.id.list_item_message },
				new String[] { Qeval.Columns.TRANSLATION, Qeval.Columns.EVALUATION, Qeval.Columns.VIEW, Qeval.Columns.MESSAGE },
				new int[] { R.id.list_item_translation, R.id.list_item_evaluation, R.id.list_item_view, R.id.list_item_message }
		);

		mListView.setAdapter(mAdapter);

		mQueryHandler = new QueryHandler(this, mAdapter);

		mQueryHandler.startQuery(
				TOKEN_GROUP,
				null,
				QUERY_CONTENT_URI,
				QUERY_PROJECTION,
				null,
				null,
				Query.Columns.TIMESTAMP + " DESC"
		);
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

		case R.id.menuSettings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	protected void onSuccess(List<String> matches) {
		if (matches.isEmpty()) {
			toast("ERROR: empty list was returned not an error message.");
		} else {
			String result = matches.iterator().next();
			mEt.setText(result);
			new TranslateTask().execute(matches);
		}
	}


	private void launchIntent(String view, String translation) {
		if (view != null && view.startsWith("http://")) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(view)));
		} else if (translation != null) {
			Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
			search.putExtra(SearchManager.QUERY, translation);
			startActivity(search);
		}
	}


	private static Intent createRecognizerIntent(String grammar, String langLinearize, boolean useInternalTranslator) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(EXTRA_GRAMMAR_URL, grammar);
		if (! useInternalTranslator) {
			intent.putExtra(EXTRA_GRAMMAR_LANG, langLinearize);
		}
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		return intent;
	}


	private class LoadPGFTask extends AsyncTask<Void, Void, PGF> {

		private ProgressDialog mProgress;

		protected void onPreExecute() {
			mProgress = ProgressDialog.show(Unitconv.this, "", getString(R.string.progressLoadingGrammar), true);
		}

		protected PGF doInBackground(Void... a) {
			InputStream is = getResources().openRawResource(R.raw.grammar);
			try {
				PGF pgf = PGFBuilder.fromInputStream(is, new String[] {mLangParse, mLangLinearize});
				return pgf;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected void onPostExecute(PGF result) {
			mPGF = result;
			if (mProgress != null) {
				mProgress.dismiss();
			}
		}
	}


	private class TranslateTask extends AsyncTask<List<String>, Void, List<Map<String, String>>> {

		private ProgressDialog mProgress;

		protected void onPreExecute() {
			if (mUseInternalTranslator) {
				mProgress = ProgressDialog.show(Unitconv.this, "", getString(R.string.progressExecuting), true);
			}
		}

		protected List<Map<String, String>> doInBackground(List<String>... s) {
			if (mUseInternalTranslator) {
				return getResultsWithInternalTranslator(s[0].get(0));
			}
			return getResultsWithExternalTranslator(s);
		}


		private List<Map<String, String>> getResultsWithInternalTranslator(String s) {
			try {
				// Creating a Parser object for the P_LANG concrete grammar
				Parser mParser = new Parser(mPGF, mLangParse);
				// Simple tokenization
				String[] tokens = s.split(" ");
				// Parsing the tokens
				ParseState mParseState = mParser.parse(tokens);
				Tree[] trees = (Tree[]) mParseState.getTrees();

				int numberOfTrees = trees.length;
				List<Map<String, String>> translations = new ArrayList<Map<String, String>>();

				if (numberOfTrees == 0) {
				} else {
					// Creating a Linearizer object for the L_LANG concrete grammar
					// Linearizing all the trees (i.e. the ambiguity)
					Linearizer mLinearizer = new Linearizer(mPGF, mLangLinearize);
					for (int i = 0; i < numberOfTrees; i++) {
						Map<String, String> map = new HashMap<String, String>();
						Converter conv = null;
						try {
							String t = mLinearizer.linearizeString(trees[i]);
							conv = new Converter(t);
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
							map.put("out", getString(R.string.error));
						}
						translations.add(map);
					}
				}
				return translations;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
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
						map.put("out", getString(R.string.error));
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


	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

		// Note that the constructor does not take a Cursor. This is done to avoid querying the 
		// database on the main thread.
		public MyExpandableListAdapter(Context context, int groupLayout,
				int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
				int[] childrenTo) {

			/*
			 * 
context	The context where the ExpandableListView associated with this SimpleCursorTreeAdapter is running
cursor	The database cursor
groupLayout	The resource identifier of a layout file that defines the views for a group.
            The layout file should include at least those named views defined in groupTo.
groupFrom	A list of column names that will be used to display the data for a group.
groupTo	The group views (from the group layouts) that should display column in the "from" parameter.
These should all be TextViews or ImageViews.
The first N views in this list are given the values of the first N columns in the from parameter.

childLayout	The resource identifier of a layout file that defines the views for a child. The layout file should include at least those named views defined in childTo.
childFrom	A list of column names that will be used to display the data for a child.
childTo	The child views (from the child layouts) that should display column in the "from" parameter. These should all be TextViews or ImageViews. The first N views in this list are given the values of the first N columns in the from parameter.
			 */
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
