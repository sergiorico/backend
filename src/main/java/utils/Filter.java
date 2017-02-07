package utils;

public class Filter {
	public static String xss(String input) {
		// Some fields are optional so allow for null here
		if (input == null)
			return input;

		input = input.replaceAll("</", " ");
		input = input.replaceAll("/>", " "); 
		input = input.replaceAll("<", " ");
		input = input.replaceAll(">", " "); 
		return input;
	}
}

