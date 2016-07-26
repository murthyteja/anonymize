import java.sql.Connection;
import java.sql.DriverManager;

public class DataSource {
	Connection conn;
	String driverClassPath;
	String connectionString;
	AppProperties appProperties;
	ErrorLogger errorLogger;
	
	/* Public: Constructor for Data Source. This function picks up the driver and
	 * 		   connection string details from the config.properties file and tries
	 * 		   to establish a connection to the database.
	 */
	public DataSource(){
		System.out.println("Connecting to the Data Source......");
		appProperties = new AppProperties();
		errorLogger = new ErrorLogger();
		driverClassPath = appProperties.getProperty("driver_class");
		connectionString = appProperties.getProperty("connection_string");
		try{
			Class.forName(driverClassPath);
			conn = DriverManager.getConnection(connectionString);
			System.out.println("Connecting Established......");
		}
		catch(Exception exp){
			System.out.println("Something went wrong: " + exp.getMessage());
			errorLogger.write("Failed to connect to the data source:");
			errorLogger.write(exp.getMessage());
		}
	}
}
