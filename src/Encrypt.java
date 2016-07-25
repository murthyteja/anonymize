
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class Encrypt {

	public static String encodeString(String input) throws UnsupportedEncodingException{
		String encryptedText;
		if (Utilities.isEmail(input)){
			encryptedText = encodeEmail(input);
		}
		else{
			encryptedText = encodeText(input);
		}
		return encryptedText;
	}

	public static String decodeString(String input) throws UnsupportedEncodingException{
		String decryptedText;
		if (Utilities.isEmail(input)){
			decryptedText = decodeEmail(input);
		}
		else{
			decryptedText = decodeText(input);
		}
		return decryptedText;
	}

	public static double encodeDouble(double number){
		AppProperties appProperties = new AppProperties();
		int divisor = Integer.parseInt(appProperties.getProperty("number_divisor"));
		return number / divisor;
	}

	public static double decodeDouble(double number){
		AppProperties appProperties = new AppProperties();
		int divisor = Integer.parseInt(appProperties.getProperty("number_divisor"));
		return number * divisor;
	}

	public static double encodeInteger(double number){
		AppProperties appProperties = new AppProperties();
		int divisor = Integer.parseInt(appProperties.getProperty("number_divisor"));
		return number / divisor;
	}

	public static double decodeInteger(double number){
		AppProperties appProperties = new AppProperties();
		int divisor = Integer.parseInt(appProperties.getProperty("number_divisor"));
		return number * divisor;
	}
	
	private static String encodeEmail(String email) throws UnsupportedEncodingException{
		String[] chunks = email.split("@");
		// Consider mark@facebook.com, mailId = "mark" and restOfEmail = "facebook.com"
		String mailId = chunks[0];
		String restOfEmail = chunks[1];
		// To encrypt an email, encrypt the ID alone
		return (encodeText(mailId) + "@" + restOfEmail);
	}
	
	private static String decodeEmail(String encodedEmail) throws UnsupportedEncodingException{
		String[] chunks = encodedEmail.split("@");
		String encodedMailId = chunks[0];
		String restOfEmail = chunks[1];
		// To decrypt an email, decrypt the ID alone
		return (decodeText(encodedMailId) + "@" + restOfEmail);
	}
	
	private static String encodeText(String input) throws UnsupportedEncodingException{
		byte[] bytesEncoded = Base64.getEncoder().encode(input.getBytes());
		return new String(bytesEncoded, "UTF-8");
	}
	
	private static String decodeText(String input) throws UnsupportedEncodingException{
		byte[] decodedBytes = Base64.getDecoder().decode(input.getBytes());
		return new String(decodedBytes, "UTF-8");
	}
}
