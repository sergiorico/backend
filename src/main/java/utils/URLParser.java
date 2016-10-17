package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLParser {
	// Pattern for recognizing a URL, based off RFC 3986
	private static final Pattern urlPattern = Pattern.compile(
	        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
	                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
	                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
	        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	
	/**
	 * @return RFC 3986 based URL matching pattern
	 */
	public static Pattern getPattern() { return urlPattern; }
	
	/**
	 * Extract the first URL from the given string.
	 * @return "" if no url is found, url otherwise
	 */
	public static String find(String content) {
		Matcher matcher = urlPattern.matcher(content);
		if (matcher.find()) {
		    int matchStart = matcher.start(1);
		    int matchEnd = matcher.end();
		    return content.substring(matchStart, matchEnd);
		}
		return "";
	}
}
