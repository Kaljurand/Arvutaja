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

import ee.ioc.phon.android.arvutaja.command.Command;
import ee.ioc.phon.android.arvutaja.command.CommandParseException;
import ee.ioc.phon.android.arvutaja.command.CommandParser;
import ee.ioc.phon.android.arvutaja.provider.Query;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Shows the full DB record:
 *
 * TIMESTAMP
 * UTTERANCE
 * LANG
 * TRANSLATION (linearization), possibly missing
 * EVALUATION, possibly missing
 * TARGET_LANG
 * VIEW (show which external evaluator would be used, support clicking on it)
 * MESSAGE (show error message if present)
 */
public class ShowActivity extends AbstractRecognizerActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.show);

		final Uri contentUri = getIntent().getData();

		ImageButton bDelete = (ImageButton) findViewById(R.id.bDelete);
		bDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getContentResolver().delete(contentUri, null, null);
				finish();
			}});

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
				TextView tvLang = (TextView) findViewById(R.id.tvLang);
				String lang = c.getString(c.getColumnIndex(Query.Columns.LANG));
				tvLang.setText(lang);

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
						tvEvaluation.setVisibility(View.GONE);
						// If the internal evaluation is missing then show the internal error message (if present)
						// Actually, don't show the message because it looks scary and is not localized.
						/*
						String message = c.getString(c.getColumnIndex(Query.Columns.MESSAGE));
						if (message == null || message.length() == 0) {
						} else {
							tvMessage.setVisibility(View.VISIBLE);
							tvMessage.setText(message);
						}
						 */
					} else {
						// Otherwise show the internal evaluation
						tvEvaluation.setText(evaluation);
					}

					// Show a link to the external evaluator
					final TextView tvView = (TextView) findViewById(R.id.tvView);
					try {
						final Command command = CommandParser.getCommand(this, translation);
						final Intent intent = command.getIntent();
						if (getIntentActivities(intent).size() == 0) {
							tvView.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									AlertDialog d = Utils.getGoToStoreDialog(
											getApplicationContext(),
											getString(R.string.errorIntentActivityNotPresent),
											command.getSuggestion()
											);
									d.show();
									// Make the textview clickable. Must be called after show()
									((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
								}
							});
						} else {
							String uri = intent.getDataString();
							if (uri == null) {
								tvView.setText(intent.getAction());
							} else {
								tvView.setText(Uri.decode(uri));
							}
							llInterpretation.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									startForeignActivity(intent);
								}
							});
						}
					} catch (CommandParseException e) {
						toast(getString(R.string.errorCommandNotSupported));
					}
				}
			}
			c.close();
		} catch (IllegalArgumentException e) {
			tvMessage.setVisibility(View.VISIBLE);
			tvMessage.setText(e.getMessage());
		}
	}
}