import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// This is where all the action happens
public class Anonymize {
	static Statement statement;

	private static boolean anonymizeRow(Connection conn, String query){
		try{
			statement = conn.createStatement();
			boolean status = statement.execute(query);
			System.out.print("''''''''''''''''''''''''''''''");
			System.out.print(query);
			System.out.print("Result of the Query is: " + status);
			System.out.print("''''''''''''''''''''''''''''''");
		}
		catch(Exception exp){
			System.out.println("Something went wrong:" + exp.getMessage());
		}
		return true;
	}
	
	/*
	 * This method frames the update query to anonymize a given record
	 */
	private static String getUpdateQuery(String tableName, String primaryKeyName, Object primaryKeyValue, String conditions){
		String baseQuery = "update " + tableName + " set ";
		/*
		 * This conditions string will have a Comma character at the end. We need to remove this
		 */
		conditions = conditions.substring(0, conditions.length()-1 );
		baseQuery = baseQuery + conditions + " where " + primaryKeyName + "=";
		if(primaryKeyValue instanceof String)
		{
			baseQuery += "'" + primaryKeyValue.toString() + "'";
		}
		else if(primaryKeyValue instanceof Integer){
			baseQuery += Integer.parseInt(primaryKeyValue.toString());
		}
		else if(primaryKeyValue instanceof Date)
		{
			baseQuery += "'" + primaryKeyValue.toString() + "'";
		}
		else
		{
			baseQuery += "'" + primaryKeyValue.toString() + "'";
		}
		return baseQuery;
	}

	/*
	 * Parameter Description:
	 * 		conn - This is the basic connection object
	 * 		tableName - The table that is currently in scope for anonymization
	 * 		columnsList - The list of column names of columns that we would like
	 * 					  to anonymize
	 * 		primaryKey - Primary key of the given table
	 */
	private static boolean anonymizeTable(Connection conn, String tableName, List<String> columnsList, String primaryKey) throws UnsupportedEncodingException{
		// First frame a result set with all the data in the table
		System.out.println("Comes here to anonymize: " + tableName);
		System.out.println("The primary Key is: " + primaryKey);
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		// Frame the base query to get all the records
		String sqlQuery = "select * from " + tableName;
		try {
			Statement st = conn.createStatement();
			ResultSet baseSet = st.executeQuery(sqlQuery);
			System.out.println(sqlQuery);
			Map<String, String> allColumnDetails = new HashMap<String, String>();
			if (baseSet != null){
				ResultSetMetaData rsmd = baseSet.getMetaData();
				// Feed all the column details before calling anonymizeRow() method
				int i = 0;
				while(i < rsmd.getColumnCount()){
					i++;
					allColumnDetails.put(rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
				}
			}
			/*
			 * Now here is all the dirty and bulky work. Frame the right update query
			 * using the primary key and columns information and update the shit right
			 * away.
			 * # TODO: Run java equivalent bench mark tests and check for the performance
			 * of this module given the size of  the database
			 */
			String conditionsQuery = "";
			String columnDataType;
			String updateQuery = "";
			String primaryKeyDataType;
			Object primaryKeyValue;
			while(baseSet.next()){
				// baseSet has the next row in scope now
				primaryKeyDataType = allColumnDetails.get(primaryKey);
				System.out.println("=========================================");
				System.out.println(primaryKeyDataType);
				System.out.println("=========================================");
				if(primaryKeyDataType == "VARCHAR")
				{
					primaryKeyValue = baseSet.getString(primaryKey);
				}
				else if(primaryKeyDataType == "INTEGER"){
					primaryKeyValue = baseSet.getInt(primaryKey);
				}
				else if(primaryKeyDataType == "TIMESTAMP")
				{
					primaryKeyValue = baseSet.getDate(primaryKey);
				}
				else
				{
					primaryKeyValue = baseSet.getString(primaryKey);
				}
				// Frame conditions query for the given set of columns
				for(String columnName : columnsList){
					columnDataType = allColumnDetails.get(columnName);
					/*
					 * Consider numbers in the below logic
					 */
					if (columnDataType != null){
						if(columnDataType == "VARCHAR"){
							//System.out.println();
							conditionsQuery += (columnName + "=" + "'" + Encrypt.encodeString(baseSet.getString(columnName)) + "' ,");
						}
						else
						{
							System.out.println("Description column not available in table - " + tableName);
						}
					}
				}
				if(!conditionsQuery.isEmpty()){
					System.out.println(getUpdateQuery(tableName, primaryKey, primaryKeyValue, conditionsQuery));
					updateQuery = getUpdateQuery(tableName, primaryKey, primaryKeyValue, conditionsQuery);
					@SuppressWarnings("unused")
					boolean status = anonymizeRow(conn, updateQuery);
				}
				conditionsQuery = "";
			}
			baseSet.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("Something went wrong: " + e.getMessage());
		}
		return true;
	}

	public static boolean anonymizeDatabase(Connection conn, List<String> ignoreTablesList){
		boolean result = false;
		try{
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs = md.getTables(null, null, "%", null);
			System.out.println("Tables to be anonymized are:");
			String primaryKey;
			// The below list contains the column names to be anonymized
			List<String> columnsList = new ArrayList<String>();
			columnsList.add("Description");
			columnsList.add("Name");
			columnsList.add("AddressStreet");
			columnsList.add("FriendlyDescription");
			columnsList.add("FriendlyFutureDescription");
			while (rs.next()){
				/*
				 * #TODO: The line beneath is shitty. Look for a better way to retrieve
				 * 		the table name instead of passing the hard coded constant 3
				 */
				String tableName = rs.getString(3);
				primaryKey = Utilities.getPrimaryKey(md, tableName);
				/*
				 * We should not anonymize the tables in Ignored List
				 * Also, for tables without a primary key, do not do anonymization
				 * # TODO: Think about anonymizing the tables without primary key attribute
				 * 			defined
				 */
				if(!ignoreTablesList.contains(tableName) && primaryKey != null && !primaryKey.isEmpty()){
					anonymizeTable(conn, tableName, columnsList, primaryKey);
				}
			}
		}
		catch(Exception exp) {
			System.out.println("Something went wrong: " + exp.getMessage());
			exp.printStackTrace();
		}
		return result;
	}
}
