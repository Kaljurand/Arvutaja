package ee.ioc.phon.android.unitconv;

import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView.OnEditorActionListener;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import java.io.InputStream;
import java.util.List;

import org.grammaticalframework.Linearizer;
import org.grammaticalframework.PGF;
import org.grammaticalframework.PGFBuilder;
import org.grammaticalframework.Parser;
import org.grammaticalframework.parser.ParseState;
import org.grammaticalframework.Trees.Absyn.Tree;

public class Unitconv extends AbstractRecognizerActivity {

	// Set of non-standard extras that RecognizerIntentActivity supports
	public static final String EXTRA_GRAMMAR_JSGF = "EXTRA_GRAMMAR_JSGF";

	// Note: make sure that the grammar has been registered with the server
	//private static final String GRAMMAR = "http://net-speech-api.googlecode.com/git-history/gf/lm/UnitconvEst.jsgf";

	private static final String P_LANG = "UnitconvEst";
	private static final String L_LANG = "UnitconvApp";

	private ArrayAdapter<String> mArrayAdapter;
	private EditText mEt;
	private PGF mPGF;
	private Intent mIntent;
	private ImageButton speakButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mEt = (EditText) findViewById(R.id.edittext);

		speakButton = (ImageButton) findViewById(R.id.buttonMicrophone);

		if (getRecognizers().size() == 0) {
			speakButton.setEnabled(false);
			toast(getString(R.string.errorRecognizerNotPresent));
		}

		new LoadPGFTask().execute();

		mArrayAdapter = new ArrayAdapter<String>(this, R.layout.listitem);
		ListView list = (ListView)findViewById(R.id.list);
		list.setAdapter(mArrayAdapter);

		mEt.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					new TranslateTask().execute(mEt.getText().toString());
				}
				return true;
			}
		});

		mIntent = createRecognizerIntent(getString(R.string.defaultGrammar));
	}


	public void recognize(View v) {
		launchRecognizerIntent(mIntent);
	}


	@Override
	protected void onSuccess(List<String> matches) {
		if (matches.isEmpty()) {
			toast("ERROR: empty list was returned not an error message.");
		} else {
			// TODO: support multiple results
			String result = matches.iterator().next();
			mEt.setText(result);
			new TranslateTask().execute(result);
		}
	}


	private static Intent createRecognizerIntent(String grammar) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(EXTRA_GRAMMAR_JSGF, grammar);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say e.g.: kaks meetrit jalgades");
		return intent;
	}


	private class LoadPGFTask extends AsyncTask<Void, Void, PGF> {

		private ProgressDialog progress;

		protected void onPreExecute() {
			this.progress =
				ProgressDialog.show(Unitconv.this, getString(R.string.labelApp), getString(R.string.progressLoadingGrammar), true);
		}

		protected PGF doInBackground(Void... a) {
			int pgf_res = R.raw.unitconv;
			InputStream is = getResources().openRawResource(pgf_res);
			try {
				PGF pgf = PGFBuilder.fromInputStream(is, new String[] {P_LANG, L_LANG});
				return pgf;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected void onPostExecute(PGF result) {
			mPGF = result;
			if (this.progress != null) {
				this.progress.dismiss();
			}
		}
	}


	private class TranslateTask extends AsyncTask<String, Void, String[]> {

		private ProgressDialog mProgress;

		protected void onPreExecute() {
			mProgress = ProgressDialog.show(Unitconv.this, getString(R.string.labelApp), getString(R.string.progressExecuting), true);
		}

		protected String[] doInBackground(String... s) {
			try {
				// Creating a Parser object for the P_LANG concrete grammar
				Parser mParser = new Parser(mPGF, P_LANG);
				// Simple tokenization
				String[] tokens = s[0].split(" ");
				// Parsing the tokens
				ParseState mParseState = mParser.parse(tokens);
				Tree[] trees = (Tree[]) mParseState.getTrees();

				int numberOfTrees = trees.length;
				String[] translations = new String[numberOfTrees];

				if (numberOfTrees == 0) {
				} else {
					// Creating a Linearizer object for the L_LANG concrete grammar
					// Linearizing all the trees (i.e. the ambiguity)
					Linearizer mLinearizer = new Linearizer(mPGF, L_LANG);
					for (int i = 0; i < numberOfTrees; i++) {
						try {
							String t = mLinearizer.linearizeString(trees[i]);
							translations[i] = Converter.convert(t) + " [" + t + "]";
						} catch (java.lang.Exception e) {
							translations[i] = getString(R.string.errorLinearizer);
						}
					}
				}
				return translations;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}


		protected void onPostExecute(String[] result) {
			if (mProgress != null) {
				mProgress.dismiss();
			}
			if (result.length == 0) {
				toast(getString(R.string.warningParserInputNotSupported));
			} else {
				mArrayAdapter.clear();
				for (String sentence : result) {
					mArrayAdapter.add(sentence);
				}
			}
		}
	}
}