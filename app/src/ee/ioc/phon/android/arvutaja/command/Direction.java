package ee.ioc.phon.android.arvutaja.command;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Direction implements Command {

	@Override
	public Intent getIntent(Context context, String command) throws CommandParseException {
		String uriAsString = "http://maps.google.com/maps?daddr=" + command;
		if (command.contains("FROM")) {
			String query = new String(command);
			query = query.replaceFirst("FROM", "saddr=");
			query = query.replaceFirst("TO", "&daddr=");
			uriAsString = "http://maps.google.com/maps?" + query;
		}

		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriAsString));
	}

}