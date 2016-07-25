import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
	
	/*
	* Get the primary key in the given table
	* #TODO: Handle composite primary Keys condition
	*/
	public static List<String> getPrimaryKeys(DatabaseMetaData dmd, String tableName) throws SQLException{
		ResultSet rs = null;
		String conditionalPrimaryKey = null;
		List<String> primaryKeys = new ArrayList<String>();
		rs = dmd.getPrimaryKeys(null, null, tableName);
		while (rs.next()) {
			conditionalPrimaryKey = rs.getString("COLUMN_NAME");
			if(!conditionalPrimaryKey.isEmpty() && conditionalPrimaryKey != null){
				primaryKeys.add(conditionalPrimaryKey);
			}
		}
		return primaryKeys;
	}
}
