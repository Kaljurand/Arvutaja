package ee.ioc.phon.android.arvutaja.command;

import android.content.Intent;
import android.net.Uri;

public interface Command {

	Intent getIntent() throws CommandParseException;

	/**
	 * <p>Returns a message that helps the user to resolve the situation
	 * where the command (intent) cannot be executed because the corresponding
	 * activity is not installed.</p>
	 *
	 * @return suggestion for an activity
	 */
	Uri getSuggestion();

	String getCommand();

	/**
	 * <p>Evaluates the expression and returns the result.
	 * This method can throw various exceptions.</p>
	 *
	 * @return evaluation of the expression that was given to the constructor
	 */
	Object getOut();


	/**
	 * @return resource ID of the message to show in the UI for this command.
	 */
	int getMessage();
}