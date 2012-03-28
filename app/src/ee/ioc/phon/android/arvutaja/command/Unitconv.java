package ee.ioc.phon.android.arvutaja.command;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Unitconv implements Command {

	@Override
	public Intent getIntent(Context context, String command) throws CommandParseException {
		String uriAsString = "http://www.wolframalpha.com/input/?i=" + command;
		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriAsString));
	}

}