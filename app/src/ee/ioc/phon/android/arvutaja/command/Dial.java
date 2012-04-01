package ee.ioc.phon.android.arvutaja.command;

import android.content.Intent;
import android.net.Uri;

public class Dial extends DefaultCommand {

	public static final String PREFIX = "call ";

	public Dial(String command) {
		super(command);
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		String number = getCommand().replaceFirst(PREFIX, "");
		Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
		return intent;
	}


	public static boolean isCommand(String command) {
		return command.startsWith(PREFIX);
	}

}