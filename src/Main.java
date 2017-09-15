public class Main {
	
	/*
	 * Private: Static method that prints out summary information on the console
	 * 
	 * Returns nothing
	 */
	private static void printSummary(long start, long end){
		System.out.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		System.out.println("Anonymization Process has been completed");
		System.out.println("Total Time Taken: " + (end - start)/1000f + " Seconds");
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	}

	/*
	 * Public: Main function for the Anonymization application
	 * 
	 * Returns nothing
	 */
	public static void main(String[] args){

		// Initialize the loggers
		AppLogger appLogger = new AppLogger("app_log_path");
		AppLogger errorLogger = new AppLogger("error_log_path");
		AppLogger statusLogger = new AppLogger("status_log_path");
		
		// Initialize the Data Source Object
		DataSource ds = new DataSource(appLogger, errorLogger, statusLogger);

		System.out.println("Starting Anonymization Process");
		long start = System.currentTimeMillis();
		// Call the Big Daddy. Let him do all the heavy lifting
		Anonymize anonymizeObject = new Anonymize(appLogger, errorLogger, statusLogger);
		//Anonymize.anonymizeDatabase(ds.conn);
		anonymizeObject.anonymizeDatabase(ds.conn);
		long end = System.currentTimeMillis();
		printSummary(start, end);

		appLogger.closeFileHandler();
		errorLogger.closeFileHandler();
		statusLogger.closeFileHandler();
		
	}
}
