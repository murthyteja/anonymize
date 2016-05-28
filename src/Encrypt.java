
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class Encrypt {

	public static String encodeString(String input) throws UnsupportedEncodingException{
		byte[] bytesEncoded = Base64.getEncoder().encode(input.getBytes());
		return new String(bytesEncoded, "UTF-8");
	}
	
	public static String decodeString(String input) throws UnsupportedEncodingException{
		byte[] decodedBytes = Base64.getDecoder().decode(input.getBytes());
		return new String(decodedBytes, "UTF-8");
	}
}
