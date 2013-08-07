package ee.ioc.phon.android.arvutaja;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * TODO: If we could (easily) query the resources by locale, then this class would not be needed.
 */
public class LocalizedStrings {

	private static final Map<Integer, String> STRINGS_ET;
	private static final Map<Integer, String> STRINGS_EN;
	private static final Map<String, Map<Integer, String>> STRINGS;

	static {
		Map<Integer, String> aMapEt = new HashMap<Integer, String>();
		aMapEt.put(R.string.equals, "on");
		aMapEt.put(R.string.ambiguous, "Sisend on mitmene: %d avaldist.");
		aMapEt.put(R.string.errorResultNoMatch, "Sisendkõne transkribeerimine luhtus. Proovige uuesti!");
		STRINGS_ET = Collections.unmodifiableMap(aMapEt);

		Map<Integer, String> aMapEn = new HashMap<Integer, String>();
		aMapEn.put(R.string.equals, "equals");
		aMapEn.put(R.string.ambiguous, "Input is ambiguous between %d readings.");
		aMapEn.put(R.string.errorResultNoMatch, "No match was found for the recorded speech. Please try again!");
		STRINGS_EN = Collections.unmodifiableMap(aMapEn);

		Map<String, Map<Integer, String>> aMap = new HashMap<String, Map<Integer, String>>();
		aMap.put("et", STRINGS_ET);
		aMap.put("en", STRINGS_EN);
		STRINGS = Collections.unmodifiableMap(aMap);
	}

	public static String getString(Locale locale, int resId) {
		Map<Integer, String> map = STRINGS.get(locale.getLanguage().substring(0, 2));
		if (map == null) {
			return null;
		}
		return map.get(resId);
	}

	public static String getString(Locale locale, int resId, Object... formatArgs) {
		Map<Integer, String> map = STRINGS.get(locale.getLanguage().substring(0, 2));
		if (map == null) {
			return null;
		}
		String format = map.get(resId);
		if (format == null) {
			return null;
		}
		return String.format(locale, format, formatArgs);
	}
}