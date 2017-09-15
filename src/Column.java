public class Column {
	String name;
	/* 
	 * Intent behind having an algorithm Name attribute is to allow the user to
	 * Specify a unique masking technique for a given column.
	 * 
	 * For Example: We may want to mask messages differently, Locations differently,
	 *              and regular User Names differently
	 */
	String algorithmName;
	// This is an optional parameter. The user may pass an optional dummy text to replace a
	// text value in the table
	String dummmyValue;
}
