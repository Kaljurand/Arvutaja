package ee.ioc.phon.android.arvutaja.command;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;

public class CommandParser {

	public static Intent getIntent(Context context, String command) throws CommandParseException {
		if (command == null) {
			throw new CommandParseException();
		}

		if (command.startsWith("alarm ")) {
			return new Alarm().getIntent(context, command);
		} else if (command.contains("convert ") && command.contains(" to ")) {
			return new Unitconv().getIntent(context, command);
		} else if (command.contains(",")) {
			return new Direction().getIntent(context, command);
		}
		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		intent.putExtra(SearchManager.QUERY, command);
		return intent;
	}

}