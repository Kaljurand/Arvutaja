package ee.ioc.phon.android.arvutaja.command;

import ee.ioc.phon.android.arvutaja.R;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;

public class DefaultCommand implements Command {

	private final String mCommand;

	public DefaultCommand(String command) {
		mCommand = command;
	}

	public int getMessage() {
		return R.string.msgActionWebSearch;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
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

	protected static Intent getActionView(String urlPrefix, String urlSuffix) {
		return new Intent(Intent.ACTION_VIEW, Uri.parse(urlPrefix + Uri.encode(urlSuffix)));
	}
}