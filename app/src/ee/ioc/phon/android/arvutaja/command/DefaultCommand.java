package ee.ioc.phon.android.arvutaja.command;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;

public class DefaultCommand implements Command {

	private final String mCommand;

	public DefaultCommand(String command) {
		mCommand = command;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		Intent intent = new Intent(Intent.ACTION_SEARCH);
		intent.putExtra(SearchManager.QUERY, mCommand);
		return intent;
	}

	@Override
	public Uri getSuggestion() {
		return Uri.parse("https://play.google.com/store");
	}

	@Override
	public String getCommand() {
		return mCommand;
	}

	@Override
	public String getOut() {
		return "";
	}

}