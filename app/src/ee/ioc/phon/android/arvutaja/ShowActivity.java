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

import ee.ioc.phon.android.arvutaja.provider.Query;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

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

				long timestamp = c.getLong(c.getColumnIndex(Query.Columns.TIMESTAMP));
				Time time = new Time();
				time.set(timestamp);
				((TextView) findViewById(R.id.tvTimestamp)).setText(time.format2445());

				((TextView) findViewById(R.id.tvUtterance)).setText(c.getString(c.getColumnIndex(Query.Columns.UTTERANCE)));

				TextView tvTranslation = (TextView) findViewById(R.id.tvTranslation);
				String translation = c.getString(c.getColumnIndex(Query.Columns.TRANSLATION));
				if (translation == null || translation.length() == 0) {
					tvTranslation.setBackgroundResource(R.color.red);
				} else {
					tvTranslation.setText(translation);
				}

				TextView tvEvaluation = (TextView) findViewById(R.id.tvEvaluation);
				String evaluation = c.getString(c.getColumnIndex(Query.Columns.EVALUATION));
				if (evaluation == null || evaluation.length() == 0) {
				} else {
					tvEvaluation.setText(evaluation);
				}

				TextView tvView = (TextView) findViewById(R.id.tvView);
				String view = c.getString(c.getColumnIndex(Query.Columns.VIEW));
				if (view == null || view.length() == 0) {
					tvView.setVisibility(View.GONE);
				} else {
					tvView.setVisibility(View.VISIBLE);
					tvView.setText(view);
				}

				String message = c.getString(c.getColumnIndex(Query.Columns.MESSAGE));
				if (message == null || message.length() == 0) {
					tvMessage.setVisibility(View.GONE);
				} else {
					tvMessage.setVisibility(View.VISIBLE);
					tvMessage.setText(message);
				}
			}
			c.close();
		} catch (IllegalArgumentException e) {
			tvMessage.setVisibility(View.VISIBLE);
			tvMessage.setText(e.getMessage());
		}
	}
}