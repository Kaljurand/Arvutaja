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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;


/**
 * <p>Some useful static methods.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class Utils {

	private Utils() {}


	/**
	 * TODO: should we immediately return null if id = 0?
	 */
	public static String idToValue(Context context, Uri contentUri, String columnId, String columnUrl, long id) {
		String value = null;
		Cursor c = context.getContentResolver().query(
				contentUri,
				new String[] { columnUrl },
				columnId + "= ?",
				new String[] { String.valueOf(id) },
				null);

		if (c.moveToFirst()) {
			value = c.getString(0);
		}
		c.close();
		return value;
	}


	public static AlertDialog getOkDialog(final Context context, String msg) {
		final SpannableString s = new SpannableString(msg);
		Linkify.addLinks(s, Linkify.ALL);
		return new AlertDialog.Builder(context)
		.setPositiveButton(context.getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.setMessage(s)
		.create();
	}


	public static AlertDialog getYesNoDialog(Context context, String confirmationMessage, final Executable ex) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder
		.setMessage(confirmationMessage)
		.setCancelable(false)
		.setPositiveButton(context.getString(R.string.buttonYes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				ex.execute();
			}
		})
		.setNegativeButton(context.getString(R.string.buttonNo), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		return builder.create();
	}


	public static AlertDialog getGoToStoreDialog(final Context context, String msg, final Uri uri) {
		final SpannableString s = new SpannableString(msg);
		Linkify.addLinks(s, Linkify.ALL);
		return new AlertDialog.Builder(context)
		.setPositiveButton(context.getString(R.string.buttonGoToStore), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
		})
		.setNegativeButton(context.getString(R.string.buttonCancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.setMessage(s)
		.create();
	}


	public static AlertDialog getGoToStoreDialogWithThreeButtons(final Context context, String msg, final Uri uri) {
		final SpannableString s = new SpannableString(msg);
		Linkify.addLinks(s, Linkify.ALL);
		return new AlertDialog.Builder(context)
		.setPositiveButton(context.getString(R.string.buttonGoToStore), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
		})
		.setNegativeButton(context.getString(R.string.buttonNever), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(context.getString(R.string.prefFirstTime), false);
				editor.commit();
				dialog.cancel();
			}
		})
		.setNeutralButton(context.getString(R.string.buttonLater), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}})
			.setMessage(s)
			.create();
	}


	public static String getVersionName(Context c) {
		PackageInfo info = getPackageInfo(c);
		if (info == null) {
			return "?.?.?";
		}
		return info.versionName;
	}


	private static PackageInfo getPackageInfo(Context c) {
		PackageManager manager = c.getPackageManager();
		try {
			return manager.getPackageInfo(c.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e(Utils.class.getName(), "Couldn't find package information in PackageManager", e);
		}
		return null;
	}
}
