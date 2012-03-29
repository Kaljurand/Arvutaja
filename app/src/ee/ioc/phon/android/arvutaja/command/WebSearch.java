package ee.ioc.phon.android.arvutaja.command;

import android.app.SearchManager;
import android.content.Intent;

public class WebSearch extends AbstractCommand {

	private final String mCommand;

	public WebSearch(String command) {
		mCommand = command;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		intent.putExtra(SearchManager.QUERY, mCommand);
		return intent;
	}

}
