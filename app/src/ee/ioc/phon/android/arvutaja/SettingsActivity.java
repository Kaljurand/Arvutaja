/*
 * Copyright 2011-2015, Institute of Cybernetics at Tallinn University of Technology
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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speechutils.RecognitionServiceManager;

public class SettingsActivity extends SubActivity implements OnSharedPreferenceChangeListener {
    private SettingsFragment mSettingsFragment;
    private SharedPreferences mPrefs;
    private String mKeyService;
    private String mKeyLanguage;
    private String mKeyMaxResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsFragment = new SettingsFragment();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mKeyService = getString(R.string.keyService);
        mKeyLanguage = getString(R.string.keyLanguage);
        mKeyMaxResults = getString(R.string.keyMaxResults);

        getFragmentManager().beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();
    }


    @Override
    protected void onResume() {
        super.onResume();

        RecognitionServiceManager mngr = new RecognitionServiceManager();
        ListPreference prefServices = (ListPreference) mSettingsFragment.findPreference(mKeyService);
        populateServices(prefServices, mngr.getServices(getPackageManager()));
        populateLangs();

        Preference pref = mSettingsFragment.findPreference(mKeyMaxResults);
        String maxResults = mPrefs.getString(mKeyMaxResults, getString(R.string.defaultMaxResults));
        setSummary(pref, R.plurals.summaryMaxResults, maxResults);

        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(mKeyService)) {
            ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
            pref.setSummary(pref.getEntry());
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(getString(R.string.keyService), pref.getValue());
            editor.apply();
            populateLangs();
        } else if (key.equals(mKeyLanguage)) {
            ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
            pref.setSummary(pref.getEntry());
        } else if (mKeyMaxResults.equals(key)) {
            ListPreference pref = (ListPreference) mSettingsFragment.findPreference(key);
            setSummary(pref, R.plurals.summaryMaxResults, pref.getEntry().toString());
        }
    }


    private void setSummary(Preference pref, int pluralsResource, String countAsString) {
        int count = Integer.parseInt(countAsString);
        pref.setSummary(getResources().getQuantityString(pluralsResource, count, count));
    }


    private void populateServices(ListPreference prefServices, List<String> services) {
        CharSequence[] entryValues = services.toArray(new CharSequence[services.size()]);
        Arrays.sort(entryValues);

        CharSequence[] entries = new CharSequence[services.size()];

        String selectedService = mPrefs.getString(mKeyService, null);
        int index = 0;
        int selectedIndex = 0;
        for (CharSequence service : entryValues) {
            entries[index] = RecognitionServiceManager.getServiceLabel(this, service.toString());
            if (service.equals(selectedService)) {
                selectedIndex = index;
            }
            index++;
        }

        prefServices.setEntries(entries);
        prefServices.setEntryValues(entryValues);
        prefServices.setValueIndex(selectedIndex);
        prefServices.setSummary(prefServices.getEntry());
    }


    /**
     * Update the list of languages by filling in those that the currently selected
     * recognizer supports.
     */
    private void populateLangs() {
        final ListPreference prefServices = (ListPreference) mSettingsFragment.findPreference(mKeyService);
        final ListPreference prefLanguages = (ListPreference) mSettingsFragment.findPreference(mKeyLanguage);
        RecognitionServiceManager mngr = new RecognitionServiceManager();
        mngr.populateCombos(this, prefServices.getValue(), new RecognitionServiceManager.Listener() {
            @Override
            public void onComplete(List<String> combos, Set<String> selectedCombos) {
                Set<String> languages = new HashSet<>();
                for (String combo : combos) {
                    String[] serviceAndLang = RecognitionServiceManager.getServiceAndLang(combo);
                    languages.add(serviceAndLang[1]);
                }
                if (languages.size() > 1) {
                    updateSupportedLanguages(prefLanguages, languages);
                }
            }
        });
    }


    private void updateSupportedLanguages(ListPreference prefLanguages, Set<String> languages) {
        String selectedLang = prefLanguages.getValue();
        // Populate the entry values with the supported languages
        CharSequence[] entryValues = languages.toArray(new CharSequence[languages.size()]);
        Arrays.sort(entryValues);
        prefLanguages.setEntryValues(entryValues);

        // Populate the entries with human-readable language names
        CharSequence[] entries = new CharSequence[languages.size()];
        int index = 0;
        for (CharSequence lang : entryValues) {
            entries[index] = RecognitionServiceManager.makeLangLabel(lang.toString());
            index++;
        }
        prefLanguages.setEntries(entries);

        // Select one entry, trying in this order:
        // 1. Select the previously selected language
        // 2. Select the language that Arvutaja prefers
        // 3. Select the first language
        if (languages.contains(selectedLang)) {
            prefLanguages.setValue(selectedLang);
        } else {
            String defaultLang = getString(R.string.defaultLanguage);
            if (languages.contains(defaultLang)) {
                prefLanguages.setValue(defaultLang);
            } else {
                prefLanguages.setValueIndex(0);
            }
        }
        // Update the summary to show the selected value
        prefLanguages.setSummary(prefLanguages.getEntry());
    }
}