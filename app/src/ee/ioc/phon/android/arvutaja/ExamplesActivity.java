/*
 * Copyright 2012-2016, Institute of Cybernetics at Tallinn University of Technology
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

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebView;

import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class ExamplesActivity extends SubActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webview = new WebView(this);
        setContentView(webview);
        String locale = PreferenceUtils.getPrefString(PreferenceManager.getDefaultSharedPreferences(this),
                getResources(), R.string.keyLanguage, R.string.defaultLanguage);
        if ("et-EE".equals(locale)) {
            webview.loadUrl(getString(R.string.fileExamplesEt));
        } else {
            webview.loadUrl(getString(R.string.fileExamplesEnUs));
        }
    }

}