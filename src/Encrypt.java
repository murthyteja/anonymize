
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;

public class Encrypt{
	final static String key = "Bar12345Bar12345"; // 128 bit key
	final static String initVector = "RandomInitVector"; // 16 bytes IV
	final static Properties properties = new Properties();
	
	//Creates a CryptoCipher instance with the transformation and properties.
	final static String transform = "AES/CBC/PKCS5Padding";
	
	public static String encodeString(String input) throws Exception{
		String encryptedText;
		if (Utilities.isEmail(input)){
			encryptedText = encodeEmail(input);
		}
		else{
			encryptedText = encodeText(input);
		}
		return encryptedText;
	}
	
	public static String getRandomAlphaNumericString(int length){
		boolean useLetters = true;
		boolean useNumbers = true;
		return RandomStringUtils.random(length, useLetters, useNumbers);
	}

	public static double encodeDouble(double number){
		AppProperties appProperties = new AppProperties();
		int divisor = Integer.parseInt(appProperties.getProperty("number_divisor"));
		return number / divisor;
	}

	public static int encodeInteger(int number){
		AppProperties appProperties = new AppProperties();
		int delta = Integer.parseInt(appProperties.getProperty("delta"));
		return number - delta;
	}

	private static String encodeEmail(String email) throws UnsupportedEncodingException{
		//		String[] chunks = email.split("@");
		// Consider mark@facebook.com, mailId = "mark" and restOfEmail = "facebook.com"
		//		String mailId = chunks[0];
		//		String restOfEmail = chunks[1];
		//		// To encrypt an email, encrypt the ID alone
		//		return (encodeText(mailId) + "@" + restOfEmail);
		return "Tactical_Applications@rbc.com";
	}
	
	/*
	 * Private: Method that performs very basic AES encryption using a criptographic key and
	 * 			an init vector
	 * 
	 * Parameters:
	 * 			value - Input value in String format
	 * 
	 * Returns:
	 * 			Returns a string value
	 */
	@SuppressWarnings("unused")
	private static String encrypt(String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            String output = "";
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            output = Base64.encodeBase64String(encrypted);
            if (output.length() !=0 && output!=null)
            {
            	output = output.substring(0, Math.min(output.length(), 10));
            }
            return output;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }


    public static String decrypt(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

	private static String encodeText(String input) throws Exception{
		// Replace all special characters in the text input with a space
		String output = "";
		if (input != null && !input.isEmpty()){
			//input = input.toLowerCase();
			//input = input.replaceAll("[^a-zA-Z0-9]", " ");
			//AppProperties prop = new AppProperties();
			//byte[] bytesEncoded = Base64.getEncoder().encode(input.getBytes("UTF-8"));
			//output = new String(bytesEncoded, prop.getProperty("character_set"));
			// Changes to make stuff work with Java 6 :(
			//output = new BASE64Encoder().encode(input.getBytes());
			//if (output.length() !=0 && input!=null){
			//	output = output.substring(0, (input.length() - 1));
			//}
			//output = encrypt(input);
			// Using Random string generation technique for now
			output = getRandomAlphaNumericString(input.length());
		}
		return output;
	}
}
