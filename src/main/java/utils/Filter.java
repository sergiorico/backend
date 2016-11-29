package utils;

public class Filter {
	public static String xss(String input){
		input=input.replaceAll("</", " ");
		input = input.replaceAll("/>", " "); 
		input=input.replaceAll("<", " ");
		input = input.replaceAll(">", " "); 
		return input;
	}
}

