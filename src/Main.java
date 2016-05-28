import java.io.UnsupportedEncodingException;


public class Main {

	public static void main(String[] args) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		System.out.println("This is my first Java program");
		// Initialize Application Properties Object
		AppProperties app = new AppProperties();
		
		
		String str = Encrypt.encodeString("Sample dude");
		System.out.println(str);
		System.out.println(Encrypt.decodeString(str));
		System.out.println(app.getProperty("number_divisor"));
	}

}
