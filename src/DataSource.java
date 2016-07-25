import java.sql.Connection;
import java.sql.DriverManager;

public class DataSource {
	Connection conn;
	String driverClassPath;
	String connectionString;
	public DataSource(){
		System.out.println("Constructor for Data Source");
		driverClassPath = "net.ucanaccess.jdbc.UcanaccessDriver";
		connectionString = "jdbc:ucanaccess://H:/Work/Anonymiye_Test/B23.mdb";
		try{
			Class.forName(driverClassPath);
			conn = DriverManager.getConnection(connectionString);
		}
		catch(Exception exp){
			System.out.println("Something went wrong: " + exp.getMessage());
		}
	}
}
