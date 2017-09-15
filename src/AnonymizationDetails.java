import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class AnonymizationDetails
{
	String fileName = ".\\anonymization_details.properties";
	InputStream inputStream;
	Properties prop = new Properties();

	public AnonymizationDetails(){
		try{
			inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
			if (inputStream != null){
				prop.load(inputStream);
			}
			else{
				throw new FileNotFoundException("Property File named '" + fileName + "' is missing");
			}
		}
		catch(Exception exp){
			System.out.println("Something went wrong: " + exp.getMessage());
		}
	}

	public String[] getColumnNames(String key){
		return prop.getProperty(key).split(",");
	}

	/*
	 * Public: Instance method that gets all 
	 */
	@SuppressWarnings("unchecked")
	public Enumeration<String> getTables(){
		return (Enumeration<String>) prop.propertyNames();
	}
}
