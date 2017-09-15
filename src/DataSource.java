import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import oracle.jdbc.OracleDriver;

public class DataSource {
	Connection conn;
	String driverClassPath;
	String connectionString;
	AppProperties appProperties;
	AppLogger appLogger;
	AppLogger errorLogger;
	AppLogger processStatusLogger;
	int vendorId;

	/*
	 * Public: Constructor for Data Source. This function picks up the driver
	 * and connection string details from the config.properties file and tries
	 * to establish a connection to the database.
	 */
	public DataSource(AppLogger appLogger, AppLogger errorLogger, AppLogger processStatusLogger) {

		appProperties = new AppProperties();

		this.appLogger = appLogger;
		this.errorLogger = errorLogger;
		this.processStatusLogger = processStatusLogger;

		driverClassPath = appProperties.getProperty("driver_class");
		connectionString = appProperties.getProperty("connection_string");
		vendorId = Integer.parseInt(appProperties.getProperty("vendor_id"));
		try {
			appLogger.write("Connecting to the Data Source.");
			processStatusLogger.write("Connecting to the Data Source.");
			System.out.println("Connecting to the Data Source.");
			conn = getConnectionForVendor(vendorId);
			appLogger.write("Connection Established.");
			processStatusLogger.write("Connection Established.");
			System.out.println("Connection Established.");
		} catch (Exception exp) {
			errorLogger.write("Something went wrong: Failed to connect to the data source - " + exp.getMessage());
			System.out.println("Something went wrong: Failed to connect to the data source - " + exp.getMessage());
		}
	}

	/*
	 * Private: Method that fetches connection object based on the Vendor name
	 * passed as argument
	 * 
	 * Parameter Description: vendorName: String containing name of the database
	 * vendor
	 */
	private Connection getConnectionForVendor(int vendorName) throws SQLException, ClassNotFoundException {
		driverClassPath = appProperties.getProperty("driver_class");
		connectionString = appProperties.getProperty("connection_string");

		switch (vendorName) {
		case 1:
			/*
			 * Connection Code for MS Access database
			 */
			Class.forName(driverClassPath);
			System.out.println("Connecting to: " + connectionString);
			conn = DriverManager.getConnection(connectionString);
			break;
		case 2:
			/*
			 * Connection Code for Oracle Database
			 */
			int connectionType = Integer.parseInt(appProperties.getProperty("connection_type"));
			switch (connectionType) {
			case 1:
				/*
				 * TNS based connection specific code
				 */
				System.setProperty("oracle.net.tns_admin", appProperties.getProperty("tnsnames_path"));
				break;
			}
			DriverManager.registerDriver(new OracleDriver());
			conn = DriverManager.getConnection(connectionString);
			break;
		case 3:
			/*
			 * Connection Code for SQL Server Database
			 */
			Class.forName(driverClassPath);
			conn = DriverManager.getConnection(connectionString);
			break;
		}
		return conn;
	}
}
