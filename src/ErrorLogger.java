import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ErrorLogger {
	AppProperties appProperties = new AppProperties();
	Logger log;
	FileHandler fh;

	/*
	 * Error Logger Constructor
	 */
	public ErrorLogger() {
		String errorLogPath = appProperties.getProperty("error_log_path");
		log = Logger.getLogger(errorLogPath);
		// Remove logs from console
		log.setUseParentHandlers(false);

		// This block configures the logger with handler and formatter
		try {
			errorLogPath = System.getProperty("user.dir") + errorLogPath;
			// The second argument to file handler opens the file and appends to
			// the existing log
			fh = new FileHandler(errorLogPath, true);
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
		fh.setFormatter(formatter);
	}

	/*
	 * This instance method writes to the Log file
	 */
	public void write(String message) {
		log.info(message);
	}

}
