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

package ee.ioc.phon.android.arvutaja;

import java.text.DecimalFormat;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.util.Linkify;


/**
 * <p>Some useful static methods.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Utils {

	private Utils() {}


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
			}
		})
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


	public static String makeLangLabel(String localeAsStr) {
		Locale l = new Locale(localeAsStr);
		return l.getDisplayName(l) + " (" + localeAsStr + ")";
	}


	public static String localeToTtsCode(Locale locale) {
		String iso3 = locale.getISO3Language();
		return Character.toUpperCase(iso3.charAt(0)) + iso3.substring(1) + "tts";
	}


	/**
	 * <p>Renders the expression and its value in a way that it can be spoken.
	 * The value can be Infinity, NaN, very large, and have many places after the dot.
	 * We render only 4 places after the dot and do not render scientific notation.</p>
	 *
	 * TODO: add localization
	 */
	public static String makeTtsOutput(Locale locale, String expression, Double value) {
		if (value != null) {
			String valueAsString = new DecimalFormat("#.####").format(value);
			String equals = LocalizedStrings.getString(locale, R.string.equals);
			return expression + " " + equals + " " + valueAsString;
			//return expression + " = " + String.format(locale, "%.4f", value);
		}
		return expression;
	}


	private static PackageInfo getPackageInfo(Context c) {
		PackageManager manager = c.getPackageManager();
		try {
			return manager.getPackageInfo(c.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e("Couldn't find package information in PackageManager: " + e);
		}
		return null;
	}
}
