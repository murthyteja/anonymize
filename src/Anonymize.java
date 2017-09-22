/*
 * Author: Sri Murthy Upadhyayula
 * 
 * BIL ID: XQW9X
 * 
 * Class Description:
 * 				This is the module where the entire process logic for database
 * anonymization resides
 * 
 */
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
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
	int vendorId;
	boolean useRowNum;
	boolean useBatchUpdation;
	int batchSize;
	AppProperties appProperties;

	public Anonymize(AppLogger appLogger, AppLogger errorLogger, AppLogger statusLogger){
		appProperties = new AppProperties();
		vendorId = Integer.parseInt(appProperties.getProperty("vendor_id"));
		useRowNum = Boolean.parseBoolean(appProperties.getProperty("use_rownum"));
		useBatchUpdation = Boolean.parseBoolean(appProperties.getProperty("batch_switch"));
		batchSize = Integer.parseInt(appProperties.getProperty("batch_size"));
		this.appLogger = appLogger;
		this.errorLogger = errorLogger;
		this.statusLogger = statusLogger;
	}
	
	
	/*
	 * Private: For each vendor, the row_number might have a different DSL. So, we have to write a method
	 * 			that would get the right where query
	 * 
	 * Returns a string
	 */
	private String getRowNumberQuerySuffix()
	{
		return " row_num = ?";
	}
	
	/*
	 * Private: This method frames the base Where query for the prepared statement SQL.
	 * 			
	 * Returns a string
	 */
	private String getPreparedStatementWhereQuery(Map<String, Object> conditionalAttributesKeyValueMap)
	{
		String whereQuery = appProperties.getProperty("WHERE_CLAUSE");
		String groupConditionsClause = appProperties.getProperty("GROUP_CONDITIONS_CLAUSE");
		
		if(useRowNum){
			whereQuery += getRowNumberQuerySuffix(); 
		}
		else
		{
			int conditionalKeysCount = conditionalAttributesKeyValueMap.size();
			for (Map.Entry<String, Object> primaryKeyValueEntry : conditionalAttributesKeyValueMap.entrySet()) {
				whereQuery += primaryKeyValueEntry.getKey() + "=?";
				if(conditionalKeysCount > 1 && (conditionalKeysCount - 1) > 0){
					whereQuery += groupConditionsClause;
					conditionalKeysCount-- ;
				} 
			}
		}
		return whereQuery;
	}

	/*
	 * Private: This method prepares the base for PreparedStatement.
	 * 			The reason why we are going for prepared statement is to avoid
	 * 			re-compilation of the query at the database end every time the 
	 * 			adapter does a round trip to the database.
	 * 
	 * Returns String
	 */
	private String getPreparedStatementSQLBase(String tableName, List<String> columnsList, Map<String, Object> conditionalAttributesKeyValueMap){
		String baseQuery = appProperties.getProperty("UPDATE_CLAUSE") + tableName + appProperties.getProperty("SET_CLAUSE");
		for(String columnName : columnsList){
			baseQuery += columnName + "=? ,";
		}
		/*
		 * This baseQuery string will have a Comma character at the end. We need to remove this
		 */
		baseQuery = baseQuery.substring(0, baseQuery.length()-1 );
		
		// Add the where clause to the Prepared Statement

		baseQuery += getPreparedStatementWhereQuery(conditionalAttributesKeyValueMap);

		return baseQuery;
	}
	
	/*
	 * Private: Method that fetches the final prepared statement which is ready for execution
	 * 
	 * Returns a preparedStatement object
	 */
	private PreparedStatement getPreparedStatementForExecutation(PreparedStatement updatePreparedStatementObject, Map<String, String> allColumnDetails, List<String> columnsList, Map<String, Object> currentRecordKeyValueMap, int rowNumber, Map<String, Object> primaryKeyValueMap) throws Exception{
		
		// First set the values for the columns that we intend to Anonymize
		int parameterCount = 1;
		String columnType = "";
		for(String columnName : columnsList){
			columnType = allColumnDetails.get(columnName);
			if (columnType != null){
				if(Utilities.isStringType(columnType) || Utilities.isTextType(columnType)){
					updatePreparedStatementObject.setString(parameterCount, Encrypt.encodeString(String.valueOf(currentRecordKeyValueMap.get(columnName))));
				}
				else if(Utilities.isIntegerType(columnType)){
					updatePreparedStatementObject.setLong(parameterCount, Encrypt.encodeLong((Long) currentRecordKeyValueMap.get(columnName)));
				}
				else if(Utilities.isDoubleType(columnType)){
					updatePreparedStatementObject.setDouble(parameterCount, Encrypt.encodeDouble((Double) currentRecordKeyValueMap.get(columnName)));
				}
				else
				{
					errorLogger.write("The Datatype " + columnType + " doesn't have a default masking logic in your application");
				}
			}
			parameterCount++;
		}
		/*
		 * The above block ensures all the anonymization is done and values are set correctly in the Prepared Statement
		 * Now, we need to ensure that the conditional filter (where part of the query) is correctly prepared
		 */
		if(useRowNum){
			// Not that we do not increment parameterCount here as it is already incremented in the previous iteration
			updatePreparedStatementObject.setInt(parameterCount, rowNumber);
		}
		else{
			/*
			 * If we do not use RowNum to prepare the statement, then we need to fill in the values based on the presence
			 * of Primary Keys in the tables
			 */
			Map<String, Object> conditionalParameterValuesMap = new HashMap<String, Object>();
			if(primaryKeyValueMap == null || primaryKeyValueMap.isEmpty()){
				conditionalParameterValuesMap = currentRecordKeyValueMap;
			}
			else {
				conditionalParameterValuesMap = primaryKeyValueMap;
			}
			/*
			 * At this point, we have the right conditional parameter values Map with us, we are left with finishing up
			 * the prepared statement
			 */
			for(Map.Entry<String, Object> conditionalParameterEntry : conditionalParameterValuesMap.entrySet()){
				columnType = allColumnDetails.get(conditionalParameterEntry.getKey());
				if (columnType != null){
					if(Utilities.isStringType(columnType) || Utilities.isTextType(columnType)){
						updatePreparedStatementObject.setString(parameterCount, (String) conditionalParameterEntry.getValue());
					}
					else if(Utilities.isIntegerType(columnType)){
						updatePreparedStatementObject.setLong(parameterCount, (Long) conditionalParameterEntry.getValue());
					}
					else if(Utilities.isDoubleType(columnType)){
						updatePreparedStatementObject.setDouble(parameterCount, (Double) conditionalParameterEntry.getValue());
					}
					else
					{
						errorLogger.write("Mess up in where clause preparation. The Datatype "
								+ columnType + " doesn't have a default masking logic in your application");
					}
				}
				parameterCount++;
			}
			
		}
		return updatePreparedStatementObject;
	}
	
	/*
	 * Private: Method that Anonymizes a row in the database.
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
			errorLogger.write("Something went wrong while executing query: " + exp.getMessage());;
		}
		finally{
			try {
				statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
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
	
	/*
	 * Private: Method that takes a Result Set Reference as input and gets the primary key,
	 *          value information for all the primary keys in that table
	 */
	private Map<String, Object> getPrimaryKeyValueDetails(ResultSet baseSet, List<String> primaryKeys, Map<String, String> allColumnDetails) throws Exception
	{
		Map<String, Object> primaryKeyValueMap = new HashMap<String, Object>();
		String primaryKeyDataType;
		Object primaryKeyValue;
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
		return primaryKeyValueMap;
	}
	
	/*
	 * Private: Method that takes a Result Set Reference as input and gets current record
	 *          value information for all the columns
	 */
	private Map<String, Object> getCurrentRecordKeyValueDetails(ResultSet baseSet, Map<String, String> allColumnDetails) throws Exception
	{
		Map<String, Object> currentRecordKeyValueMap = new HashMap<String, Object>();
		String currentRecordDataType;
		Object currentRecordValue;
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
		return currentRecordKeyValueMap;
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
		
		String updateQuery = "";

		/*
		 * ==============================================================
		 * STEP 1:  For each table in the scope for anonymization, frame
		 * 			a result set with all the data in the table
		 * ==============================================================
		 * 
		 */
		appLogger.write("Comes here to anonymize: " + tableName);
		// Frame the base query to get all the record
		String sqlQuery = appProperties.getProperty("SELECT_ALL_CLAUSE") + tableName;
		int rowNumber = 0;
		int rowProblems = 0;
		int batchCounter = 0;
		try {
			Statement st;
			ResultSet baseSet;
			st = conn.createStatement();
			PreparedStatement updatePreparedStatementObject;
			
			/*
			 * 
			 * BASE SET NOW HAS ALL THE ROWS IN THE TABLE UNDER CONSIDERATION
			 * 
			 */
			baseSet = st.executeQuery(sqlQuery);
			statusLogger.write("Started processing: " + tableName);
			System.out.println("Started processing: " + tableName);

			/*
			 * =======================================================================================
			 * STEP 2: TABLE PROCESSING
			 * =======================================================================================
			 */
			if (baseSet != null){
				appLogger.write("Table: " + tableName +" is not blank.");
				System.out.println("Table: " + tableName +" is not blank.");

				Map<String, String> allColumnDetails = new HashMap<String, String>();
				Map<String, Object> currentRecordKeyValueMap = new HashMap<String, Object>();
				Map<String, Object> primaryKeyValueMap = new HashMap<String, Object>();
				String basePreparedStatementSQL;

				/*
				 * =======================================================================================
				 * STEP 2.1: Get Table Meta Data. This is where the tool captures the columns in the table
				 *           and details about their data type (Meta Data of columns in the table)
				 * =======================================================================================
				 */
				ResultSetMetaData rsmd = baseSet.getMetaData();
				// Feed all the column details before calling anonymizeRow() method
				int i = 0, rowID = 0;
				
				/*
				 * =======================================================================================
				 * By the end of this block, allColumnDetails will have <ColumnName, DataType> Information
				 * for all the columns in the table
				 * =======================================================================================
				 * 
				 */
				while(i < rsmd.getColumnCount()){
					i++;
					allColumnDetails.put(rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
				}				
				
				while(baseSet.next())
				{
					rowNumber++;
					rowID = baseSet.getRow();
					// If the Map is not cleared, the values just get replaced next time around
					currentRecordKeyValueMap.clear();

					// Get column value information for every column in a  given table
					
					currentRecordKeyValueMap = getCurrentRecordKeyValueDetails(baseSet, allColumnDetails);

					/*
					 * =====================================================================================
					 * STEP 2.2: Frame the update query using Row Num or Primary Key Information
					 * 
					 * # TODO: Run java equivalent bench mark tests and check for the performance
					 * of this module given the size of  the database
					 * =====================================================================================
					 */
					if(useRowNum) // (STEP 2.2.1)
					{
						/*
						 * STEP 2.2.1.A
						 * 
						 * If we use ROW NUM for anonymization, the format would be as follows
						 * 
						 * UPDATE TABLE_NAME SET COLUMN1=<value>, COLUMN2=<VALUE> where rownum=<value>
						 * 
						 */
						System.out.println("Here to anonymize using RowNum baby");
						basePreparedStatementSQL = getPreparedStatementSQLBase(tableName, columnsList, null);
					}
					else // (STEP 2.2.2)
					{
						if(!primaryKeys.isEmpty()){
							primaryKeyValueMap = getPrimaryKeyValueDetails(baseSet, primaryKeys, allColumnDetails);
							basePreparedStatementSQL = getPreparedStatementSQLBase(tableName, columnsList, primaryKeyValueMap);
						}
						else{
							basePreparedStatementSQL = getPreparedStatementSQLBase(tableName, columnsList, currentRecordKeyValueMap);
						}
					}
					
					/* 
					 * ==============================================================================================================
					 * At the end of this step, we have the base query to form the prepared statement object
					 * ==============================================================================================================
					 */
					updatePreparedStatementObject = conn.prepareStatement(basePreparedStatementSQL);

					// Now we need to set values into the Prepared Statement Object
					if(primaryKeys.isEmpty())
					{
						updatePreparedStatementObject = getPreparedStatementForExecutation(updatePreparedStatementObject, allColumnDetails, columnsList, currentRecordKeyValueMap, rowID, null);
					}
					else{
						updatePreparedStatementObject = getPreparedStatementForExecutation(updatePreparedStatementObject, allColumnDetails, columnsList, currentRecordKeyValueMap, rowID, primaryKeyValueMap);
					}
					
					/*
					 * Step 3: Execute the query based on the query execution settings (Update by batch/Update each row)
					 */
					if(useBatchUpdation){
						batchCounter++;
						if(batchCounter == batchSize){
							int[] recordsUpdated = updatePreparedStatementObject.executeBatch();
						}
						else{
							updatePreparedStatementObject.addBatch();
						}
						
					}
					else{
						updatePreparedStatementObject.execute();
					}
				}
			}
			else
			{
				appLogger.write("Table: " + tableName +" is blank.");
				System.out.println("Table: " + tableName +" is blank.");
			}
			

//				String conditionsQuery = "";
//				String columnDataType;
//				Map<String, Object> primaryKeyValueMap = new HashMap<String, Object>();
//				Map<String, Object> currentRecordKeyValueMap = new HashMap<String, Object>();
//				String primaryKeyDataType;
//				Object primaryKeyValue;
//				String currentRecordDataType;
//				Object currentRecordValue;
//				//Statement batchStatement;
//				//batchStatement = conn.createStatement();
//				//conn.setAutoCommit(false);
//				int batchCount = 0;
//				int updatedRecords = 0;
//				while(baseSet.next()){
//					rowNumber++;
//					// If the Map is not cleared, the values just get replaced next time around
//					currentRecordKeyValueMap.clear();
//					
//					/*
//					 * ============================================================================
//					 * Get the column type information for every primary key in a given table
//					 * ============================================================================
//					 */
//
//					
//
//				// Frame conditions query for the given set of columns
//				for(String columnName : columnsList){
//					columnDataType = allColumnDetails.get(columnName);
//					/*
//					 * Consider numbers in the below logic
//					 */
//					if (columnDataType != null){
//						if(Utilities.isStringType(columnDataType) || Utilities.isTextType(columnDataType)){
//							if(vendorId == 1){
//								conditionsQuery += (columnName + "=" + "\"" + Encrypt.encodeString(baseSet.getString(columnName)) + "\" ,");
//							}
//							else
//							{
//								// For Oracle and SQL Server DB, we need to use single quotes while specifying strings in conditional statements
//								// For eg: UPDATE FUNDS set FUND_NAME='1asdasd2' where ID=1234;
//								conditionsQuery += (columnName + "=" + "'" + Encrypt.encodeString(baseSet.getString(columnName)) + "' ,");
//							}	
//						}
//						else if(Utilities.isIntegerType(columnDataType)){
//							conditionsQuery += (columnName + "=" + Long.toString(Encrypt.encodeInteger(baseSet.getInt(columnName))) + " ,");
//						}
//						else if(Utilities.isDoubleType(columnDataType)){
//							conditionsQuery += (columnName + "=" + Double.toString(Encrypt.encodeDouble(baseSet.getDouble(columnName))) + " ,");
//						}
//						else
//						{
//							errorLogger.write("The Datatype " +columnDataType + " doesn't have a default masking logic in your application");
//						}
//					}
//				}
//				
//				if(!conditionsQuery.isEmpty()){
//					if(primaryKeys.isEmpty()){
//						updateQuery = getUpdateQuery(tableName, currentRecordKeyValueMap, conditionsQuery);
//					}
//					else{
//						updateQuery = getUpdateQuery(tableName, primaryKeyValueMap, conditionsQuery);
//					}
////					batchCount++;
////					updatedRecords++;
////					if (batchCount < 50){
////						batchStatement.addBatch(updateQuery);
////					}
////					if (batchCount == 50){
////						batchStatement.addBatch(updateQuery);
////						int [] recordsAffected = batchStatement.executeBatch();
////						errorLogger.write("Executing batch now");
////						System.out.println("==================================");
////						System.out.println(updatedRecords);
////						System.out.println("==================================");
////						batchCount = 0;
////						conn.commit();
////					}
//				}
//
//				/*
//				 * ===================================================================================================
//				 * STEP 5: Once the update query is properly framed, then run the query
//				 * ===================================================================================================
//				 */
//				if(!updateQuery.isEmpty()){
//					boolean status = anonymizeRow(conn, updateQuery);
//					appLogger.write("Processing Row: " + rowNumber + " With Query: " + updateQuery + "Status: " + status);
//					conn.commit();
//				}
//				conditionsQuery = "";
//			}

			// Do not move the below close statement's from the try block, The baseset and st objects are closed after each table
			// is processed and their position here is correct.
			baseSet.close();
			st.close();

		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
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
