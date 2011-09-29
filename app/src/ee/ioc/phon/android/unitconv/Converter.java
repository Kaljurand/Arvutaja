package ee.ioc.phon.android.unitconv;

import javax.measure.unit.Unit;

public class Converter {

	/**
	 * <p>Converts a unit conversion expression (String) into a number
	 * and returns it as a string. If an error occurs then returns the
	 * error message.</p>
	 * 
	 * <p>The input string is expected to be something like: 1 2 m in ft</p>
	 * 
	 * TODO: make this code not ugly
	 */
	public static String convert(String str) {
		try {
			String[] splits = str.split(" in ");
			String number = splits[0].replaceFirst("[^0-9 ]+", "").replaceAll("[^0-9]", "");
			String unit1 = splits[0].replaceFirst("^[0-9 ]+", "").trim();
			String unit2 = splits[1].trim();
			return "" + Unit.valueOf(unit1).getConverterTo(Unit.valueOf(unit2)).convert(Double.parseDouble(number));
		} catch (Exception e) {
			return e.getMessage();
		}
	}
}