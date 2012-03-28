package ee.ioc.phon.android.arvutaja.command;

import android.content.Context;
import android.content.Intent;

public interface Command {

	Intent getIntent(Context context, String command) throws CommandParseException;
}