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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;

public class SettingsActivity extends SubActivity implements OnSharedPreferenceChangeListener {

	private SettingsFragment mSettingsFragment;
	private SharedPreferences mPrefs;
	private String mKeyService;
	private String mKeyLanguage;

	// TODO: we support one service per package, this might
	// be a limitation...
	private final Map<String, String> mPkgToCls = new HashMap<String, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettingsFragment = new SettingsFragment();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mKeyService = getString(R.string.keyService);
		mKeyLanguage = getString(R.string.keyLanguage);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();
	}


	@Override
	protected void onResume() {
		super.onResume();
		mPrefs.registerOnSharedPreferenceChangeListener(this);

		populateServices();
	}


	@Override
	protected void onPause() {
		super.onPause();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);

		// Save the selected service class name, otherwise we cannot construct the
		//recognizer.
		String pkg = mPrefs.getString(getString(R.string.keyService), null);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefRecognizerServiceCls), mPkgToCls.get(pkg));
		editor.commit();
	}


	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals(mKeyService)) {
			ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
			pref.setSummary(pref.getEntry());
			populateLangs();
		} else if (key.equals(mKeyLanguage)) {
			ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
			pref.setSummary(pref.getEntry());
		}
	}


	private void populateServices() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> services = pm.queryIntentServices(
				new Intent(RecognitionService.SERVICE_INTERFACE), 0);

		String selectedService = mPrefs.getString(mKeyService, null);
		int selectedIndex = 0;

		CharSequence[] entries = new CharSequence[services.size()];
		CharSequence[] entryValues = new CharSequence[services.size()];

		int index = 0;
		for (ResolveInfo ri : services) {
			ServiceInfo si = ri.serviceInfo;
			if (si == null) {
				Log.i("serviceInfo == null");
				continue;
			}
			String pkg = si.packageName;
			String cls = si.name;
			CharSequence label = si.loadLabel(pm);
			mPkgToCls.put(pkg, cls);
			Log.i(pkg + " :: " + label + " :: " + mPkgToCls.get(pkg));
			entries[index] = label;
			entryValues[index] = pkg;
			if (pkg.equals(selectedService)) {
				selectedIndex = index;
			}
			index++;
		}

		ListPreference list = (ListPreference) mSettingsFragment.findPreference(mKeyService);
		list.setEntries(entries);
		list.setEntryValues(entryValues);
		list.setValueIndex(selectedIndex);
		list.setSummary(list.getEntry());

		populateLangs();
	}


	/**
	 * Update the list of languages by filling in those that the currently selected
	 * recognizer supports.
	 * 
	 * TODO: This works with Google and K6nele, but not with Vlingo.
	 */
	private void populateLangs() {
		ListPreference pref = (ListPreference) mSettingsFragment.findPreference(mKeyService);
		updateSupportedLanguages(pref.getValue());
	}


	/**
	 * Note: According to the <code>BroadcastReceiver</code> documentation,
	 * setPackage is respected only on ICS and later.
	 *
	 * @param packageName name of the app that is the only one to receive the broadcast
	 */
	private void updateSupportedLanguages(String packageName) {
		Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
		intent.setPackage(packageName);
		updateSupportedLanguages(intent);
	}


	/**
	 * Send a broadcast to find up what is the language preference of
	 * the speech recognizer service that matches the intent.
	 * The expectation is that only one service matches this intent.
	 */
	private void updateSupportedLanguages(Intent intent) {

		sendOrderedBroadcast(intent, null, new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				if (getResultCode() != Activity.RESULT_OK) {
					toast(getString(R.string.errorNoDefaultRecognizer));
					return;
				}

				Bundle results = getResultExtras(true);

				// Current list
				ListPreference list = (ListPreference) mSettingsFragment.findPreference(mKeyLanguage);
				String selectedLang = list.getValue();

				// Supported languages
				String prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
				ArrayList<CharSequence> allLangs = results.getCharSequenceArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

				Log.i("Supported langs: " + prefLang + ": " + allLangs);

				if (allLangs == null) {
					allLangs = new ArrayList<CharSequence>();
				}

				// Make sure we don't end up with an empty list of languages
				if (allLangs.isEmpty()) {
					if (prefLang == null) {
						allLangs.add(getString(R.string.defaultLanguage));
					} else {
						allLangs.add(prefLang);
					}
				}

				// Populate the entry values with the supported languages
				CharSequence[] entryValues = allLangs.toArray(new CharSequence[allLangs.size()]);
				list.setEntryValues(entryValues);

				// Populate the entries with human-readable language names
				CharSequence[] entries = new CharSequence[allLangs.size()];
				for (int i = 0; i < allLangs.size(); i++) {
					String ev = entryValues[i].toString();
					Locale l = new Locale(ev);
					entries[i] = l.getDisplayName(l) + " (" + ev + ")";
				}
				list.setEntries(entries);

				// Set the selected item
				if (allLangs.contains(selectedLang)) {
					list.setValue(selectedLang);
				} else if (prefLang != null) {
					list.setValue(prefLang);
				} else {
					list.setValueIndex(0);
				}
				// Update the summary to show the selected value
				list.setSummary(list.getEntry());

			}}, null, Activity.RESULT_OK, null, null);
	}
}