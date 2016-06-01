import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class DataSource {
	Connection conn;
	public DataSource(){
		System.out.println("Constructor for Data Source");
		try{
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
			conn = DriverManager.getConnection("jdbc:ucanaccess://C:/Users/murthyteja/Desktop/Database1.accdb");
		}
		catch(Exception exp){
			System.out.println("Something went wrong: " + exp.getMessage());
		}
	}
}
