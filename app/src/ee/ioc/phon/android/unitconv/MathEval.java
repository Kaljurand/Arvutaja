// Created by Lawrence PC Dol.  Released into the public domain.
//
// Source is licensed for any use, provided this copyright notice is retained.
// No warranty for any purpose whatsoever is implied or expressed.  The author
// is not liable for any losses of any kind, direct or indirect, which result
// from the use of this software.

// Comment by Kaarel Kaljurand:
// This file was downloaded on 2011-10-06 from
// http://www.softwaremonkey.org/Code/src/MathEval.java
// (see more: http://www.softwaremonkey.org/Code/MathEval)
// Only the package name was changed.

package ee.ioc.phon.android.unitconv;

// Commented out (Eclipse said that it is not used, Kaarel 2011-10-06)
//import java.math.*;
import java.util.*;

/**
 * Math Evaluator.  Provides the ability to evaluate a String math expression, with support for functions, variables and
 * standard math constants.
 * <p>
 * Predefined Constants:
 * <ul>
 *   <li>E                  - The double value that is closer than any other to e, the base of the natural logarithms (2.718281828459045).
 *   <li>Euler              - Euler's Constant (0.577215664901533).
 *   <li>LN2                - Log of 2 base e (0.693147180559945).
 *   <li>LN10               - Log of 10 base e (2.302585092994046).
 *   <li>LOG2E              - Log of e base 2 (1.442695040888963).
 *   <li>LOG10E             - Log of e base 10 (0.434294481903252).
 *   <li>PHI                - The golden ratio (1.618033988749895).
 *   <li>PI                 - The double value that is closer than any other to pi, the ratio of the circumference of a circle to its diameter (3.141592653589793).
 *   </ul>
 * <p>
 * Supported Functions:
 * <ul>
 *   <li>abs(val)           - The absolute value of a double value.
 *   <li>acos(val)          - The arc cosine of a value; the returned angle is in the range 0.0 through pi.
 *   <li>asin(val)          - The arc sine of a value; the returned angle is in the range -pi/2 through pi/2.
 *   <li>atan(val)          - The arc tangent of a value; the returned angle is in the range -pi/2 through pi/2.
 *   <li>cbrt(val)          - The cube root of val.
 *   <li>ceil(val)          - The smallest (closest to negative infinity) double value that is greater than or equal to the argument and is equal to a mathematical integer.
 *   <li>cos(val)           - The trigonometric cosine of an angle.
 *   <li>cosh(val)          - The hyperbolic cosine of val.
 *   <li>exp(val)           - Euler's number e raised to the power of a double value.
 *   <li>expm1(val)         - <i>e</i><super>x</super>-1.
 *   <li>floor(val)         - The largest (closest to positive infinity) double value that is less than or equal to the argument and is equal to a mathematical integer.
 *   <li>getExponent(val)   - The unbiased exponent used in the representation of val.
 *   <li>log(val)           - The natural logarithm (base e) of a double  value.
 *   <li>log10(val)         - The base 10 logarithm of val.
 *   <li>log1p(val)         - The natural logarithm of (val+1).
 *   <li>nextUp(val)        - The floating-point value adjacent to val in the direction of positive infinity.
 *   <li>random()           - A double value with a positive sign, greater than or equal to 0.0 and less than 1.0.
 *   <li>round(val)         - The closest 64 bit integer to the argument.
 *   <li>roundHE(val)       - The double value that is closest in value to the argument and is equal to a mathematical integer, using the half-even rounding method.
 *   <li>signum(val)        - The signum function of the argument; zero if the argument is zero, 1.0 if the argument is greater than zero, -1.0 if the argument is less than zero.
 *   <li>sin(val)           - The trigonometric sine of an angle.
 *   <li>sinh(val)          - The hyperbolic sine of a double value.
 *   <li>sqrt(val)          - The correctly rounded positive square root of a double value.
 *   <li>tan(val)           - The trigonometric tangent of an angle.
 *   <li>tanh(val)          - The hyperbolic tangent of a double value.
 *   <li>toDegrees(val)     - Converts an angle measured in radians to an approximately equivalent angle measured in degrees.
 *   <li>toRadians(val)     - Converts an angle measured in degrees to an approximately equivalent angle measured in radians.
 *   <li>ulp(val)           - The size of an ulp of the argument.
 *   </ul>
 * <p>
 * Threading Design : [x] Single Threaded  [ ] Threadsafe  [ ] Immutable  [ ] Isolated
 *
 * @author          Lawrence Dol
 * @since           Build 2008.0426.1016
 */

public class MathEval
extends Object
{

// *****************************************************************************
// INSTANCE PROPERTIES
// *****************************************************************************

private Map<String,Double>              constants;                              // external constants
private Map<String,Double>              variables;                              // external variables
private boolean                         relaxed;                                // allow variables to be undefined

private int                             offset;                                 // used when returning from a higher precedence sub-expression evaluation
private boolean                         isConstant;                             // constant expression flag

// *****************************************************************************
// INSTANCE CREATE/DELETE
// *****************************************************************************

/**
 * Create a math evaluator.
 */
public MathEval() {
    super();

    constants=new TreeMap<String,Double>(String.CASE_INSENSITIVE_ORDER);
    setConstant("E"     ,Math.E);
    setConstant("Euler" ,0.577215664901533D);
    setConstant("LN2"   ,0.693147180559945D);
    setConstant("LN10"  ,2.302585092994046D);
    setConstant("LOG2E" ,1.442695040888963D);
    setConstant("LOG10E",0.434294481903252D);
    setConstant("PHI"   ,1.618033988749895D);
    setConstant("PI"    ,Math.PI);

    variables=new TreeMap<String,Double>(String.CASE_INSENSITIVE_ORDER);
    }

// *****************************************************************************
// INSTANCE METHODS - ACCESSORS
// *****************************************************************************

/** Set a named constant (constant names are not case-sensitive).  Constants are like variables but are not cleared by clear(). Variables of the same name have precedence over constants. */
public double getConstant(String nam) {
    Double                              val=constants.get(nam);

    return (val==null ? 0 : val.doubleValue());
    }

/** Set a named constant (constants names are not case-sensitive).  Constants are like variables but are not cleared by clear(). Variables of the same name have precedence over constants. */
public MathEval setConstant(String nam, double val) {
    return setConstant(nam,new Double(val));
    }

/** Set a named constant (constants names are not case-sensitive).  Constants are like variables but are not cleared by clear(). Variables of the same name have precedence over constants. */
public MathEval setConstant(String nam, Double val) {
    if(!Character.isLetter(nam.charAt(0))) { throw new IllegalArgumentException("Constant names must start with a letter"     ); }
    if(nam.indexOf('(')!=-1              ) { throw new IllegalArgumentException("Constant names may not contain a parenthesis"); }
    if(nam.indexOf(')')!=-1              ) { throw new IllegalArgumentException("Constant names may not contain a parenthesis"); }
    if(constants.get(nam)!=null          ) { throw new IllegalArgumentException("Constants may not be redefined"              ); }
    constants.put(nam,val);
    return this;
    }

/** Set a named variable (variables names are not case-sensitive). */
public double getVariable(String nam) {
    Double                              val=variables.get(nam);

    return (val==null ? 0 : val.doubleValue());
    }

/** Set a named variable (variables names are not case-sensitive). */
public MathEval setVariable(String nam, double val) {
    return setVariable(nam,new Double(val));
    }

/** Set a named variable (variables names are not case-sensitive). */
public MathEval setVariable(String nam, Double val) {
    if(!Character.isLetter(nam.charAt(0))) { throw new IllegalArgumentException("Variable must start with a letter"           ); }
    if(nam.indexOf('(')!=-1              ) { throw new IllegalArgumentException("Variable names may not contain a parenthesis"); }
    if(nam.indexOf(')')!=-1              ) { throw new IllegalArgumentException("Variable names may not contain a parenthesis"); }
    variables.put(nam,val);
    return this;
    }

/** Clear all variables (constants are not affected). */
public MathEval clear() {
    variables.clear();
    return this;
    }

/** Get whether a variable which is used in an expression is required to be explicitly set. If not explicitly set, the value 0.0 is assumed. */
public boolean getVariableRequired() {
    return relaxed;
    }

/** Set whether a variable which is used in an expression is required to be explicitly set. If not explicitly set, the value 0.0 is assumed. */
public MathEval setVariableRequired(boolean val) {
    relaxed=(!val);
    return this;
    }

// *****************************************************************************
// INSTANCE METHODS - PUBLIC API
// *****************************************************************************

/**
 * Evaluate this expression.
 */
public double evaluate(String exp) throws NumberFormatException, ArithmeticException {
    isConstant=true;
    return _evaluate(exp,0,(exp.length()-1),'=',0,'=');
    }

/**
 * Return whether the previous expression evaluated was constant (i.e. contained no variables).
 * This is useful when optimizing to store the result instead of repeatedly evaluating a constant expression like "2+2".
 */
public boolean previousExpressionConstant() throws NumberFormatException, ArithmeticException {
    return isConstant;
    }

// *****************************************************************************
// INSTANCE METHODS - PRIVATE IMPLEMENTATION
// *****************************************************************************

/**
 * @param exp       Entire expression.
 * @param beg       Inclusive begin offset for subexpression.
 * @param end       Inclusive end offset for subexpression.
 * @param pop       Pending operator (operator previous to this subexpression).
 * @param tot       Running total to initialize this subexpression.
 * @param cop       Current operator (the operator for this subexpression).
 */
private double _evaluate(String exp, int beg, int end, char pop, double tot, char cop) throws NumberFormatException, ArithmeticException {
    char                                nop=0;                                  // next operator
    int                                 ofs=beg;                                // current expression offset

    while(beg<=end && Character.isWhitespace(exp.charAt(end))) { end--; }
    if(beg>end) { throw exception(exp,beg,"Mathematical expression is blank or contains a blank sub-expression"); }

    OUTER:
    for(ofs=beg; ofs<=end; ofs++) {
        while(ofs<=end && Character.isWhitespace(exp.charAt(ofs))) { ofs++; }
        if(ofs<=end) {
            boolean bkt=false;
            for(beg=ofs; ofs<=end; ofs++) {
                nop=exp.charAt(ofs);
                if(nop=='(') {
                    int cls=skipTo(')',exp,(ofs+1),end,false,false);
                    if(cls>end) { throw exception(exp,ofs,"Mathematical expression contains an unclosed parenthesis"); }
                    bkt=true;
                    ofs=cls;
                    }
                if(isOperator(nop) && (ofs!=beg || !isSign(nop))) {              // break at operator, excluding lead sign character
                    break;
                    }
                }

            String txt=exp.substring(beg,ofs);
            char   ch0=txt.charAt(0);
            double val;

            if(ch0=='(') {
                val=_evaluate(exp,(beg+1),(beg+txt.length()-2),'=',0,'=');
                }
            else if(Character.isLetter(ch0)) {
                if(bkt) { val=function(exp,beg,(beg+txt.length()-1)); }
                else    { val=variable(exp,beg,(beg+txt.length()-1)); }
                }
            else {
                try {
                    if(bkt) {                                                   // implied multiplication; e.g 2(x+3)
                        txt=txt.substring(0,txt.indexOf('('));
                        ofs=(beg+txt.length());
                        nop='*';
                        ofs--;                                                  // backup to before '(' to compensate for outer loop ofs++
                        }
                    txt=txt.trim();
                    if(stringSW(txt,"0x")) { val=(double)Long.parseLong(txt.substring(2),16); }
                    else                   { val=Double.parseDouble(txt);                     }
                    }
                catch(NumberFormatException thr) { throw exception(exp,beg,"Mathematical expression contains the invalid value \""+txt+"\""); }
                }

            if(cop!='=' && opPrecedence(nop)>opPrecedence(cop)) {               // correct even for last (non-opr) character, since non-oprs have "precedence" zero
                val=_evaluate(exp,(ofs+1),end,cop,val,nop);                     // from after operator to end of current subexpression
                ofs=offset;                                                     // modify offset
                nop=exp.charAt(ofs<=end ? ofs : end);                           // modify next operator
                }

            try {
                switch(cop) {
                    case '=' : { tot=val;               } break;
                    case '+' : { tot=(tot+val);         } break;
                    case '-' : { tot=(tot-val);         } break;
                    case '*' : { tot=(tot*val);         } break;
                    case '/' : { tot=(tot/val);         } break;
                    case '%' : { tot=(tot%val);         } break;
                    case '^' : { tot=Math.pow(tot,val); } break;
                    default  : {
                        int tmp=beg;
                        while(tmp>0 && !isOperator(exp.charAt(tmp))) { tmp--; }
                        throw exception(exp,tmp,"Operator '"+cop+"' not handled by math engine (Programmer error: The list of operators is inconsistent with the engine)");
                        }
                    }
                }
            catch(ArithmeticException thr) { throw exception(exp,beg,"Mathematical expression failed to evaluate (Exception: "+thr.getMessage()+")"); }
            cop=nop;
            if(pop!='=' && opPrecedence(cop)<=opPrecedence(pop)) { break OUTER; }
            }
        if(ofs==end && isOperator(nop)) { throw exception(exp,ofs,"Mathematical expression ends with a blank sub-expression"); }
        }
    offset=ofs;
    return tot;
    }

private double function(String exp, int beg, int end) {
    while(beg<=end && Character.isWhitespace(exp.charAt(end))) { end--; }
    if(beg>end) { throw exception(exp,beg,"Mathematical expression is blank or contains a blank sub-expression"); }

    // CONSTANT VALUE FUNCTIONS: THESE FUNCTIONS RETURN THE SAME VALUE GIVEN THE SAME PARAMETERS EACH TIME THEY ARE CALLED
    if(regionEIC(exp,beg,"abs("        )) { return Math.abs        (_evaluate(exp,(beg+"abs("        .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"acos("       )) { return Math.acos       (_evaluate(exp,(beg+"acos("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"asin("       )) { return Math.asin       (_evaluate(exp,(beg+"asin("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"atan("       )) { return Math.atan       (_evaluate(exp,(beg+"atan("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"ceil("       )) { return Math.ceil       (_evaluate(exp,(beg+"ceil("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"cos("        )) { return Math.cos        (_evaluate(exp,(beg+"cos("        .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"exp("        )) { return Math.exp        (_evaluate(exp,(beg+"exp("        .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"floor("      )) { return Math.floor      (_evaluate(exp,(beg+"floor("      .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"log("        )) { return Math.log        (_evaluate(exp,(beg+"log("        .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"round("      )) { return Math.round      (_evaluate(exp,(beg+"round("      .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"roundHE("    )) { return Math.rint       (_evaluate(exp,(beg+"roundHE("    .length()),(end-1),'=',0,'=')); } // round half-even
    if(regionEIC(exp,beg,"sin("        )) { return Math.sin        (_evaluate(exp,(beg+"sin("        .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"sqrt("       )) { return Math.sqrt       (_evaluate(exp,(beg+"sqrt("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"tan("        )) { return Math.tan        (_evaluate(exp,(beg+"tan("        .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"toDegrees("  )) { return Math.toDegrees  (_evaluate(exp,(beg+"toDegrees("  .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"toRadians("  )) { return Math.toRadians  (_evaluate(exp,(beg+"toRadians("  .length()),(end-1),'=',0,'=')); }

    // CONSTANT VALUE FUNCTIONS: THESE FUNCTIONS RETURN THE SAME VALUE GIVEN THE SAME PARAMETERS EACH TIME THEY ARE CALLED (J5+)
    if(regionEIC(exp,beg,"cbrt("       )) { return Math.cbrt       (_evaluate(exp,(beg+"cbrt("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"cosh("       )) { return Math.cosh       (_evaluate(exp,(beg+"cosh("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"expm1("      )) { return Math.expm1      (_evaluate(exp,(beg+"expm1("      .length()),(end-1),'=',0,'=')); }
    // Commented out because Eclipse complained (not in Java 5?) (Kaarel, 2011-10-06)
    //if(regionEIC(exp,beg,"getExponent(")) { return Math.getExponent(_evaluate(exp,(beg+"getExponent(".length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"log10("      )) { return Math.log10      (_evaluate(exp,(beg+"log10("      .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"log1p("      )) { return Math.log1p      (_evaluate(exp,(beg+"log1p("      .length()),(end-1),'=',0,'=')); }
    // Commented out because Eclipse complained (not in Java 5?) (Kaarel, 2011-10-06)
    //if(regionEIC(exp,beg,"nextUp("     )) { return Math.nextUp     (_evaluate(exp,(beg+"nextUp("     .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"signum("     )) { return Math.signum     (_evaluate(exp,(beg+"signum("     .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"sinh("       )) { return Math.sinh       (_evaluate(exp,(beg+"sinh("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"tanh("       )) { return Math.tanh       (_evaluate(exp,(beg+"tanh("       .length()),(end-1),'=',0,'=')); }
    if(regionEIC(exp,beg,"ulp("        )) { return Math.ulp        (_evaluate(exp,(beg+"ulp("        .length()),(end-1),'=',0,'=')); }

    isConstant=false;

    // INCONSTANT VALUE FUNCTIONS: THESE FUNCTIONS POTENTIALLY RETURN A DIFFERENT VALUE EVERY TIME THEY ARE CALLED, EVEN IF WITH THE SAME PARAMETERS
    if(regionEIC(exp,beg,"random("     )) { return Math.random     (                                                              ); }

    throw exception(exp,beg,"Unrecognized function \""+exp.substring(beg,(end+1))+"\"");
    }

private double variable(String exp, int beg, int end) {
    String                  nam=exp.substring(beg,(end+1));
    Double                  val;

    while(beg<=end && Character.isWhitespace(exp.charAt(end))) { end--; }
    if(beg>end) { throw exception(exp,beg,"Mathematical expression is blank or contains a blank sub-expression"); }

    if     ((val=constants.get(nam))!=null) {                   return val.doubleValue(); }
    else if((val=variables.get(nam))!=null) { isConstant=false; return val.doubleValue(); }
    else if(relaxed                       ) { isConstant=false; return 0.0;               }

    throw exception(exp,beg,"Unrecognized variable or constant \""+exp.substring(beg,(end+1))+"\"");
    }

private boolean isSign(char chr) {
    return (chr=='-' || chr=='+');
    }

private boolean isOperator(char chr) {
    return (chr<128 && PRECEDENCE[chr]!=0);
    }

private int opPrecedence(char chr) {
    return (chr<128 ? PRECEDENCE[chr] : 0);
    }

private ArithmeticException exception(String exp, int ofs, String txt) {
    return new ArithmeticException(txt+" at offset "+ofs+" in expression \""+exp+"\"");
    }

// *****************************************************************************
// INSTANCE INNER CLASSES
// *****************************************************************************

// *****************************************************************************
// STATIC NESTED CLASSES
// *****************************************************************************

// *****************************************************************************
// STATIC PROPERTIES
// *****************************************************************************

static public  final String             OPERATIONS;
static private final int[]              PRECEDENCE;
static {
    int[] prc=new int[127];
    prc['+']=1;
    prc['-']=1;
    prc['*']=2;
    prc['/']=2;
    prc['%']=2;
    prc['^']=3;
    PRECEDENCE=prc;

    String ops="";
    for(int xa=0; xa<PRECEDENCE.length; xa++) {
        if(PRECEDENCE[xa]!=0) { ops+=(char)xa; }
        }
    OPERATIONS=ops;
    }

// *****************************************************************************
// STATIC INIT & MAIN
// *****************************************************************************

// *****************************************************************************
// STATIC METHODS - UTILITY
// *****************************************************************************

static private boolean regionEIC(String str, int ofs, String val) {
    return str.regionMatches(true,ofs,val,0,val.length());
    }

static private boolean stringSW(String str, String val) {
    if(str==null) { return (val==null); }
    if(val==null) { return false;      }
    return str.regionMatches(true,0,val,0,val.length());
    }

static private int skipTo(char chr, String str, int beg, int end, boolean ignqut, boolean ignesc) {
    int                                 ofs=beg,lvl=1;
    boolean                             nst,qut,esc;                            // nesting, quoting, escaping
    char                                opn;

    if     (chr==')' ) { nst=true;  opn='('; qut=ignqut; esc=false;  }
    else if(chr==']' ) { nst=true;  opn='['; qut=ignqut; esc=false;  }
    else if(chr=='}' ) { nst=true;  opn='{'; qut=ignqut; esc=false;  }
    else if(chr=='>' ) { nst=true;  opn='<'; qut=ignqut; esc=false;  }
    else if(chr=='"' ) { nst=false; opn=0;   qut=false;  esc=ignesc; }
    else if(chr=='\'') { nst=false; opn=0;   qut=false;  esc=ignesc; }
    else               { nst=false; opn=0;   qut=ignqut; esc=ignesc; }

    for(; ofs<=end; ofs++) {
        char cur=str.charAt(ofs);
        if     (cur==opn  && nst) { lvl++; continue;                                  }
        else if(cur=='\'' && qut) { ofs=skipTo(cur,str,(ofs+1),end,false,ignesc); }
        else if(cur=='\"' && qut) { ofs=skipTo(cur,str,(ofs+1),end,false,ignesc); }
        else if(cur==chr) {
            if((!esc || !isEscaped(str,beg,ofs)) && (--lvl)==0) { break; }
            }
        }
    return ofs;
    }

static private boolean isEscaped(String str, int beg, int ofs) {
    boolean                             ie=false;

    while(ofs>beg && str.charAt(--ofs)=='\\') { ie=!ie; }
    return ie;
    }

} // END PUBLIC CLASS
