package ee.ioc.phon.android.unitconv;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import java.io.InputStream;
import org.grammaticalframework.Linearizer;
import org.grammaticalframework.PGF;
import org.grammaticalframework.PGFBuilder;
import org.grammaticalframework.Parser;
import org.grammaticalframework.parser.ParseState;
import org.grammaticalframework.Trees.Absyn.Tree;

public class Unitconv extends Activity {

	private static final String P_LANG = "UnitconvEst";
	private static final String L_LANG = "UnitconvApp";

	private ArrayAdapter<String> mArrayAdapter;
	private PGF mPGF;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		new LoadPGFTask().execute();

		mArrayAdapter = new ArrayAdapter<String>(this, R.layout.listitem);
		ListView list = (ListView)findViewById(R.id.list);
		list.setAdapter(mArrayAdapter);
	}

	public void translate(View v) {
		TextView tv = (TextView)findViewById(R.id.edittext);
		String input = tv.getText().toString();
		new TranslateTask().execute(input);
	}


	private class LoadPGFTask extends AsyncTask<Void, Void, PGF> {

		private ProgressDialog progress;

		protected void onPreExecute() {
			this.progress =
				ProgressDialog.show(Unitconv.this, "Translate", "Loading grammar, please wait", true);
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

		private ProgressDialog progress;

		protected void onPreExecute() {
			this.progress =
				ProgressDialog.show(Unitconv.this, "Translate", "Parsing, please wait", true);
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

				String[] translations = new String[trees.length];
				// Creating a Linearizer object for the L_LANG concrete grammar
				Linearizer mLinearizer = new Linearizer(mPGF, L_LANG);
				// Linearizing all the trees (i.e. the ambiguity)
				for (int i = 0 ; i < trees.length ; i++) {
					try {
						String t = mLinearizer.linearizeString(trees[i]);
						translations[i] = t;
					} catch (java.lang.Exception e) {
						translations[i] = "/!\\ Linearization error";
					}
				}
				return translations;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected void onPostExecute(String[] result) {
			mArrayAdapter.clear();
			for (String sentence : result)
				mArrayAdapter.add(sentence);
			if (this.progress != null)
				this.progress.dismiss();
		}
	}
}
