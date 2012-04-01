package ee.ioc.phon.android.arvutaja.command;

import android.app.SearchManager;
import android.content.Intent;

public class Search extends DefaultCommand {

	public static final String PREFIX = "find ";

	public Search(String command) {
		super(command);
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		String thing = getCommand().replaceFirst(PREFIX, "");
		Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
		intent.putExtra(SearchManager.QUERY, thing);
		return intent;
	}


	public static boolean isCommand(String command) {
		return command.startsWith(PREFIX);
	}

}