/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.test;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.datatype.DataType;

/**
 * Various utility methods for tables etc used in tests.
 */
public final class DatabaseUtils {

	private DatabaseUtils(){}

	/**
	 * Creates a table called "testTable" with 3 columns (String, int, Date).
	 */
	public static DefaultTable createTestTable() {
		final Column[] columns = new Column[]{
				new Column("column1", DataType.VARCHAR)
				, new Column("column2", DataType.INTEGER)
				, new Column("column3", DataType.DATE)
		};
		return new DefaultTable("testTable", columns);
	}

}
