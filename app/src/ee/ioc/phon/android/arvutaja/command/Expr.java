package ee.ioc.phon.android.arvutaja.command;

import java.util.regex.Pattern;

import android.content.Intent;

/**
 * <p>Examples of supported expressions:</p>
 * <ul>
 * <li>( 12 + - 34 ) * 123</li>
 * </ul>
 */
public class Expr extends DefaultCommand {

	public static final Pattern p1 = Pattern.compile("^[0-9.]+$");
	public static final Pattern p2 = Pattern.compile("^\\(.*\\)$");

	public Expr(String command) {
		super(command);
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		return getActionView("http://www.wolframalpha.com/input/?i=", getCommand());
	}

	public String getOut() {
		MathEval math = new MathEval();
		// We remove all the space characters otherwise MathEval's tokenizer can fail
		return Double.toString(math.evaluate(getCommand().replaceAll("\\s+", "")));
	}

	public static boolean isCommand(String command) {
		return p1.matcher(command).matches() || p2.matcher(command).matches();
	}

}