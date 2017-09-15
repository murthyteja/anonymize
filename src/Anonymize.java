import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This is where all the awesome action happens
public class Anonymize {
	static Statement statement;
	AppLogger appLogger;
	AppLogger errorLogger;
	AppLogger statusLogger;

	public Anonymize(AppLogger appLogger, AppLogger errorLogger, AppLogger statusLogger){
		this.appLogger = appLogger;
		this.errorLogger = errorLogger;
		this.statusLogger = statusLogger;
	}
	
	/*
	 * Private: Method that anonymizes a row in the database.
	 * 
	 * Parameter Description:
	 * 			conn - The connection object to the underlying database
	 * 			query - The SQL query string that gets executed on the underlying
	 * 					database. This is usually an update query
	 */
	private boolean anonymizeRow(Connection conn, String query){
		boolean queryExecutionStatus = false;
		try{
			statement = conn.createStatement();
			queryExecutionStatus = statement.execute(query);
		}
		catch(Exception exp){
			//System.out.println("Something went wrong: " + exp.getMessage());
			//exp.printStackTrace();
			errorLogger.write("Something went wrong while executing query: " + exp.getMessage());;
		}
		finally{
			try {
				statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//System.out.println("Something went wrong while closing statement: " + e.getMessage());
				errorLogger.write("Something went wrong while closing statement: " + e.getMessage());
				e.printStackTrace();
			}
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
	private String getUpdateQuery(String tableName, Map<String, Object> primaryKeyValueMap, String conditions){
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
	private String getWhereQuery(Map<String, Object> primaryKeyValueMap){
		AppProperties appProperties = new AppProperties();
		String baseQuery = appProperties.getProperty("WHERE_CLAUSE");
		String groupConditionsClause = appProperties.getProperty("GROUP_CONDITIONS_CLAUSE");
		int primaryKeysCount = primaryKeyValueMap.size();

		for (Map.Entry<String, Object> primaryKeyValueEntry : primaryKeyValueMap.entrySet()) {
			String temp = null;
			Object temp1 = primaryKeyValueEntry.getValue();
			if( temp1 != null){
				temp = primaryKeyValueEntry.getValue().toString();
			}
			if(temp != null && !temp.contains("\"") && !temp.contains("\'")){
				baseQuery += getConditionForEntry(primaryKeyValueEntry.getKey(), primaryKeyValueEntry.getValue());
				if(primaryKeysCount > 1 && (primaryKeysCount - 1 ) > 0){
					baseQuery += groupConditionsClause;
				}
			}
			temp = null;
			temp1 = null;
			primaryKeysCount--;
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
	private String getConditionForEntry(String primaryKeyName, Object primaryKeyValue){
		String condition = primaryKeyName + "=";
		AppProperties appProperties = new AppProperties();
		int vendorId = Integer.parseInt(appProperties.getProperty("vendor_id"));
		if(primaryKeyValue instanceof String)
		{
			if(vendorId == 3){
				condition = primaryKeyName + appProperties.getProperty("LIKE_CLAUSE");
			}
			condition += "\'" + primaryKeyValue.toString() + "\'";
		}
		else if(primaryKeyValue instanceof Integer){
			condition += Integer.parseInt(primaryKeyValue.toString());
		}
		else if(primaryKeyValue instanceof Long){
			condition += Long.parseLong(primaryKeyValue.toString());
		}
		else if(primaryKeyValue instanceof Double){
			condition += Double.parseDouble(primaryKeyValue.toString());
		}
		else if(primaryKeyValue instanceof Timestamp)
		{
			condition += "\"" + primaryKeyValue.toString()  + "\"";
		}
		else if(primaryKeyValue instanceof Time)
		{
			condition += "\"" + primaryKeyValue.toString() + "\"";
		}
		else if(primaryKeyValue instanceof Date)
		{
			//condition += "DATE\'" + primaryKeyValue.toString() + "\'";
			condition += " TO_DATE(\'" + primaryKeyValue.toString() + "\', 'YYYY-MM-DD')";
		}
		else
		{
			condition += "\"" + primaryKeyValue.toString() + "\"";
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
	private boolean anonymizeTable(Connection conn, String tableName, List<String> columnsList, List<String> primaryKeys) throws UnsupportedEncodingException{
		AppProperties appProperties = new AppProperties();
		int vendorId = Integer.parseInt(appProperties.getProperty("vendor_id"));
		// First frame a result set with all the data in the table
		// System.out.println("Comes here to anonymize: " + tableName);
		appLogger.write("Comes here to anonymize: " + tableName);
		// Frame the base query to get all the record
		String sqlQuery = appProperties.getProperty("SELECT_ALL_CLAUSE") + tableName;
		int rowNumber = 0;
		int rowProblems = 0;
		try {
			Statement st;
			ResultSet baseSet;
			st = conn.createStatement();
			baseSet = st.executeQuery(sqlQuery);
			
			Map<String, String> allColumnDetails = new HashMap<String, String>();
			if (baseSet != null){
				appLogger.write("Table: " + tableName +" is not blank..............");
				statusLogger.write("Started processing: " + tableName);
				System.out.println("Started processing: " + tableName);
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
			Map<String, Object> currentRecordKeyValueMap = new HashMap<String, Object>();
			String primaryKeyDataType;
			Object primaryKeyValue;
			String currentRecordDataType;
			Object currentRecordValue;
			//Statement batchStatement;
			//batchStatement = conn.createStatement();
//			conn.setAutoCommit(false);
			int batchCount = 0;
			int updatedRecords = 0;
			while(baseSet.next()){
				rowNumber++;
				// If the Map is not cleared, the values just get replaced next time around
				currentRecordKeyValueMap.clear();
				// Get the column type information for every primary key in a given table
				for ( String primaryKey : primaryKeys){
					primaryKeyDataType = allColumnDetails.get(primaryKey);
					if(Utilities.isStringType(primaryKeyDataType))
					{
						primaryKeyValue = baseSet.getString(primaryKey);
					}
					else if(Utilities.isIntegerType(primaryKeyDataType)){
						primaryKeyValue = baseSet.getLong(primaryKey);
					}
					else if(Utilities.isDoubleType(primaryKeyDataType)){
						primaryKeyValue = baseSet.getDouble(primaryKey);
					}
					else if(Utilities.isTimeType(primaryKeyDataType))
					{
						primaryKeyValue = baseSet.getTimestamp(primaryKey);
					}
					else if(Utilities.isDateType(primaryKeyDataType)){
						primaryKeyValue = baseSet.getDate(primaryKey);
					}
					else
					{
						primaryKeyValue = baseSet.getString(primaryKey);
					}
					primaryKeyValueMap.put(primaryKey, primaryKeyValue);
				}
				// Get column value information for every column in a  given table
				
				for (String columnName : allColumnDetails.keySet()){
					currentRecordDataType = allColumnDetails.get(columnName);
					if(Utilities.isStringType(currentRecordDataType))
					{
						currentRecordValue = baseSet.getObject(columnName);
						if (currentRecordValue != null){
						  currentRecordValue = baseSet.getString(columnName);
						}
					}
					else if(Utilities.isIntegerType(currentRecordDataType)){
						currentRecordValue = baseSet.getObject(columnName);
						if (currentRecordValue != null){
							currentRecordValue = baseSet.getLong(columnName);
						}
					}
					else if(Utilities.isDoubleType(currentRecordDataType)){
						currentRecordValue = baseSet.getDouble(columnName);
					}
					else if(Utilities.isTimeType(currentRecordDataType))
					{
						currentRecordValue = baseSet.getTimestamp(columnName);
					}
					else if(Utilities.isDateType(currentRecordDataType))
					{
						currentRecordValue = baseSet.getDate(columnName);
					}
					else
					{
						currentRecordValue = baseSet.getObject(columnName);
						if (currentRecordValue != null){
						  currentRecordValue = baseSet.getString(columnName);
						}
					}

					if(!Utilities.isDateType(currentRecordDataType) && currentRecordValue != null){
						currentRecordKeyValueMap.put(columnName, currentRecordValue);
					}
				}
				// Frame conditions query for the given set of columns
				for(String columnName : columnsList){
					columnDataType = allColumnDetails.get(columnName);
					/*
					 * Consider numbers in the below logic
					 */
					if (columnDataType != null){
						if(Utilities.isStringType(columnDataType) || Utilities.isTextType(columnDataType)){
							if(vendorId == 1){
								conditionsQuery += (columnName + "=" + "\"" + Encrypt.encodeString(baseSet.getString(columnName)) + "\" ,");
							}
							else
							{
								// For Oracle and SQL Server DB, we need to use single quotes while specifying strings in conditional statements
								// For eg: UPDATE FUNDS set FUND_NAME='1asdasd2' where ID=1234;
								conditionsQuery += (columnName + "=" + "'" + Encrypt.encodeString(baseSet.getString(columnName)) + "' ,");
							}	
						}
						else if(Utilities.isIntegerType(columnDataType)){
							conditionsQuery += (columnName + "=" + Long.toString(Encrypt.encodeInteger(baseSet.getInt(columnName))) + " ,");
						}
						else if(Utilities.isDoubleType(columnDataType)){
							conditionsQuery += (columnName + "=" + Double.toString(Encrypt.encodeDouble(baseSet.getDouble(columnName))) + " ,");
						}
						else
						{
							errorLogger.write("The Datatype " +columnDataType + " doesn't have a default masking logic in your application");
						}
					}
				}
				if(!conditionsQuery.isEmpty()){
					if(primaryKeys.isEmpty()){
						//System.out.println(getUpdateQuery(tableName, currentRecordKeyValueMap, conditionsQuery));
						updateQuery = getUpdateQuery(tableName, currentRecordKeyValueMap, conditionsQuery);
					}
					else{
						//System.out.println(getUpdateQuery(tableName, primaryKeyValueMap, conditionsQuery));
						updateQuery = getUpdateQuery(tableName, primaryKeyValueMap, conditionsQuery);
					}
//					@SuppressWarnings("unused")
					boolean status = anonymizeRow(conn, updateQuery);
					appLogger.write("Processing Row: " + rowNumber + " With Query: " + updateQuery);
					conn.commit();
//					batchCount++;
//					updatedRecords++;
//					if (batchCount < 50){
//						batchStatement.addBatch(updateQuery);
//					}
//					if (batchCount == 50){
//						batchStatement.addBatch(updateQuery);
//						int [] recordsAffected = batchStatement.executeBatch();
//						errorLogger.write("Executing batch now");
//						System.out.println("==================================");
//						System.out.println(updatedRecords);
//						System.out.println("==================================");
//						batchCount = 0;
//						conn.commit();
//					}
				}
				conditionsQuery = "";
			}
			// Do not move the below close statement's from the try block, The baseset and st objects are closed after each table
			// is processed and their position here is correct.
			baseSet.close();
			st.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//System.out.println("Something went wrong: " + e.getMessage());
			//e.printStackTrace();
			rowProblems++;
			errorLogger.write("Something went in Table: " + tableName + e.getMessage());
			errorLogger.write("Row Number: " + rowNumber);
		}
		finally{
			//statement.close();
			statusLogger.write("Finished Processing " + tableName);
			statusLogger.write("Number of rows with Issues: " + rowProblems);
			System.out.println("Finished Processing " + tableName);
			System.out.println("Number of rows with Issues: " + rowProblems);
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
	public boolean anonymizeDatabase(Connection conn){
		boolean result = false;
		try{
			DatabaseMetaData md = conn.getMetaData();
			AnonymizationDetails anonymizationDetails = new AnonymizationDetails();
			ArrayList<String> tables = Collections.list(anonymizationDetails.getTables());
			for(String tableName : tables){
				List<String> primaryKeys = Utilities.getPrimaryKeys(md, tableName);
				List<String> columnsList = Arrays.asList(anonymizationDetails.getColumnNames(tableName));
				anonymizeTable(conn, tableName, columnsList, primaryKeys);
			}
		}
		catch(Exception exp) {
			System.out.println("Something went wrong: " + exp.getMessage());
			exp.printStackTrace();
		}
		return result;
	}
}
