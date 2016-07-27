
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This is where all the awesome action happens
public class Anonymize {
	static Statement statement;
	static ErrorLogger errorLogger = new ErrorLogger();
	/*
	 * Private: Method that anonymizes a row in the database.
	 * 
	 * Parameter Description:
	 * 			conn - The connection object to the underlying database
	 * 			query - The SQL query string that gets executed on the underlying
	 * 					database. This is usually an update query
	 */
	private static boolean anonymizeRow(Connection conn, String query){
		boolean queryExecutionStatus = false;
		try{
			statement = conn.createStatement();
			queryExecutionStatus = statement.execute(query);
		}
		catch(Exception exp){
			System.out.println("Something went wrong:" + exp.getMessage());
		}
		return queryExecutionStatus;
	}
	
	/*
	 * Private: This method frames the update query to anonymize a given record
	 * 
	 * Parameter Description:
	 * 			tableName - The name of the table which is being anonymized
	 * 			primaryKeyValueMap - A Hashmap containing all primary keys and their values for a given row
	 * 			conditions - A conditional query which basically sets the selected columns data to 
	 * 						anonymized values
	 */
	private static String getUpdateQuery(String tableName, Map<String, Object> primaryKeyValueMap, String conditions){
		AppProperties appProperties = new AppProperties();
		String baseQuery = appProperties.getProperty("UPDATE_CLAUSE") + tableName + appProperties.getProperty("SET_CLAUSE");
		
		/*
		 * This conditions string will have a Comma character at the end. We need to remove this
		 */
		conditions = conditions.substring(0, conditions.length()-1 );
		baseQuery = baseQuery + conditions + getWhereQuery(primaryKeyValueMap);
		return baseQuery;
	}

	/*
	 * Private: Method that prepares the where clause in the update query for anonymizing
	 * 			a given row
	 * 
	 * Parameter Description:
	 * 			primaryKeyValueMap - This is a HashMap that contains the primary key names
	 * 								and their values in a given row
	 */
	private static String getWhereQuery(Map<String, Object> primaryKeyValueMap){
		AppProperties appProperties = new AppProperties();
		String baseQuery = appProperties.getProperty("WHERE_CLAUSE");
		String groupConditionsClause = appProperties.getProperty("GROUP_CONDITIONS_CLAUSE");
		int primaryKeysCount = primaryKeyValueMap.size();

		for (Map.Entry<String, Object> primaryKeyValueEntry : primaryKeyValueMap.entrySet()) {
			baseQuery += getConditionForEntry(primaryKeyValueEntry.getKey(), primaryKeyValueEntry.getValue());
			if(primaryKeysCount > 1 && (primaryKeysCount - 1 ) > 0){
				baseQuery += groupConditionsClause;
				primaryKeysCount--;
			}
		}
		return baseQuery;
	}

	/*
	 * Private: This method frames the right conditional statement based on the data type of
	 * 			a given primary key value. If the value is a NUMBER, then we will get a String like
	 * 			this: attribute=NUMBER_VALUE.
	 * 
	 * 			If the value is a string. We might have to encode the same in quotes. Hence the output
	 * 			would look like attribute='STRING_VALUE'
	 * 
	 * Parameter Description:
	 * 			primaryKeyName - Name of the primary key attribute
	 * 			primaryKeyValue - Value of the primary key attribute in a given row 
	 */
	private static String getConditionForEntry(String primaryKeyName, Object primaryKeyValue){
		String condition = primaryKeyName + "=";
		if(primaryKeyValue instanceof String)
		{
			condition += "'" + primaryKeyValue.toString() + "'";
		}
		else if(primaryKeyValue instanceof Integer){
			condition += Integer.parseInt(primaryKeyValue.toString());
		}
		else if(primaryKeyValue instanceof Double){
			condition += Double.parseDouble(primaryKeyValue.toString());
		}
		else if(primaryKeyValue instanceof Timestamp)
		{
			condition += "'" + primaryKeyValue.toString()  + "'";
		}
		else if(primaryKeyValue instanceof Time)
		{
			condition += "'" + primaryKeyValue.toString() + "'";
		}
		else
		{
			condition += "'" + primaryKeyValue.toString() + "'";
		}
		return condition;
	}

	/* Private: This method that gets invoked for anonymizing a given table
	 * 
	 * Parameter Description:
	 * 		conn - This is the basic connection object
	 * 		tableName - The table that is currently in scope for anonymization
	 * 		columnsList - The list of column names of columns that we would like
	 * 					  to anonymize
	 * 		primaryKey - Primary key of the given table
	 */
	private static boolean anonymizeTable(Connection conn, String tableName, List<String> columnsList, List<String> primaryKeys) throws UnsupportedEncodingException{
		AppProperties appProperties = new AppProperties();
		// First frame a result set with all the data in the table
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		System.out.println("Comes here to anonymize: " + tableName);
		// Frame the base query to get all the records
		//String sqlQuery = "select * from " + tableName;
		String sqlQuery = appProperties.getProperty("SELECT_ALL_CLAUSE") + tableName;
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
			Map<String, Object> primaryKeyValueMap = new HashMap<String, Object>();
			String primaryKeyDataType;
			Object primaryKeyValue;
			while(baseSet.next()){
				// Get the column type information for every primary key in a given table
				for ( String primaryKey : primaryKeys){
					primaryKeyDataType = allColumnDetails.get(primaryKey);
					if(Utilities.isStringType(primaryKeyDataType))
					{
						primaryKeyValue = baseSet.getString(primaryKey);
					}
					else if(Utilities.isIntegerType(primaryKeyDataType)){
						primaryKeyValue = baseSet.getInt(primaryKey);
					}
					else if(Utilities.isDoubleType(primaryKeyDataType)){
						primaryKeyValue = baseSet.getDouble(primaryKey);
					}
					else if(Utilities.isTimeType(primaryKeyDataType))
					{
						primaryKeyValue = baseSet.getTimestamp(primaryKey);
					}
					else
					{
						primaryKeyValue = baseSet.getString(primaryKey);
					}
					primaryKeyValueMap.put(primaryKey, primaryKeyValue);
				}
				// Frame conditions query for the given set of columns
				for(String columnName : columnsList){
					columnDataType = allColumnDetails.get(columnName);
					/*
					 * Consider numbers in the below logic
					 */
					if (columnDataType != null){
						if(Utilities.isStringType(columnDataType)){
							//System.out.println();
							conditionsQuery += (columnName + "=" + "'" + Encrypt.encodeString(baseSet.getString(columnName)) + "' ,");
						}
						else if(Utilities.isStringType(columnDataType)){
							conditionsQuery += (columnName + "=" + Integer.toString(Encrypt.encodeInteger(baseSet.getInt(columnName))) + " ,");
						}
						else if(Utilities.isDoubleType(columnDataType)){
							conditionsQuery += (columnName + "=" + Double.toString(Encrypt.encodeDouble(baseSet.getDouble(columnName))) + " ,");
						}
						else
						{
							errorLogger.write("The datatype "+columnDataType+ " is not supported by your application");
							System.out.println("Description column not available in table - " + tableName);
						}
					}
				}
				if(!conditionsQuery.isEmpty()){
					System.out.println(getUpdateQuery(tableName, primaryKeyValueMap, conditionsQuery));
					updateQuery = getUpdateQuery(tableName, primaryKeyValueMap, conditionsQuery);
					@SuppressWarnings("unused")
					boolean status = anonymizeRow(conn, updateQuery);
				}
				conditionsQuery = "";
			}
			baseSet.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Something went wrong: " + e.getMessage());
			//e.printStackTrace();
		}
		return true;
	}

	/*
	 * Public: Method that anonymizes a given database.
	 * 
	 * Parameter Description:
	 * 				conn - The connection object to connect and alter the underlying
	 * 						database
	 * 				ignoreTablesList - List of tables that we would like to ignore
	 * 						during our anonymization. In most of the applications, we
	 * 						might have settings data in a table named 'Settings'. We might
	 * 						want to ignore such tables.
	 */
	public static boolean anonymizeDatabase(Connection conn, List<String> ignoreTablesList){
		boolean result = false;
		try{
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs = md.getTables(null, null, "%", null);
			System.out.println("Tables to be anonymized are:");
			List<String> primaryKeys;
			// The below list contains the column names to be Anonymized
			List<String> columnsList = new ArrayList<String>();
			columnsList.add("Description");
//			columnsList.add("Name");
//			columnsList.add("AddressStreet");
//			columnsList.add("FriendlyDescription");
//			columnsList.add("FriendlyFutureDescription");
//			columnsList.add("AccountName");
//			columnsList.add("Quanitity");
//			columnsList.add("CostValueAmountinInstrumentCurrency");
//			columnsList.add("AccountBalanceInAccountCurrency");
//			columnsList.add("AccountBalanceInFundCurrency");
			columnsList.add("CompartmentName");
			columnsList.add("FundName");
			while (rs.next()){
				/*
				 * #TODO: The line beneath is shitty. Look for a better way to retrieve
				 * 		the table name instead of passing the hard coded constant 3
				 */
				String tableName = rs.getString(3);
				primaryKeys = Utilities.getPrimaryKeys(md, tableName);

				/*
				 * We should not anonymize the tables in Ignored List
				 * Also, for tables without a primary key(s), do not do anonymization
				 * # TODO: Think about anonymizing the tables without primary key(s) attribute
				 * 			defined
				 */
				if(!ignoreTablesList.contains(tableName) && !primaryKeys.isEmpty()){
					anonymizeTable(conn, tableName, columnsList, primaryKeys);
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
