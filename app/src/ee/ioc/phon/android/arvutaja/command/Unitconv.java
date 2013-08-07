package ee.ioc.phon.android.arvutaja.command;

import javax.measure.unit.Unit;

import ee.ioc.phon.android.arvutaja.R;

import android.content.Intent;

/**
 * <p>Examples of supported expressions:</p>
 * <ul>
 * <li>convert 12 m to ft</li>
 * <li>convert 5.5 USD to EUR</li>
 * </ul>
 */
public class Unitconv extends DefaultCommand {

	public Unitconv(String command) {
		super(command);
	}

	public int getMessage() {
		return R.string.msgActionViewWolframAlpha;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		return getActionView("http://www.wolframalpha.com/input/?i=", getCommand());
	}

	public Object getOut() {
		String query = new String(getCommand());
		query = query.replace("convert ", "");
		String[] splits = query.split(" to ");
		String numberAsStr = splits[0].replaceFirst("[^0-9\\. ].*", "").replaceAll("[^0-9\\.]", "");
		double mNumber = Double.parseDouble(numberAsStr);
		String mIn  = splits[0].replaceFirst("^[0-9\\. ]+", "").replaceAll("\\s+", "");
		String mOut = splits[1].replaceAll("\\s+", "");
		return Unit.valueOf(mIn).getConverterTo(Unit.valueOf(mOut)).convert(mNumber);
	}

	public static boolean isCommand(String command) {
		return command.contains("convert ") && command.contains(" to ");
	}

}