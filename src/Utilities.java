import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;

public class Utilities {

	static final AppProperties appProperties = new AppProperties();
	/* 
	 * Public: Checks if the input string is an email or not
	 * 			by comparing with a regular expression
	 * 
	 * Parameter Description:
	 * 			string - Contains the input string for which an email
	 * 					pattern validation needs to be performed.
	 */
	public static boolean isEmail(String string){
		Pattern emailPattern = Pattern.compile(".+@.+\\.[a-z]+", Pattern.CASE_INSENSITIVE);
		boolean status = false;
		if (string != null && !string.isEmpty()){
			Matcher m = emailPattern.matcher(string);
			status = m.matches();
		}
		return status;
	}

	/* 
	 * Public: This method gets the primary keys in a given table
	 * 
	 * Parameter Description:
	 * 				dmd - The database metadata reference for a given table
	 * 				tableName - Stringified table name in the database
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
		rs.close();
		return primaryKeys;
	}

	/*
	 * Public: This method checks if the input type is a string based data type used
	 * 		   in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "VARCHAR2"
	 */
	public static boolean isStringType(String type){
		String[] stringDataTypeList = appProperties.getProperty("STRING_LIST").split(",");
		return ArrayUtils.contains(stringDataTypeList, type);
	}

	/*
	 * Public: This method checks if the input type is a text based data type used
	 * 		   in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "text"
	 */
	public static boolean isTextType(String type){
		String[] stringDataTypeList = appProperties.getProperty("TEXT_LIST").split(",");
		return ArrayUtils.contains(stringDataTypeList, type);
	}

	
	/*
	 * Public: This method checks if the input type is a integer based data type used
	 * 		   in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "INTEGER"
	 */
	public static boolean isIntegerType(String type){
		String[] stringDataTypeList = appProperties.getProperty("INTEGER_LIST").split(",");
		return ArrayUtils.contains(stringDataTypeList, type);
	}
	
	/*
	 * Public: This method checks if the input type is a floating point number 
	 * 		   based data type used in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "DOUBLE"
	 */
	public static boolean isDoubleType(String type){
		String[] stringDataTypeList = appProperties.getProperty("DOUBLE_LIST").split(",");
		return ArrayUtils.contains(stringDataTypeList, type);
	}
	
	/*
	 * Public: This method checks if the input type is a date time 
	 * 		   based data type used in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "DATETIME"
	 */
	public static boolean isTimeType(String type){
		String[] stringDataTypeList = appProperties.getProperty("TIME_LIST").split(",");
		return ArrayUtils.contains(stringDataTypeList, type);
	}
	
	/*
	 * Public: This method checks if the input type is a date 
	 * 		   based data type used in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "DATE"
	 */
	public static boolean isDateType(String type){
		String[] stringDataTypeList = appProperties.getProperty("DATE_LIST").split(",");
		return ArrayUtils.contains(stringDataTypeList, type);
	}

	/*
	 * Public: This method checks if the input type is a CLOB 
	 * 		   based data type used in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "CLOB"
	 */
	public static boolean isClobType(String type){
		String[] clobDataTypeList = appProperties.getProperty("CLOB_LIST").split(",");
		return ArrayUtils.contains(clobDataTypeList, type);
	}
	
	/*
	 * Public: This method checks if the input type is a CLOB 
	 * 		   based data type used in SQL or not
	 * 
	 * Parameter Description:
	 * 			type: String value like "CLOB"
	 */
	public static boolean isBlobType(String type){
		String[] blobDataTypeList = appProperties.getProperty("BLOB_LIST").split(",");
		return ArrayUtils.contains(blobDataTypeList, type);
	}
}
