import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AppLogger {
	AppProperties appProperties = new AppProperties();
	Logger log;
	FileHandler fh;

	/*
	 * App Logger Constructor
	 */
	public AppLogger(String log_path) {
		String appLogPath = appProperties.getProperty(log_path);
		String loggerPropertiesPath = appProperties.getProperty("log_properties_path");
		log = Logger.getLogger(appLogPath);
		Properties preferences = new Properties();
//		try{
//			FileInputStream configFile = new FileInputStream(loggerPropertiesPath);
//			preferences.load(configFile);
//		    LogManager.getLogManager().readConfiguration(configFile);
//		}
//		catch(Exception exp){
//			System.out.println(exp.getMessage());
//		}
		// Remove logs from console
		log.setUseParentHandlers(false);

		// This block configures the logger with handler and formatter
		try {
			// The second argument to file handler opens the file and appends to
			// the existing log
			fh = new FileHandler(appLogPath, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
		
		fh.setFormatter(formatter);
	}

	/* Public: This instance method writes to the Log file
	 * 
	 * Parameter Description:
	 * 			message: Message to be written to the error log
	 */
	public void write(String message) {
		log.info("\n\n" + message + "\n\n");
	}
	
	public void closeFileHandler(){
		this.fh.close();
		System.out.println("File handler closed");
	}
}
