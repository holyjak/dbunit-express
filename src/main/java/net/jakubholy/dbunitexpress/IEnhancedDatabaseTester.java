/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.dbunit.DatabaseTestCase;
import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.XmlDataSet;

/**
 * The actual manager of the test database with useful methods for accessing it
 * and modifying its content.
 *
 * @see #getDataSource()
 * @see #getSqlConnection()
 * @see #clearTable(String)
 * @see #replaceDatabase(String)
 *
 * @author jholy
 * @since 1.1.0
 *
 */
public interface IEnhancedDatabaseTester extends IDatabaseTester {

	/**
	 * Replace the content of the database from the provided data set XML file.
	 * This is useful e.g. if, for a particular test method, you need some special
	 * data in the database and you don't want to create a new test class for that.
	 * This method does the same database re-initialization that happens in {@link DatabaseTestCase#setUp}.
	 * <p>
	 * Consider this to be an experimental method that may have yet undiscovered
	 * side effects on DbUnit's state and behavior and use at your own risk.
	 *
	 * <h4>Usage example</h4>
	 * <pre><code>
	 * public void testFetchData_emtpyTable() throws Exception {
	 * 		this.replaceDatabase("TestData/empty_data_set.xml");
	 * 		// Call the tested method
	 * 		final List data = this.target.fetchData();
	 * 		// Verify results
	 * 		assertEquals("empty data set =&gt; shall be empty", 0, data.size());
	 * }
	 *
	 * // other test method may use the default data set w/o calling replaceDatabase
	 * </code></pre>
	 *
	 * @param dataSetFile (required) File name of a DbUnit Data Set XML
	 * @throws FileNotFoundException If the provided string isn't an existing file's name
	 * @throws DatabaseUnitRuntimeException thrown as Exception by {@link IDatabaseTester#onSetup()}
	 * @throws DataSetException thrown by {@link XmlDataSet#XmlDataSet(java.io.InputStream)}
	 */
	void replaceDatabase(final String dataSetFile)
			throws FileNotFoundException, DatabaseUnitRuntimeException, DataSetException;

	/**
	 * See {@link #replaceDatabase(String)} for detailed description, here
	 * we only describe the differences.
	 * <p>
	 * This variant replaces the data by the provided Data Set - which
	 * may be constructed programmatically - instead of
	 * loading them from a .xml file.
	 *
	 * <h4>Example</h4>
	 * <pre><code>
	 * import org.dbunit.dataset.*;
	 * import org.dbunit.dataset.datatype.DataType;
	 *
	 * final DefaultTable table = new DefaultTable("my_test_schema.my_test_table", new Column[]{
	 *			new Column("id", DataType.INTEGER)
	 *			, new Column("some_text", DataType.VARCHAR) });
	 * table.addRow( new Object[] {new Integer(1), "hello!"} );
	 *
	 * final DefaultDataSet dataSet = new DefaultDataSet(table);
	 * this.replaceDatabase( dataSet );
	 * </code></pre>
	 *
	 * @param newDataSet (required) the data set used to replace the data with
	 * @throws DatabaseUnitRuntimeException thrown as Exception by {@link IDatabaseTester#onSetup()}
	 */
	void replaceDatabase(final IDataSet newDataSet) throws DatabaseUnitRuntimeException;

	/**
	 * Remove all data from the given test table.
	 * This is useful e.g. if you want to verify that your code behaves correctly
	 * when there are no data.
	 * @param tableName Table name in the form used by the underlying tester,
	 * 	i.e. either fully qualified (schema.table) or unqualified (only table) -
	 * 	likely same as in your data set XML file
	 * @throws SQLException
	 */
	void clearTable(final String tableName) throws SQLException;

	/**
	 * Returns a data source connected to the test database configured for
	 * this DbUnit test. This is an alternative of {@link #getConnection()}
	 * for providing access to the database to the classes under test
	 * that need a DataSource instead of a Connection
	 * (which is often the case when using Spring).
	 * <p>
	 * The data source uses this database tester underneath.
	 *
	 * @throws DatabaseUnitRuntimeException
	 */
	DataSource getDataSource() throws DatabaseUnitRuntimeException;

	/**
	 * Creates a connection to the underlying test database.
	 * @throws SQLException
	 * @throws DatabaseUnitRuntimeException
	 *
	 * @see #getConnection()
	 * @see IDatabaseConnection#getConnection()
	 */
	Connection getSqlConnection() throws SQLException, DatabaseUnitRuntimeException;

}
