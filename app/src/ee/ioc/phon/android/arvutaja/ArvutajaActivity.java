package ee.ioc.phon.android.arvutaja;

/*
 * Copyright 2011-2013, Institute of Cybernetics at Tallinn University of Technology
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
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ee.ioc.phon.android.arvutaja.Constants.State;
import ee.ioc.phon.android.arvutaja.command.Command;
import ee.ioc.phon.android.arvutaja.command.CommandParseException;
import ee.ioc.phon.android.arvutaja.command.CommandParser;
import ee.ioc.phon.android.arvutaja.provider.Qeval;
import ee.ioc.phon.android.arvutaja.provider.Query;
import ee.ioc.phon.android.speechutils.TtsProvider;


public class ArvutajaActivity extends AbstractRecognizerActivity {

	public static final String EXTRA_LAUNCH_RECOGNIZER = "ee.ioc.phon.android.extra.LAUNCH_RECOGNIZER";

	// Set of non-standard extras that K6nele supports
	public static final String EXTRA_GRAMMAR_URL = "ee.ioc.phon.android.extra.GRAMMAR_URL";
	public static final String EXTRA_GRAMMAR_TARGET_LANG = "ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG";

	public static final String RESULTS_RECOGNITION_LINEARIZATIONS = "ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATIONS";
	public static final String RESULTS_RECOGNITION_LINEARIZATION_COUNTS = "ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATION_COUNTS";


	private State mState = State.INIT;

	private static final Uri QUERY_CONTENT_URI = Query.Columns.CONTENT_URI;
	private static final Uri QEVAL_CONTENT_URI = Qeval.Columns.CONTENT_URI;

	private static final String SORT_ORDER_TIMESTAMP = Query.Columns.TIMESTAMP + " DESC";
	private static final String SORT_ORDER_TRANSLATION = Query.Columns.TRANSLATION + " ASC";
	private static final String SORT_ORDER_EVALUATION = Query.Columns.EVALUATION + " DESC";

	private Resources mRes;
	private SharedPreferences mPrefs;
	private Vibrator mVibrator;

	private static String mCurrentSortOrder;

	private MicButton mButtonMicrophone;
	private ExpandableListView mListView;

	private MyExpandableListAdapter mAdapter;
	private QueryHandler mQueryHandler;

	private SpeechRecognizer mSr;
	private TtsProvider mTts;

	private static final String[] QUERY_PROJECTION = new String[] {
		Query.Columns._ID,
		Query.Columns.TIMESTAMP,
		Query.Columns.UTTERANCE,
		Query.Columns.TRANSLATION,
		Query.Columns.EVALUATION
	};

	private static final String[] QEVAL_PROJECTION = new String[] {
		Qeval.Columns._ID,
		Qeval.Columns.TIMESTAMP,
		Qeval.Columns.UTTERANCE,
		Qeval.Columns.TRANSLATION,
		Qeval.Columns.EVALUATION
	};

	private static final int GROUP_TIMESTAMP_COLUMN_INDEX = 1;

	// Query identifiers for onQueryComplete
	private static final int TOKEN_GROUP = 0;
	private static final int TOKEN_CHILD = 1;


	private static final class QueryHandler extends AsyncQueryHandler {
		private final WeakReference<ArvutajaActivity> mRef;
		private CursorTreeAdapter mAdapter;

		public QueryHandler(ArvutajaActivity activity, CursorTreeAdapter adapter) {
			super(activity.getContentResolver());
			mRef = new WeakReference<ArvutajaActivity>(activity);
			mAdapter = adapter;
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
			updateUi();
		}

		protected void onDeleteComplete(int token, Object cookie, int result) {
			updateUi();
		}

		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			updateUi();
			// TODO: This should be done in a better way.
			// The idea is that if insert was called with cookie set to "true",
			// then we launch the Show-activity to display the inserted entry.
			ArvutajaActivity outerClass = mRef.get();
			if (outerClass != null && cookie != null && cookie instanceof Boolean) {
				Boolean cookieAsBoolean = (Boolean) cookie;
				if (cookieAsBoolean.booleanValue()) {
					outerClass.showDetails(uri);
				}
			}
		}

		public void insert(Uri contentUri, ContentValues values) {
			startInsert(1, null, contentUri, values);
		}

		public void insert(Uri contentUri, ContentValues values, boolean displayEntry) {
			startInsert(1, displayEntry, contentUri, values);
		}

		public void delete(Uri contentUri, long key) {
			Uri uri = ContentUris.withAppendedId(contentUri, key);
			startDelete(1, null, uri, null, null);
		}

		private void updateUi() {
			ArvutajaActivity outerClass = mRef.get();
			if (outerClass != null) {
				int count = mAdapter.getGroupCount();
				outerClass.getActionBar().setSubtitle(
						outerClass.getResources().getQuantityString(R.plurals.numberOfInputs, count, count));
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRes = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Some devices (NOOK) do not have a vibrator
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (mVibrator != null && ! mVibrator.hasVibrator()) {
			mVibrator = null;
		}

		mButtonMicrophone = (MicButton) findViewById(R.id.buttonMicrophone);


		mListView = (ExpandableListView) findViewById(R.id.list);

		mListView.setGroupIndicator(getResources().getDrawable(R.drawable.list_selector_expandable));


		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View _view, int position, long _id) {
				ExpandableListView listView = (ExpandableListView) parent;

				// Converts a flat list position (the raw position of an item (child or group) in the list)
				// to a group and/or child position (represented in a packed position).
				long packedPosition = listView.getExpandableListPosition(position);
				Cursor cursor = null;
				final Uri contentUri;
				final long key;
				String translation = null;
				if (ExpandableListView.getPackedPositionType(packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {			
					cursor = (Cursor) listView.getExpandableListAdapter().getGroup(position);
					if (cursor == null) {
						return false;
					}
					key = cursor.getLong(cursor.getColumnIndex(Query.Columns._ID));
					contentUri = QUERY_CONTENT_URI;
					translation = cursor.getString(cursor.getColumnIndex(Query.Columns.TRANSLATION));
				} else if (ExpandableListView.getPackedPositionType(packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
					int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
					int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
					cursor = (Cursor) listView.getExpandableListAdapter().getChild(groupPosition, childPosition);
					if (cursor == null) {
						return false;
					}
					key = cursor.getLong(cursor.getColumnIndex(Qeval.Columns._ID));
					contentUri = QEVAL_CONTENT_URI;
					translation = cursor.getString(cursor.getColumnIndex(Qeval.Columns.TRANSLATION));
				} else {
					return false;
				}

				String message = null;
				if (translation == null) {
					message = getString(R.string.confirmDeleteMultiEntry);
				} else {
					message = String.format(getString(R.string.confirmDeleteEntry), translation);
				}
				Utils.getYesNoDialog(
						ArvutajaActivity.this,
						message,
						new Executable() {
							public void execute() {
								mQueryHandler.delete(contentUri, key);
							}
						}
						).show();
				return true;
			}
		});


		mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Cursor cursor = (Cursor) parent.getExpandableListAdapter().getGroup(groupPosition);

				String translation = cursor.getString(cursor.getColumnIndex(Query.Columns.TRANSLATION));
				if (translation != null) {
					long key = cursor.getLong(cursor.getColumnIndex(Query.Columns._ID));
					showDetails(ContentUris.withAppendedId(QUERY_CONTENT_URI, key));
				}
				return false;
			}
		});

		mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Cursor cursor = (Cursor) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
				long key = cursor.getLong(cursor.getColumnIndex(Qeval.Columns._ID));
				showDetails(ContentUris.withAppendedId(QEVAL_CONTENT_URI, key));
				return false;
			}
		});

		mListView.setOnScrollListener(new OnScrollListener() {
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					mButtonMicrophone.fadeIn();
				} else {
					mButtonMicrophone.fadeOut();
				}
			}
		});


		mAdapter = new MyExpandableListAdapter(
				this,
				R.layout.list_item_group,
				R.layout.list_item_child,
				new String[] { Query.Columns.UTTERANCE, Query.Columns.TRANSLATION, Query.Columns.EVALUATION },
				new int[] { R.id.list_item_utterance, R.id.list_item_translation, R.id.list_item_evaluation },
				new String[] { Qeval.Columns.UTTERANCE, Qeval.Columns.TRANSLATION, Qeval.Columns.EVALUATION },
				new int[] { R.id.list_item_utterance, R.id.list_item_translation, R.id.list_item_evaluation }
				);

		mListView.setAdapter(mAdapter);

		registerForContextMenu(mListView);

		getActionBar().setHomeButtonEnabled(false);

		mQueryHandler = new QueryHandler(this, mAdapter);

		startQuery(mPrefs.getString(getString(R.string.prefCurrentSortOrder), SORT_ORDER_TIMESTAMP));
	}


	/**
	 * We initialize the speech recognizer here, assuming that the configuration
	 * changed after onStop. That is why onStop destroys the recognizer.
	 */
	@Override
	public void onStart() {
		super.onStart();

		if (mPrefs.getBoolean(getString(R.string.prefFirstTime), true)) {
			if (isRecognizerInstalled(getString(R.string.nameK6nelePkg), getString(R.string.nameK6neleCls))) {
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putString(getString(R.string.keyService), getString(R.string.nameK6nelePkg));
				editor.putString(getString(R.string.prefRecognizerServiceCls), getString(R.string.nameK6neleService));
				editor.putBoolean(getString(R.string.prefFirstTime), false);
				editor.commit();
				AlertDialog d = Utils.getOkDialog(
						this,
						getString(R.string.msgFoundK6nele)
						);
				d.show();
			} else {
				// This can have 3 outcomes: K6nele gets installed, "Later" is pressed, "Never" is pressed.
				// In the latter case we set prefFirstTime = false, so that this dialog is not shown again.
				goToStore();
			}
		}

		ComponentName serviceComponent = getServiceComponent();

		if (serviceComponent == null) {
			toast(getString(R.string.errorNoDefaultRecognizer));
			goToStore();
		} else {
			Log.i("Starting service: " + serviceComponent);
			mSr = SpeechRecognizer.createSpeechRecognizer(this, serviceComponent);
			if (mSr == null) {
				toast(getString(R.string.errorNoDefaultRecognizer));
			} else {
				final String lang = mPrefs.getString(getString(R.string.keyLanguage), getString(R.string.defaultLanguage));
				mTts = new TtsProvider(this, new TextToSpeech.OnInitListener() {
					@Override
					public void onInit(int status) {
						getActionBar().setTitle(getString(R.string.labelApp) + " (" + lang + ")");
						if (status == TextToSpeech.SUCCESS) {
							Locale locale = mTts.chooseLanguage(lang);
							if (locale == null) {
								toast(String.format(getString(R.string.errorTtsLangNotAvailable), lang));
							} else {
								mTts.setLanguage(locale);
								// TODO: temporary
								say(String.format(getString(R.string.ttsTtsLangAvailable), locale.getDisplayLanguage(locale)));
							}
						} else {
							toast(getString(R.string.errorTtsInitError));
							Log.e(getString(R.string.errorTtsInitError));
						}
					}
				});
				Intent intentRecognizer = createRecognizerIntent(
						lang,
						getString(R.string.defaultGrammar),
						getString(R.string.nameLangLinearize));
				setUpRecognizerGui(mSr, intentRecognizer);
			}
		}
	}


	@Override
	public void onResume() {
		super.onResume();
	}


	@Override
	public void onStop() {
		super.onStop();

		if (mSr != null) {
			mSr.cancel(); // TODO: do we need this, we do destroy anyway?
			mSr.destroy();
		}

		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
		editor.commit();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSr != null) {
			mSr.destroy();
			mSr = null;
		}
		// Stop TTS
		if (mTts != null) {
			mTts.shutdown();
		}
		mAdapter.changeCursor(null);
		mAdapter = null;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		// Indicate the current sort order by checking the corresponding radio button
		int id = mPrefs.getInt(getString(R.string.prefCurrentSortOrderMenu), R.id.menuMainSortByTimestamp);
		MenuItem menuItem = menu.findItem(id);
		menuItem.setChecked(true);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuMainSortByTimestamp:
			sort(item, SORT_ORDER_TIMESTAMP);
			return true;
		case R.id.menuMainSortByTranslation:
			sort(item, SORT_ORDER_TRANSLATION);
			return true;
		case R.id.menuMainSortByEvaluation:
			sort(item, SORT_ORDER_EVALUATION);
			return true;
		case R.id.menuMainExamples:
			startActivity(new Intent(this, ExamplesActivity.class));
			return true;
		case R.id.menuMainSettings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	private void sort(MenuItem item, String sortOrder) {
		startQuery(sortOrder);
		item.setChecked(true);
		// Save the ID of the selected item.
		// TODO: ideally this should be done in onDestory
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt(getString(R.string.prefCurrentSortOrderMenu), item.getItemId());
		editor.commit();
	}


	private void onSuccess(String lang, Bundle bundle) {
		ArrayList<String> lins = bundle.getStringArrayList(RESULTS_RECOGNITION_LINEARIZATIONS);
		ArrayList<Integer> counts = bundle.getIntegerArrayList(RESULTS_RECOGNITION_LINEARIZATION_COUNTS);
		ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		// TODO: confidence scores support is only in API 14

		if (lins == null || lins.isEmpty() || counts == null || counts.isEmpty()) {
			if (matches == null || matches.isEmpty()) {
				showErrorDialog(R.string.errorResultNoMatch);
				return;
			}
			lins = new ArrayList<String>();
			counts = new ArrayList<Integer>();
			for (String match : matches) {
				lins.add(match); // utterance
				lins.add(match); // translation
				lins.add("GVS"); // target language
				counts.add(1);
			}
		}
		processResults(lang, lins, counts);
	}


	private void launchIntent(String commandAsString) {
		try {
			Command command = CommandParser.getCommand(this, commandAsString);
			Intent intent = command.getIntent();
			if (getIntentActivities(intent).size() == 0) {
				AlertDialog d = Utils.getGoToStoreDialog(
						this,
						getString(R.string.errorIntentActivityNotPresent),
						command.getSuggestion()
						);
				d.show();
				// Make the textview clickable. Must be called after show()
				((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			} else {
				startForeignActivity(intent);
			}
		} catch (CommandParseException e) {
			toast(getString(R.string.errorCommandNotSupported));
		}
	}


	private Intent createRecognizerIntent(String langSource, String grammar, String langTarget) {
		Locale locale = new Locale(langSource);
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langSource);
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,
				Integer.parseInt(mPrefs.getString(getString(R.string.keyMaxResults), getString(R.string.defaultMaxResults))));
		intent.putExtra(EXTRA_GRAMMAR_URL, grammar);
		// Request the App-language and the TTS-language (e.g. Engtts).
		// We assume (in the following) that only a single variant is returned for both types of languages.
		intent.putExtra(EXTRA_GRAMMAR_TARGET_LANG, langTarget + "," + locale.getISO3Language() + "tts");
		return intent;
	}

	/**
	 * This is called after successful speech recognition to inform the user
	 * about the results and also store the results.
	 *
	 * We assume that each hypothesis comes with {@code 2*n} linearizations
	 * corresponding to {@code n} parse results of the raw recognition result.
	 * Each result has 2 linearizations (App and ???tts), in any order.
	 * We ignore that results where App is empty or is repeated.
	 *
	 * <ul>
	 *   <li>raw utterance of hypothesis 1
	 *   <li>linearization 1.1
	 *   <li>language code of linearization 1.1 (App)
	 *   <li>linearization 1.2
	 *   <li>language code of linearization 1.2 (Engtts)
	 *   <li>...
	 *   <li>raw utterance of hypothesis 2
	 *   <li>...
	 * </ul>
	 */
	private void processResults(String lang, List<String> lins, List<Integer> counts) {
		Time now = new Time();
		now.setToNow();
		long timestamp = now.toMillis(false);

		String ttsLang = (new Locale(lang).getISO3Language()) + "tts";
		String ttsPhrase = null;

		List<ContentValues> valuesList = new ArrayList<ContentValues>();
		Set<String> seen = new HashSet<String>();
		int begin = 0;
		for (Integer c : counts) {
			int end = begin + 1 + 2*c;
			String utterance = lins.get(begin);
			for (int pos = begin + 1; pos < end; pos = pos + 2) {
				String lin = lins.get(pos);
				String targetLang = lins.get(pos+1);

				// TODO: we currently only pick out the first TTS language linearization,
				// because in the case of several linearizations they are not currently spoken.
				if (ttsPhrase == null && ttsLang.equals(targetLang)) {
					ttsPhrase = lin;
					continue;
				}

				String key = lin + "|" + targetLang;
				if (lin.isEmpty() || seen.contains(key)) {
					continue;
				} else {
					seen.add(key);
				}

				ContentValues values = new ContentValues();
				values.put(Qeval.Columns.TIMESTAMP, timestamp);
				values.put(Qeval.Columns.UTTERANCE, utterance);
				values.put(Qeval.Columns.TRANSLATION, lin);
				values.put(Qeval.Columns.LANG, lang);
				values.put(Qeval.Columns.TARGET_LANG, targetLang);
				try {
					values.put(Qeval.Columns.EVALUATION, CommandParser.getCommand(getApplicationContext(), lin).getOut());
				} catch (Exception e) {
					// We store the exception message in the "message" field,
					// but it should not be shown to the user.
					values.put(Qeval.Columns.MESSAGE, e.getMessage());
				}
				valuesList.add(values);
			}
			begin = end;
		}

		if (valuesList.isEmpty()) {
			showErrorDialog(R.string.errorResultNoMatch);
			// TODO: use the speech input locale, not the GUI locale,
			// i.e. if the user speaks in English, then respond in English,
			// even though the (visual) GUI is in German.
			say(getString(R.string.errorResultNoMatch));
		} else if (valuesList.size() == 1) {
			// If the transcription is not ambiguous, and the user prefers to
			// evaluate using an external activity, then we launch it via an intent.
			boolean launchExternalEvaluator = mPrefs.getBoolean(
					getString(R.string.keyUseExternalEvaluator),
					mRes.getBoolean(R.bool.defaultUseExternalEvaluator));

			String ttsOutput = makeTtsOutput(
					ttsPhrase,
					valuesList.get(0).getAsString(Qeval.Columns.EVALUATION)
					);
			say(ttsOutput);
			mQueryHandler.insert(QUERY_CONTENT_URI, valuesList.get(0), ! launchExternalEvaluator);

			if (launchExternalEvaluator) {
				launchIntent(lins.get(1));
			}
		} else {
			say(String.format(getString(R.string.ttsAmbiguous), valuesList.size()));
			ContentValues values = new ContentValues();
			values.put(Query.Columns.TIMESTAMP, timestamp);
			// TRANSLATION must remain NULL here
			values.put(Query.Columns.EVALUATION, getString(R.string.ambiguous));
			mQueryHandler.insert(QUERY_CONTENT_URI, values);
			for (ContentValues cv : valuesList) {
				mQueryHandler.insert(QEVAL_CONTENT_URI, cv);
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

	private void say(String str) {
		if (mPrefs.getBoolean(getString(R.string.keyUseTts), mRes.getBoolean(R.bool.defaultUseTts))) {
			if (mTts == null) {
				// TODO: show Toast
			} else {
				// TODO: check if the TTS engine has been initialized
				mTts.say(str);
			}
		}
	}

	private String makeTtsOutput(String expression, String value) {
		return expression + " " + value;
	}

	private void setUpRecognizerGui(final SpeechRecognizer sr, final Intent intentRecognizer) {
		final AudioCue audioCue;

		if (mPrefs.getBoolean(getString(R.string.keyAudioCues), mRes.getBoolean(R.bool.defaultAudioCues))) {
			audioCue = new AudioCue(this);
		} else {
			audioCue = null;
		}

		sr.setRecognitionListener(new RecognitionListener() {

			@Override
			public void onBeginningOfSpeech() {
				mState = State.LISTENING;
			}

			@Override
			public void onBufferReceived(byte[] buffer) {
				// TODO maybe show buffer waveform
			}

			@Override
			public void onEndOfSpeech() {
				mState = State.TRANSCRIBING;
				mButtonMicrophone.setState(mState);
				if (audioCue != null) {
					audioCue.playStopSound();
				}
			}

			@Override
			public void onError(int error) {
				mState = State.ERROR;
				mButtonMicrophone.setState(mState);
				if (audioCue != null) {
					audioCue.playErrorSound();
				}
				switch (error) {
				case SpeechRecognizer.ERROR_AUDIO:
					showErrorDialog(R.string.errorResultAudioError);
					break;
				case SpeechRecognizer.ERROR_CLIENT:
					showErrorDialog(R.string.errorResultClientError);
					break;
				case SpeechRecognizer.ERROR_NETWORK:
					showErrorDialog(R.string.errorResultNetworkError);
					break;
				case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
					showErrorDialog(R.string.errorResultNetworkError);
					break;
				case SpeechRecognizer.ERROR_SERVER:
					showErrorDialog(R.string.errorResultServerError);
					break;
				case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
					showErrorDialog(R.string.errorResultServerError);
					break;
				case SpeechRecognizer.ERROR_NO_MATCH:
					showErrorDialog(R.string.errorResultNoMatch);
					break;
				case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
					showErrorDialog(R.string.errorResultNoMatch);
					break;
				case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
					// This is programmer error.
					break;
				default:
					break;
				}
			}

			@Override
			public void onEvent(int eventType, Bundle params) {
				// TODO ???
			}

			@Override
			public void onPartialResults(Bundle partialResults) {
				// ignore
			}

			@Override
			public void onReadyForSpeech(Bundle params) {
				mState = State.RECORDING;
				mButtonMicrophone.setState(mState);
			}

			@Override
			public void onResults(Bundle results) {
				mState = State.INIT;
				mButtonMicrophone.setState(mState);
				onSuccess(intentRecognizer.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE), results);
			}

			@Override
			public void onRmsChanged(float rmsdB) {
				mButtonMicrophone.setVolumeLevel(rmsdB);
			}
		});

		mButtonMicrophone.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					vibrate();
				}

				return false;
			}
		});

		mButtonMicrophone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mState == State.INIT || mState == State.ERROR) {
					if (audioCue != null) {
						audioCue.playStartSoundAndSleep();
					}
					sr.startListening(intentRecognizer);
				}
				else if (mState == State.RECORDING) {
					//sr.cancel();
				}
				else if (mState == State.TRANSCRIBING) {
					//sr.cancel();
				}
				else if (mState == State.LISTENING) {
					sr.stopListening();
				} else {
					// TODO: bad state to press the button
				}
			}
		});

		LinearLayout llMicrophone = (LinearLayout) findViewById(R.id.llMicrophone);
		llMicrophone.setVisibility(View.VISIBLE);
		llMicrophone.setEnabled(true);

		Intent intentArvutaja = getIntent();
		Bundle extras = intentArvutaja.getExtras();
		if (extras != null && extras.getBoolean(ArvutajaActivity.EXTRA_LAUNCH_RECOGNIZER)) {
			// We disable the extra so that it would not fire on orientation change.
			intentArvutaja.putExtra(ArvutajaActivity.EXTRA_LAUNCH_RECOGNIZER, false);
			setIntent(intentArvutaja);
			sr.startListening(intentRecognizer);
		}
	}

	private void vibrate() {
		if (mVibrator != null) {
			mVibrator.vibrate(30);
		}
	}


	private void showDetails(Uri uri) {
		Intent intent = new Intent(this, ShowActivity.class);
		intent.setData(uri);
		startActivity(intent);
	}


	private void goToStore() {
		AlertDialog d = Utils.getGoToStoreDialogWithThreeButtons(
				this,
				String.format(getString(R.string.errorRecognizerNotPresent), getString(R.string.nameRecognizer)),
				Uri.parse(getString(R.string.urlK6neleDownload))
				);
		d.show();
	}


	/**
	 * This is one way to find out if a specific recognizer is installed.
	 * Alternatively we could query the service.
	 */
	private boolean isRecognizerInstalled(String pkg, String cls) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.setComponent(new ComponentName(pkg, cls));
		return (getIntentActivities(intent).size() > 0);
	}


	/**
	 * Look up the default recognizer service in the preferences.
	 * If the default have not been set then set the first available
	 * recognizer as the default. If no recognizer is installed then
	 * return null.
	 */
	private ComponentName getServiceComponent() {
		String pkg = mPrefs.getString(getString(R.string.keyService), null);
		String cls = mPrefs.getString(getString(R.string.prefRecognizerServiceCls), null);
		if (pkg == null || cls == null) {
			List<ResolveInfo> services = getPackageManager().queryIntentServices(
					new Intent(RecognitionService.SERVICE_INTERFACE), 0);
			if (services.isEmpty()) {
				return null;
			}
			ResolveInfo ri = services.iterator().next();
			pkg = ri.serviceInfo.packageName;
			cls = ri.serviceInfo.name;
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(getString(R.string.keyService), pkg);
			editor.putString(getString(R.string.prefRecognizerServiceCls), cls);
			editor.commit();
		}
		return new ComponentName(pkg, cls);
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
