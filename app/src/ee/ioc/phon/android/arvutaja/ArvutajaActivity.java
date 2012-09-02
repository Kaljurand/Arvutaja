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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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

	// Set of non-standard extras that K6nele supports
	public static final String EXTRA_GRAMMAR_URL = "ee.ioc.phon.android.extra.GRAMMAR_URL";
	public static final String EXTRA_GRAMMAR_TARGET_LANG = "ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG";

	private enum State { INIT, RECORDING, LISTENING, TRANSCRIBING, ERROR };

	private State mState = State.INIT;

	private static final Uri QUERY_CONTENT_URI = Query.Columns.CONTENT_URI;
	private static final Uri QEVAL_CONTENT_URI = Qeval.Columns.CONTENT_URI;

	private Resources mRes;
	private SharedPreferences mPrefs;

	private static String mCurrentSortOrder;

	private List<Drawable> mVolumeLevels;
	private ImageButton mButtonMicrophone;
	private ExpandableListView mListView;
	private Intent mIntentRecognizer;

	private MyExpandableListAdapter mAdapter;
	private QueryHandler mQueryHandler;

	private SpeechRecognizer mSr;

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
		setContentView(R.layout.main);

		mRes = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		String nameRecognizerPkg = getString(R.string.nameRecognizerPkg);
		String nameRecognizerCls = getString(R.string.nameRecognizerCls);

		mIntentRecognizer = createRecognizerIntent(
				mPrefs.getString(getString(R.string.keyLanguage), getString(R.string.defaultLanguage)),
				getString(R.string.defaultGrammar),
				getString(R.string.nameLangLinearize));
		mIntentRecognizer.setComponent(new ComponentName(nameRecognizerPkg, nameRecognizerCls));

		mButtonMicrophone = (ImageButton) findViewById(R.id.buttonMicrophone);

		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);

		mVolumeLevels = new ArrayList<Drawable>();
		mVolumeLevels.add(mRes.getDrawable(R.drawable.button_mic_recording_0));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.button_mic_recording_1));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.button_mic_recording_2));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.button_mic_recording_3));

		final LinearLayout llMicrophone = (LinearLayout) findViewById(R.id.llMicrophone);
		// TODO: improve the way we find out if K6nele is present
		// Some queries will work also with Google's recognizer without grammar application
		if (SpeechRecognizer.isRecognitionAvailable(this) && getIntentActivities(mIntentRecognizer).size() == 0) {
			mIntentRecognizer = null;
			llMicrophone.setEnabled(false);
			AlertDialog d = Utils.getGoToStoreDialog(
					this,
					String.format(getString(R.string.errorRecognizerNotPresent), getString(R.string.nameRecognizer)),
					Uri.parse(getString(R.string.urlSpeakDownload))
					);
			d.show();
		} else {
			mSr = SpeechRecognizer.createSpeechRecognizer(this);
			llMicrophone.setVisibility(View.VISIBLE);
			llMicrophone.setEnabled(true);
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
					llMicrophone.setVisibility(View.VISIBLE);
				} else {
					llMicrophone.setVisibility(View.GONE);
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
		Intent intentArvutaja = getIntent();
		Bundle extras = intentArvutaja.getExtras();
		if (mIntentRecognizer != null && extras != null && extras.getBoolean(ArvutajaActivity.EXTRA_LAUNCH_RECOGNIZER)) {
			// We disable the extra so that it would not fire on orientation change.
			intentArvutaja.putExtra(ArvutajaActivity.EXTRA_LAUNCH_RECOGNIZER, false);
			setIntent(intentArvutaja);

			launchSpeechRecognizer();
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		mButtonMicrophone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mState == State.INIT || mState == State.ERROR) {
					launchSpeechRecognizer();
				}
				else if (mState == State.LISTENING) {
					mSr.stopListening();
				} else {
					// TODO: bad state to press the button
				}
			}
		});
	}


	@Override
	public void onStop() {
		super.onStop();
		// TODO: cancel recognition
		mSr.cancel();

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
			startActivity(new Intent(this, SettingsActivity.class));
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

		String message = null;
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getExpandableListAdapter().getChild(groupPos, childPos);
			message = cursor.getString(cursor.getColumnIndex(Qeval.Columns.MESSAGE));
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getExpandableListAdapter().getGroup(groupPos);
			message = cursor.getString(cursor.getColumnIndex(Query.Columns.MESSAGE));
		}

		if (message == null || message.length() == 0) {
			MenuItem menuItem = menu.findItem(R.id.cmMainShowError);
			menuItem.setEnabled(false);
		}
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final long key;
		String fname;
		String message = "";
		final Uri uri;

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getExpandableListAdapter().getChild(groupPos, childPos);
			key = cursor.getLong(cursor.getColumnIndex(Qeval.Columns._ID));
			fname = cursor.getString(cursor.getColumnIndex(Qeval.Columns.TRANSLATION));
			message = cursor.getString(cursor.getColumnIndex(Qeval.Columns.MESSAGE));
			uri = QEVAL_CONTENT_URI;
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			Cursor cursor = (Cursor) mListView.getExpandableListAdapter().getGroup(groupPos);
			key = cursor.getLong(cursor.getColumnIndex(Query.Columns._ID));
			fname = cursor.getString(cursor.getColumnIndex(Query.Columns.TRANSLATION));
			message = cursor.getString(cursor.getColumnIndex(Query.Columns.MESSAGE));
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
		case R.id.cmMainShowError:
			toast(message);
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
		String commandAsString = cursor.getString(cursor.getColumnIndex(translation));
		if (commandAsString != null) {
			launchIntent(commandAsString);
		}
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
				startActivity(intent);
			}
		} catch (CommandParseException e) {
			toast(getString(R.string.errorCommandNotSupported));
		}
	}


	private static Intent createRecognizerIntent(String langSource, String grammar, String langTarget) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langSource);
		intent.putExtra(EXTRA_GRAMMAR_URL, grammar);
		intent.putExtra(EXTRA_GRAMMAR_TARGET_LANG, langTarget);
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
					try {
						map.put("out", CommandParser.getCommand(getApplicationContext(), lin).getOut());
					} catch (Exception e) {
						// We store the exception message in the "message" field,
						// but it should not be shown to the user.
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

				// If the transcription is not ambiguous, and the user prefers to
				// evaluate using an external activity, then we launch it via an intent.

				if (results.size() == 1) {
					boolean launchExternalEvaluator = mPrefs.getBoolean(
							getString(R.string.keyUseExternalEvaluator),
							mRes.getBoolean(R.bool.defaultUseExternalEvaluator));

					if (launchExternalEvaluator) {
						launchIntent(results.get(0).get("in"));
					}
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


	private void launchSpeechRecognizer() {

		mSr.setRecognitionListener(new RecognitionListener() {

			private int mVolumeLevel;

			@Override
			public void onBeginningOfSpeech() {
				// TODO start "speak now..." animation
				mState = State.LISTENING;
				mButtonMicrophone.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_mic_recording_0));
			}

			@Override
			public void onBufferReceived(byte[] buffer) {
				// TODO show buffer waveform
			}

			@Override
			public void onEndOfSpeech() {
				// TODO stop "recording..." animation
				mState = State.TRANSCRIBING;
				mButtonMicrophone.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_mic_transcribing));
			}

			@Override
			public void onError(int error) {
				mState = State.ERROR;
				switch (error) {
				case SpeechRecognizer.ERROR_AUDIO:
					toast(getString(R.string.errorResultAudioError));
					break;
				case SpeechRecognizer.ERROR_CLIENT:
					toast(getString(R.string.errorResultClientError));
					break;
				case SpeechRecognizer.ERROR_NETWORK:
					toast(getString(R.string.errorResultNetworkError));
					break;
				case SpeechRecognizer.ERROR_SERVER:
					toast(getString(R.string.errorResultServerError));
					break;
				case SpeechRecognizer.ERROR_NO_MATCH:
					toast(getString(R.string.errorResultNoMatch));
					break;
					// TODO: cover all errors
				default: // TODO
					break;
				}
				mButtonMicrophone.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_mic));
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
				// TODO ???
			}

			@Override
			public void onResults(Bundle results) {
				ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				// TODO: confidence scores support is only in API 14
				mState = State.INIT;
				mButtonMicrophone.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_mic));
				onSuccess(matches);
			}

			@Override
			public void onRmsChanged(float rmsdB) {
				// TODO: take these from some configuration
				float min = 15.f;
				float max = 30.f;

				final int maxLevel = mVolumeLevels.size() - 1;

				int index = (int) ((rmsdB - min) / (max - min) * maxLevel);
				final int volumeLevel = Math.min(Math.max(0, index), maxLevel);

				if (volumeLevel != mVolumeLevel) {
					mButtonMicrophone.setBackgroundDrawable(mVolumeLevels.get(volumeLevel));
					mVolumeLevel = volumeLevel;
				}
			}
		});
		mSr.startListening(mIntentRecognizer);
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
