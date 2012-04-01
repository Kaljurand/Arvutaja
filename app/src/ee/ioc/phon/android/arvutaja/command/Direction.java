package ee.ioc.phon.android.arvutaja.command;

import android.content.Intent;
import android.net.Uri;

/**
 * <p>Examples of supported expressions:</p>
 * <ul>
 * <li>Estonia puiestee 123 , Tallinn</li>
 * <li>Rakvere</li>
 * </ul>
 */
public class Direction extends DefaultCommand {

	public Direction(String command) {
		super(command);
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		String uriAsString = "http://maps.google.com/maps?daddr=" + getCommand();
		if (getCommand().contains("FROM")) {
			String query = new String(getCommand());
			query = query.replaceFirst("FROM", "saddr=");
			query = query.replaceFirst("TO", "&daddr=");
			uriAsString = "http://maps.google.com/maps?" + query;
		}

		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriAsString));
	}

	public static boolean isCommand(String command) {
		return command.contains(",");
	}

}