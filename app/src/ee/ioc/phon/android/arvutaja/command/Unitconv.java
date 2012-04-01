package ee.ioc.phon.android.arvutaja.command;

import javax.measure.unit.Unit;

import android.content.Intent;
import android.net.Uri;

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

	@Override
	public Intent getIntent() throws CommandParseException {
		String uriAsString = "http://www.wolframalpha.com/input/?i=" + getCommand();
		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriAsString));
	}

	public String getOut() {
		String query = new String(getCommand());
		query = query.replace("convert ", "");
		String[] splits = query.split(" to ");
		String numberAsStr = splits[0].replaceFirst("[^0-9\\. ].*", "").replaceAll("[^0-9\\.]", "");
		double mNumber = Double.parseDouble(numberAsStr);
		String mIn  = splits[0].replaceFirst("^[0-9\\. ]+", "").replaceAll("\\s+", "");
		String mOut = splits[1].replaceAll("\\s+", "");
		return Double.toString(Unit.valueOf(mIn).getConverterTo(Unit.valueOf(mOut)).convert(mNumber));
	}

	public static boolean isCommand(String command) {
		return command.contains("convert ") && command.contains(" to ");
	}

}