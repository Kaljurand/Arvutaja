package ee.ioc.phon.android.arvutaja;

/*
 * Copyright 2011-2013, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.List;

/**
 * @author Kaarel Kaljurand
 */
public abstract class AbstractRecognizerActivity extends Activity {

    /**
     * Launches an activity which the user probably does not want to see
     * if he presses HOME while in this activity and then starts Arvutaja again
     * from the launcher.
     * <p/>
     * TODO: Note that activities cannot be launched when the app was launched via voice interaction.
     * We get the exception:
     * java.lang.SecurityException: Starting under voice control not allowed for:
     * Intent { act=android.intent.action.VIEW dat=http://maps.google.com/... flg=0x80000 }
     */
    public void startForeignActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        try {
            startActivity(intent);
        } catch (SecurityException e) {
            // TODO: localize the error message
            toast(e.getMessage());
        }
    }


    public String getVersionName() {
        return Utils.getVersionName(this);
    }


    protected List<ResolveInfo> getIntentActivities(Intent intent) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        return activities;
    }


    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


    public void showErrorDialog(int msg) {
        new AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setTitle(R.string.error)
                .setMessage(msg)
                .create()
                .show();
    }

    public void showError(int msg) {
        Toast.makeText(getApplicationContext(), getString(msg), Toast.LENGTH_LONG).show();
    }
}