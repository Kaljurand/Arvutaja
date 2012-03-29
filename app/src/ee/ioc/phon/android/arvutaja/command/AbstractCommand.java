package ee.ioc.phon.android.arvutaja.command;

public abstract class AbstractCommand implements Command {

	public String getSuggestion() {
		return "Cannot find an app to perform this command.";
	}

}
