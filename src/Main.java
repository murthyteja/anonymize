import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class Main {

	public static void main(String[] args) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		// Initialize Application Properties Object
		AppProperties app = new AppProperties();
		
		String str = Encrypt.encodeString("Sample dude");
		System.out.println(str);
		System.out.println(Encrypt.decodeString(str));
		System.out.println(app.getProperty("number_divisor"));
		
		// Testing a list of Tables to be ignored
		List<String> ignoreTablesList = new ArrayList<String>();
		ignoreTablesList.add("GlobalSettings");

		// Initialize the Data Source Object
		DataSource ds = new DataSource();
		Anonymize.anonymizeDatabase(ds.conn, ignoreTablesList);

		// Testing the error logging module
		ErrorLogger el = new ErrorLogger();
		el.write("Testing Log");
		System.out.println(Utilities.isEmail("u.murthy@tcs.com"));
		System.out.println(Utilities.isEmail("asaisiahsihasih"));

	}

}
