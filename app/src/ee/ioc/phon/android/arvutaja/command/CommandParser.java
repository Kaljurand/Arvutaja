package ee.ioc.phon.android.arvutaja.command;

import android.content.Context;

public class CommandParser {

	public static Command getCommand(Context context, String command) throws CommandParseException {
		if (command == null) {
			throw new CommandParseException();
		}

		if (command.startsWith("alarm ")) {
			return new Alarm(command, context);
		} else if (command.contains("convert ") && command.contains(" to ")) {
			return new Unitconv(command);
		} else if (command.contains(",")) {
			return new Direction(command);
		}
		return new WebSearch(command);
	}

}