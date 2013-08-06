package ee.ioc.phon.android.arvutaja.command;

import android.content.Intent;
import android.net.Uri;
import ee.ioc.phon.android.arvutaja.R;

import java.net.URI;
import java.net.URISyntaxException;

public class View extends DefaultCommand {

	private final Uri mUri;

	public View(String command) {
		super(command);
		mUri = Uri.parse(command);
	}

	public int getMessage() {
		return R.string.msgActionView;
	}

	@Override
	public Intent getIntent() throws CommandParseException {
		return new Intent(Intent.ACTION_VIEW, mUri);
	}


	public static boolean isCommand(String command) {
		try {
			new URI(command);
		} catch (URISyntaxException e) {
			return false;
		}
		return true;
	}

}