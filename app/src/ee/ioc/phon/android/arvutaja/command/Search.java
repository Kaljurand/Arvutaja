package ee.ioc.phon.android.arvutaja.command;

import ee.ioc.phon.android.arvutaja.R;
import android.app.SearchManager;
import android.content.Intent;

/**
 * This command maps to ACTION_SEARCH_LONG_PRESS, which probably means
 * different things on different devices, e.g. on SGS2 it opens the
 * voice assistant, e.g. Vlingo.
 */
public class Search extends DefaultCommand {

	public static final String PREFIX = "ask assistant";

	public Search(String command) {
		super(command);
	}

	public int getMessage() {
		return R.string.msgActionSearchLongPress;
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