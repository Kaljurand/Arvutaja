package ee.ioc.phon.android.arvutaja.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;

/**
 * <p>Examples of supported expressions:</p>
 * <ul>
 * <li>Estonia puiestee 123 , Tallinn</li>
 * <li>Rakvere</li>
 * <li>FROM Estonia puiestee 123 , Tallinn TO Rakvere</li>
 * </ul>
 */
public class Direction extends DefaultCommand {

	public static final Pattern p = Pattern.compile("[fF][rR][oO][mM] (.+) [tT][oO] (.+)");
	public static final String MAPS_GOOGLE_COM = "http://maps.google.com/maps?";

	public Direction(String command) {
		super(command);
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		Matcher m = p.matcher(getCommand());
		String fromTo;
		if (m.matches()) {
			fromTo = "saddr=" + Uri.encode(m.group(1)) + "&daddr=" + Uri.encode(m.group(2));	
		} else {
			fromTo = "daddr=" + Uri.encode(getCommand());
		}

		return new Intent(Intent.ACTION_VIEW, Uri.parse(MAPS_GOOGLE_COM + fromTo));
	}

	public static boolean isCommand(String command) {
		return command.contains(",") || p.matcher(command).matches();
	}

}