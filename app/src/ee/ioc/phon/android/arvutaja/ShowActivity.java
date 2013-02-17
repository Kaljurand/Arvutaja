/*
 * Copyright 2013, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.arvutaja;

import java.util.List;

import ee.ioc.phon.android.arvutaja.command.Command;
import ee.ioc.phon.android.arvutaja.command.CommandParseException;
import ee.ioc.phon.android.arvutaja.command.CommandParser;
import ee.ioc.phon.android.arvutaja.provider.Query;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TIMESTAMP
 * UTTERANCE
 * TRANSLATION (linearization)
 * EVALUATION (possibly missing)
 * VIEW (show which external evaluator would be used, support clicking on it)
 * MESSAGE (show error message if present)
 *
 * input language
 * output language
 */
public class ShowActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.show);

		Uri contentUri = getIntent().getData();

		TextView tvMessage = (TextView) findViewById(R.id.tvMessage);
		try {
			Cursor c = getContentResolver().query(contentUri, null, null, null, null);

			if (c.moveToFirst()) {
				// Column names are the same for Query and Qeval

				// Timestamp
				long timestamp = c.getLong(c.getColumnIndex(Query.Columns.TIMESTAMP));
				Time time = new Time();
				time.set(timestamp);
				((TextView) findViewById(R.id.tvTimestamp)).setText(time.format("%Y-%m-%d %H:%M:%S"));

				// Utterance
				((TextView) findViewById(R.id.tvUtterance)).setText(c.getString(c.getColumnIndex(Query.Columns.UTTERANCE)));
				TextView tvInputLang = (TextView) findViewById(R.id.tvInputLang);
				// TODO
				String inputLang = "et-EE"; //c.getString(c.getColumnIndex(Query.Columns.TARGET_LANG));
				tvInputLang.setText(inputLang);

				TextView tvTranslation = (TextView) findViewById(R.id.tvTranslation);
				LinearLayout llInterpretation = (LinearLayout) findViewById(R.id.llInterpretation);
				final String translation = c.getString(c.getColumnIndex(Query.Columns.TRANSLATION));
				if (translation == null || translation.length() == 0) {
					// If translation is missing, then only show a red error message
					llInterpretation.setVisibility(View.GONE);
					((TextView) findViewById(R.id.tvInterpretationMissing)).setVisibility(View.VISIBLE);
				} else {
					// otherwise show the translation
					llInterpretation.setVisibility(View.VISIBLE);
					tvTranslation.setText(translation);

					TextView tvTargetLang = (TextView) findViewById(R.id.tvTargetLang);
					String targetLang = c.getString(c.getColumnIndex(Query.Columns.TARGET_LANG));
					tvTargetLang.setText(targetLang);

					TextView tvEvaluation = (TextView) findViewById(R.id.tvEvaluation);
					String evaluation = c.getString(c.getColumnIndex(Query.Columns.EVALUATION));
					if (evaluation == null || evaluation.length() == 0) {
						// If the internal evaluation is missing then show the internal error message (if present)
						String message = c.getString(c.getColumnIndex(Query.Columns.MESSAGE));
						if (message == null || message.length() == 0) {
						} else {
							tvMessage.setVisibility(View.VISIBLE);
							tvMessage.setText(message);
						}
					} else {
						// Otherwise show the internal evaluation
						tvEvaluation.setText(evaluation);
					}

					// Show a link to the external evaluator
					// TODO: improve
					TextView tvView = (TextView) findViewById(R.id.tvView);
					tvView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							launchIntent(translation);
						}
					});
				}
			}
			c.close();
		} catch (IllegalArgumentException e) {
			tvMessage.setVisibility(View.VISIBLE);
			tvMessage.setText(e.getMessage());
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

	private List<ResolveInfo> getIntentActivities(Intent intent) {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		return activities;
	}


	private void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
}