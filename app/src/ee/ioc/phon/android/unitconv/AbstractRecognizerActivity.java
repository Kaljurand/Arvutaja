package ee.ioc.phon.android.unitconv;

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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.widget.Toast;

/**
 * <p>This demo shows how to create an input to RecognizerIntent.ACTION_RECOGNIZE_SPEECH
 * and how to respond to its output (list of matched words or an error code). This is
 * an abstract class, the UI part is in the extensions of this class.</p>
 * 
 * @author Kaarel Kaljurand
 */
public abstract class AbstractRecognizerActivity extends Activity {

	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	protected abstract void onSuccess(List<String> matches);

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
			switch (resultCode) {
			case RESULT_OK:
				ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				onSuccess(matches);
				break;
			case RecognizerIntent.RESULT_AUDIO_ERROR:
				toast(getString(R.string.errorResultAudioError));
				break;
			case RecognizerIntent.RESULT_CLIENT_ERROR:
				toast(getString(R.string.errorResultClientError));
				break;
			case RecognizerIntent.RESULT_NETWORK_ERROR:
				toast(getString(R.string.errorResultNetworkError));
				break;
			case RecognizerIntent.RESULT_SERVER_ERROR:
				toast(getString(R.string.errorResultServerError));
				break;
			case RecognizerIntent.RESULT_NO_MATCH:
				toast(getString(R.string.errorResultNoMatch));
				break;
			default:
				//toast("Not handling result code: " + resultCode);
			}
		} else {
			//toast("Not handling request code: " + requestCode);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}


	protected List<ResolveInfo> getRecognizers() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		return activities;
	}


	protected void launchRecognizerIntent(Intent intent) {
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}


	protected void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
}