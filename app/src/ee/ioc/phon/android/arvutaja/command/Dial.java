package ee.ioc.phon.android.arvutaja.command;

import ee.ioc.phon.android.arvutaja.R;
import android.content.Intent;
import android.net.Uri;

public class Dial extends DefaultCommand {

	public static final String PREFIX = "call ";

	public Dial(String command) {
		super(command);
	}

	public int getMessage() {
		return R.string.msgActionDial;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		String number = getCommand().replaceFirst(PREFIX, "");
		Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)));
		return intent;
	}


	public static boolean isCommand(String command) {
		return command.startsWith(PREFIX);
	}

}