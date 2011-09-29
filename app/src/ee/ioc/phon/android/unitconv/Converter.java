package ee.ioc.phon.android.unitconv;

import javax.measure.unit.Unit;

/**
 * <p>Converts a unit conversion expression (String) into a number
 * and returns it as a string. If an error occurs then returns the
 * error message.</p>
 * 
 * <p>The input string is expected to be something like: 1 2 m in ft</p>
 * 
 * TODO: make this code not ugly
 */

public class Converter {

	private final double mNumber;
	private final String mIn;
	private final String mOut;

	public Converter(String str) {
		String[] splits = str.split(" in ");
		String number = splits[0].replaceFirst("[^0-9\\. ]+", "").replaceAll("[^0-9\\.]", "");
		mNumber = Double.parseDouble(number);
		mIn = splits[0].replaceFirst("^[0-9\\. ]+", "").trim();
		mOut = splits[1].trim();
	}


	/**
	 * @return pretty-printed version of the expression that was given to the constructor
	 */
	public String getIn() {
		return mNumber + " " + mIn + " in " + mOut;
	}


	/**
	 * @return evaluation of the expression that was given to the constructor
	 */
	public String getOut() {
		return "" + Unit.valueOf(mIn).getConverterTo(Unit.valueOf(mOut)).convert(mNumber);
	}
}