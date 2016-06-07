import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class Anonymize {
	
	@SuppressWarnings("unused")
	private boolean anonymizeRow(){
		return true;
	}

	private static boolean anonymizeTable(Connection conn, String tableName){
		// First frame a result set with all the data in the table
		System.out.println("Comes here to anonymize: " + tableName);
		String sqlQuery = "select * from " + tableName;
		try {
			Statement st = conn.createStatement();
			ResultSet baseSet = st.executeQuery(sqlQuery);
			if (baseSet != null){
				ResultSetMetaData rsmd = baseSet.getMetaData();
				int i = 0;
				while(i < rsmd.getColumnCount()){
					i++;
					System.out.println(rsmd.getColumnName(i) + ":------" + rsmd.getColumnTypeName(i));
				}
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
		System.out.println("Tables to be ignored are:");
		for(int i=0; i<ignoreTablesList.size();i++){
			System.out.println(ignoreTablesList.get(i));
		}
		try{
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs = md.getTables(null, null, "%", null);
			System.out.println("Tables to be anonymized are:");
			while (rs.next()){
				String tableName = rs.getString(3);
				// We should not anonymize the tables in Ignored List
				if(!ignoreTablesList.contains(tableName)){
					System.out.println(tableName);
					System.out.println("================================");
					anonymizeTable(conn, tableName);
				}
			}
		}
		catch(Exception exp) {
			System.out.println("Something went wrong: " + exp.getMessage());
		}
		return result;
	}
}
