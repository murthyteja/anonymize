import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Anonymize {

	public static void main(String[] args) throws ClassNotFoundException, SQLException, UnsupportedEncodingException {
		// TODO Auto-generated method stub
		System.out.println("This is my first Java program");
		Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
		Connection conn = DriverManager.getConnection("jdbc:ucanaccess://C:/Users/murthyteja/Desktop/Database1.accdb");
		DatabaseMetaData md = conn.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		System.out.println("Tables to be anonymized are:");
		while (rs.next()){
			System.out.println(rs.getString(3));
		}
		String str = Encrypt.encodeString("Sample dude");
		System.out.println(str);
		System.out.println(Encrypt.decodeString(str));
	}

}
