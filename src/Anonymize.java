import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Anonymize {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		System.out.println("This is my first Java program");
		Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
		Connection conn = DriverManager.getConnection("jdbc:ucanaccess://C:/Users/murthyteja/Desktop/Database1.accdb");
	}

}
