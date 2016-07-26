import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) throws UnsupportedEncodingException {
		// Initialize Application Properties Object
		AppProperties app = new AppProperties();
		
		// Testing a list of Tables to be ignored
		List<String> ignoreTablesList = new ArrayList<String>();
		ignoreTablesList.add("GlobalSettings");
		// Initialize the Data Source Object
		DataSource ds = new DataSource();
		Anonymize.anonymizeDatabase(ds.conn, ignoreTablesList);
	}
}
