package ee.ioc.phon.android.arvutaja.command;

import android.content.Intent;
import android.net.Uri;

public class Unitconv extends AbstractCommand {

	private final String mCommand;

	public Unitconv(String command) {
		mCommand = command;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		String uriAsString = "http://www.wolframalpha.com/input/?i=" + mCommand;
		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriAsString));
	}

}