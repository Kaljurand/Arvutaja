package ee.ioc.phon.android.arvutaja;

/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
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
		MAP,
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
			// @deprecated
			mExprType = ExprType.UNITCONV;
			String[] splits = expr.split(" IN ");
			String numberAsStr = splits[0].replaceFirst("[^0-9\\. ].*", "").replaceAll("[^0-9\\.]", "");
			mNumber = Double.parseDouble(numberAsStr);
			mIn  = splits[0].replaceFirst("^[0-9\\. ]+", "").replaceAll("\\s+", "");
			mOut = splits[1].replaceAll("\\s+", "");
			mPrettyIn =  "convert " + mNumber + " " + mIn + " to " + mOut;
		} else if (expr.contains("convert") && expr.contains("to")) {
			mExprType = ExprType.UNITCONV;
			expr = expr.replace("convert ", "");
			String[] splits = expr.split(" to ");
			String numberAsStr = splits[0].replaceFirst("[^0-9\\. ].*", "").replaceAll("[^0-9\\.]", "");
			mNumber = Double.parseDouble(numberAsStr);
			mIn  = splits[0].replaceFirst("^[0-9\\. ]+", "").replaceAll("\\s+", "");
			mOut = splits[1].replaceAll("\\s+", "");
			mPrettyIn =  "convert " + mNumber + " " + mIn + " to " + mOut;
		} else if (expr.contains(",")) {
			mExprType = ExprType.MAP;
			// Remove space between digits
			mPrettyIn = expr.replaceAll("(\\d)\\s+", "$1");
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


	public String getView() {
		String query = mPrettyIn;
		switch (mExprType) {
		case MAP:
			if (query.contains("FROM")) {
				query = query.replaceFirst("FROM", "saddr=");
				query = query.replaceFirst("TO", "&daddr=");
				return "http://maps.google.com/maps?" + query;
			} else {
				return "http://maps.google.com/maps?daddr=" + query;
			}
		case UNITCONV:
			return "http://www.wolframalpha.com/input/?i=" + query;
		}
		return null;
	}


	/**
	 * @return evaluation of the expression that was given to the constructor
	 */
	public String getOut() {
		switch (mExprType) {
		case MAP:
			return "";
		case UNITCONV:
			return "" + Unit.valueOf(mIn).getConverterTo(Unit.valueOf(mOut)).convert(mNumber);
		case EXPR:
			MathEval math = new MathEval();
			return "" + math.evaluate(mPrettyIn);
		}
		return null;
	}
}