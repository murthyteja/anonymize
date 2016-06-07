import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * A module for all the frequently used methods
 */
public class Utilities {

	/*
	 * Checks if the input string is an email or not
	 * by comparing with a regular expression
	 */
	public static boolean isEmail(String string){
		Pattern emailPattern = Pattern.compile(".+@.+\\.[a-z]+");
		Matcher m = emailPattern.matcher(string);
		return m.matches();
	}

	
}
