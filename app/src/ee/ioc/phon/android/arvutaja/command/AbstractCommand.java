package ee.ioc.phon.android.arvutaja.command;

import android.net.Uri;

public abstract class AbstractCommand implements Command {

	public Uri getSuggestion() {
		return Uri.parse("https://play.google.com/store");
	}

}
