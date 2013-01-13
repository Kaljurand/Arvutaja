package ee.ioc.phon.android.arvutaja.command;

import android.app.SearchManager;
import android.content.Intent;

public class WebSearch extends DefaultCommand {

	public WebSearch(String command) {
		super(command);
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		intent.putExtra(SearchManager.QUERY, getCommand());
		return intent;
	}

	public static boolean isCommand(String command) {
		return command.contains("weather") || command.contains("current time");
	}

}