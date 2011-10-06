package ee.ioc.phon.android.unitconv;

import javax.measure.unit.Unit;

/**
 * <p>Converts an expression (String) into a number
 * and returns it as a string. If an error occurs then returns the
 * error message.</p>
 * 
 * <p>Different kinds of input expressions are supported:</p>
 * 
 * <ul>
 * <li>1 2 m IN ft</li>
 * <li>( 1 2 + 3 4 ) * 1 2 3</li>
 * </ul>
 * 
 * TODO: make this code not ugly
 */

public class Converter {

	public enum ExprType {
		UNITCONV,
		EXPR
	};

	private final ExprType mExprType;
	private final String mPrettyIn;

	private double mNumber;
	private String mIn;
	private String mOut;

	public Converter(String expr) {
		if (expr.contains(" IN ")) {
			mExprType = ExprType.UNITCONV;
			String[] splits = expr.split(" IN ");
			String numberAsStr = splits[0].replaceFirst("[^0-9\\. ].*", "").replaceAll("[^0-9\\.]", "");
			mNumber = Double.parseDouble(numberAsStr);
			mIn  = splits[0].replaceFirst("^[0-9\\. ]+", "").replaceAll("\\s+", "");
			mOut = splits[1].replaceAll("\\s+", "");
			mPrettyIn =  mNumber + " " + mIn + " IN " + mOut;
		} else {
			mExprType = ExprType.EXPR;
			mPrettyIn = expr.replaceAll("\\s+", "");
		}
	}


	/**
	 * @return pretty-printed version of the expression that was given to the constructor
	 */
	public String getIn() {
		return mPrettyIn;
	}


	/**
	 * @return evaluation of the expression that was given to the constructor
	 */
	public String getOut() {
		switch (mExprType) {
		case UNITCONV:
			return "" + Unit.valueOf(mIn).getConverterTo(Unit.valueOf(mOut)).convert(mNumber);
		case EXPR:
			MathEval math = new MathEval();
			return "" + math.evaluate(mPrettyIn);
		}
		return null;
	}
}