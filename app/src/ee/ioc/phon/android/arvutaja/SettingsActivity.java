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

package ee.ioc.phon.android.arvutaja;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

public class SettingsActivity extends SubActivity implements OnSharedPreferenceChangeListener {

	private SettingsFragment mSettingsFragment;
	private SharedPreferences mPrefs;
	private String mKeyLanguage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettingsFragment = new SettingsFragment();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mKeyLanguage = getString(R.string.keyLanguage);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();

		// TODO: we want to set the summary on creation, but for some reason the findPreference
		// returns null
		Preference pref = mSettingsFragment.findPreference(mKeyLanguage);
		if (pref != null) {
			pref.setSummary(mPrefs.getString(mKeyLanguage, getString(R.string.defaultLanguage)));
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals(mKeyLanguage)) {
			Preference pref = mSettingsFragment.findPreference(key);
			pref.setSummary(prefs.getString(key, ""));
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		mPrefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

}