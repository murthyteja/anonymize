import java.sql.Connection;
import java.sql.DriverManager;

public class DataSource {
	Connection conn;
	String driverClassPath;
	String connectionString;
	AppProperties appProperties;
	public DataSource(){
		appProperties = new AppProperties();
		System.out.println("Constructor for Data Source");
		driverClassPath = "net.ucanaccess.jdbc.UcanaccessDriver";
		connectionString = "jdbc:ucanaccess://H:/Work/Anonymiye_Test/B23.mdb";
		connectionString = appProperties.getProperty("connection_string");
		try{
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
			conn = DriverManager.getConnection("jdbc:ucanaccess://C:/Users/murthyteja/Desktop/sample.accdb");
		}
		catch(Exception exp){
			System.out.println("Something went wrong: " + exp.getMessage());
		}
	}
}
