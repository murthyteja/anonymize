public class Table {
	String name;
	// Boolean data attribute which conveys whether or not to process a table using
	// batch update technique in JDBC
	boolean processByBatch;
	// A Table can have many columns that we may have to process for anonymization
	Column[] columns;
}
