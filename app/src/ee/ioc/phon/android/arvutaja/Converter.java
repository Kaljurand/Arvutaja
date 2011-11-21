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
 * <li>convert 12 m to ft</li>
 * <li>convert 5.5 USD to EUR</li>
 * <li>( 12 + - 34 ) * 123</li>
 * <li>Estonia puiestee 123 , Tallinn</li>
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
	private final String mExpr;

	public Converter(String expr) {
		mExpr = expr;
		if (mExpr.contains("convert ") && mExpr.contains(" to ")) {
			mExprType = ExprType.UNITCONV;
		} else if (expr.contains(",")) {
			mExprType = ExprType.MAP;
		} else {
			mExprType = ExprType.EXPR;
		}
	}


	public String getView() {
		switch (mExprType) {
		case MAP:
			if (mExpr.contains("FROM")) {
				String query = new String(mExpr);
				query = query.replaceFirst("FROM", "saddr=");
				query = query.replaceFirst("TO", "&daddr=");
				return "http://maps.google.com/maps?" + query;
			} else {
				return "http://maps.google.com/maps?daddr=" + mExpr;
			}
		case UNITCONV:
			return "http://www.wolframalpha.com/input/?i=" + mExpr;
		}
		return null;
	}


	/**
	 * <p>Evaluates the expression and returns the result as a String.
	 * This method can throw various exceptions.</p>
	 *
	 * @return evaluation of the expression that was given to the constructor
	 */
	public String getOut() {
		switch (mExprType) {
		case MAP:
			return "";
		case UNITCONV:
			String query = new String(mExpr);
			query = query.replace("convert ", "");
			String[] splits = query.split(" to ");
			String numberAsStr = splits[0].replaceFirst("[^0-9\\. ].*", "").replaceAll("[^0-9\\.]", "");
			double mNumber = Double.parseDouble(numberAsStr);
			String mIn  = splits[0].replaceFirst("^[0-9\\. ]+", "").replaceAll("\\s+", "");
			String mOut = splits[1].replaceAll("\\s+", "");
			return Double.toString(Unit.valueOf(mIn).getConverterTo(Unit.valueOf(mOut)).convert(mNumber));
		case EXPR:
			MathEval math = new MathEval();
			return Double.toString(math.evaluate(mExpr));
		}
		return null;
	}
}