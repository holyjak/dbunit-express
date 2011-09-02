/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress;

import java.io.FileNotFoundException;

import junit.framework.TestCase;
import net.jakubholy.dbunitexpress.assertion.RowComparator;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.DataType;

/**
 * An example of a a simple unit test verifying data in the test database,
 * using directly the {@link EmbeddedDbTester} instead of extending the
 * {@link net.jakubholy.dbunitexpress.AbstractEmbeddedDbTestCase}.
 * <p>
 * Otherwise it doesn't differ from {@link SimpleEmbeddedDbTest}, please check
 * its JavaDoc for further instructions.
 */
public class SimpleNonExtendingEmbeddedDbJUnit3Test extends TestCase {

	private EmbeddedDbTester testDb = new EmbeddedDbTester();

	protected void setUp() throws Exception {
		testDb.onSetup();
	}

	/**
	 * Test that DB is correctly loaded with the expected data prior to running the test.
	 * <p>
	 * <h4>Prerequisites</h4>
	 * <ol>
	 * 	<li>The database has been created
	 *  <li>and the schemas and tables have been defined within the DB
	 * </ol>
	 */
	public void testIt_manual() throws Exception {

		// setUp has filled the DB with data, we will just verify they're as expected.
		final String sql = "select * from my_test_schema.my_test_table order by id asc";
		ITable allRowsTbl = testDb.getConnection().createQueryTable("allRows", sql);

		assertEquals("There shall be 3 rows in total.", 3, allRowsTbl.getRowCount());

		// Row #1
		// 1. Use same column name case as in the data set
		assertRowEquals(allRowsTbl, 0
				, new String[]{ "id", "some_text"}
				, new Object[]{ new Integer(1), "some text #1, xml must be escaped like in & , >"}
		);

		// Row #2
		// 1. Different column name case to verify dbunit is case-insensitive
		assertRowEquals(allRowsTbl, 1
				, new String[]{ "iD", "sOmE_teXt"}
				, new Object[]{ new Integer(2), "some xml entities may be here like in &, <>"}
		);

		// Row #3
		// 1. Use same column name case as in the data set
		assertRowEquals(allRowsTbl, 2
				, new String[]{ "id", "some_text"}
				, new Object[]{ new Integer(3), null}
		);

	}

	/**
	 * Test that DB is correctly loaded with the expected data prior to running the test.
	 * <p>
	 * <h4>Prerequisites</h4>
	 * <ol>
	 * 	<li>The database has been created
	 *  <li>and the schemas and tables have been defined within the DB
	 * </ol>
	 */
	public void testIt_withRowComparator() throws Exception {

		// setUp has filled the DB with data, we will just verify they're as expected.
		final String sql = "select * from my_test_schema.my_test_table order by id asc";
		final RowComparator rowComparator = testDb.createCheckerForSelect(sql);

		rowComparator.assertRowCount(3);

		rowComparator.assertNext(
			new Object[]{ new Integer(1), "some text #1, xml must be escaped like in & , >"});
		rowComparator.assertNext(
			new Object[]{ new Integer(2), "some xml entities may be here like in &, <>"});
		// We can also make the call simple if String comparison is enough
		rowComparator.assertNext("3", (String) null);

	} /* testIt */

	/**
	 * Replace the test data inserted by DbUnit with data from
	 * another data set XML file.
	 * This is useful if you need different data for different test methods.
	 * @throws Exception
	 * @throws FileNotFoundException
	 */
	public void testWithReplacementDataFromFile() throws FileNotFoundException, Exception {

		try {
			testDb.getEnhancedTester().replaceDatabase(
					"/path/to/the/replacementDataset.xml");
		} catch (FileNotFoundException e) {
			System.out.println("The test testWithReplacementDataFromFile " +
					"isn't yet fully implemented and is normal to fail with " + e);
		}

		// TODO perform your tests now ...

	} /* testWithReplacementDataFromFile */

	/**
	 * Replace the test data inserted by DbUnit with data from
	 * programmatically defined DataSet.
	 * This is useful if you need different data for different test methods.
	 * @throws DataSetExceptionthrown by {@link DefaultTable#addRow()}
	 */
	public void testWithReplacementDataFromDataSet() throws DataSetException, Exception {

		// Set up
		final DefaultTable table = new DefaultTable(
				"my_test_schema.my_test_table", new Column[] {
						new Column("id", DataType.INTEGER),
						new Column("some_text", DataType.VARCHAR) });
		table.addRow(new Object[] { new Integer(1), "replaced hello!" });
		final DefaultDataSet dataSet = new DefaultDataSet(table);
		testDb.getEnhancedTester().replaceDatabase(dataSet);

		// Verify
		final String sql = "select * from my_test_schema.my_test_table order by id asc";
		ITable allRowsTbl = testDb.getConnection().createQueryTable("allRows", sql);

		assertEquals("There shall be 1 row in total.", 1, allRowsTbl.getRowCount());

		// Row #1
		// 1. Use same column name case as in the data set
		assertRowEquals(allRowsTbl, 0
				, new String[]{ "id", "some_text"}
				, new Object[]{ new Integer(1), "replaced hello!"}
		);

	} /* testWithReplacementDataFromDataSet */

	/**
	 * Remove all data from a particular table before testing.
	 * @throws DataSetException
	 * @throws Exception
	 */
	public void testClearTable() throws DataSetException, Exception {
		// Set up
		testDb.getEnhancedTester().clearTable("my_test_schema.my_test_table");

		// Verify
		final String sql = "select * from my_test_schema.my_test_table order by id asc";
		ITable allRowsTbl = testDb.getConnection().createQueryTable("allRows", sql);
		assertEquals("There shall be no rows since the table shall have been cleared."
				, 0, allRowsTbl.getRowCount());
	} /* testClearTable */

	private void assertRowEquals(ITable resultTable, final int resultRowNr, String[] columnNames, Object[] columnValues)
			throws DataSetException {

		if (resultTable == null) {
			throw new IllegalArgumentException("ITable resultTable may not be null");
		}
		if (resultRowNr < 0) {
			throw new IllegalArgumentException("resultRowNr must be >= 0, is " + resultRowNr);
		} else if (resultRowNr >= resultTable.getRowCount()) {
			throw new IllegalArgumentException("resultRowNr must be < resultTable.rowCount of " +
					resultTable.getRowCount() + ", is " + resultRowNr +
					"; perhaps you forgot that are numbered from 0?");
		}
		if (columnNames == null) {
			throw new IllegalArgumentException("String[] columnNames may not be null");
		}
		if (columnValues == null) {
			throw new IllegalArgumentException("Object[] columnValues may not be null");
		}
		if (columnNames.length != columnValues.length) {
			throw new IllegalArgumentException("columnNames.length shall be same as columnValues.length");
		}

		// Do the test
		for (int i = 0; i < columnNames.length; i++) {
			Object actual = resultTable.getValue(resultRowNr, columnNames[i]);
			assertEquals("row (starting from 0) " + resultRowNr + ", column '" + columnNames[i] + "'"
					,columnValues[i], actual);
		}

	} /* assertRowEquals */

}
