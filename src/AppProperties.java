import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {
	String fileName = ".\\config.properties";
	InputStream inputStream;
	Properties prop = new Properties();

	public AppProperties(){
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

	public String getProperty(String key){
		return prop.getProperty(key);
	}
}
