package ee.ioc.phon.android.arvutaja.command;

import android.content.Intent;
import android.net.Uri;

public class Direction extends AbstractCommand {

	private final String mCommand;

	public Direction(String command) {
		mCommand = command;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		String uriAsString = "http://maps.google.com/maps?daddr=" + mCommand;
		if (mCommand.contains("FROM")) {
			String query = new String(mCommand);
			query = query.replaceFirst("FROM", "saddr=");
			query = query.replaceFirst("TO", "&daddr=");
			uriAsString = "http://maps.google.com/maps?" + query;
		}

		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriAsString));
	}

}