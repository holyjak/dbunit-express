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

import org.dbunit.DBTestCase;
import org.dbunit.DatabaseTestCase;
import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;

/**
 * Parent class for DbUnit test cases that want to use an
 * embedded (Derby) database for testing instead of an external
 * standalone DB in few easy steps. It replaces the {@link DBTestCase}.
 * Such tests are useful for testing classes that interact with a database.
 * <p>
 * This class itself doesn't do anything more than delegating to an
 * {@link EmbeddedDbTester} and it only exists for convenience and backward
 * compatibility.
 * <strong>See JavaDoc of that class to learn how to use it and how to
 * set up your database unit tests.</strong>
 *
 * See <a href="http://sourceforge.net/apps/mediawiki/jeeutils/index.php?title=DbUnit_Test_Skeleton">DbUnit_Test_Skeleton subproject of jeeutils at SourceForge</a>
 *
 * @see EmbeddedDbTester
 * @see DatabaseCreator#main(String[])
 *
 * @author jholy
 */
public abstract class AbstractEmbeddedDbTestCase extends DBTestCase {

	/**
	 * File's Subversion info (version etc.).
	 * It's replacement upon commit must be enabled in svn properties.
	 */
	public static final String SVN_ID = "$Id: AbstractEmbeddedDbTestCase.java 89 2010-04-15 16:38:26Z malyvelky $";

	//##########################################################################

    /**
     * Override if you want to load data from another file or files
     * or if you want to use different format of the data set (e.g. FlatXmlDataSet).
     * <p>
     * Warning: Once you override this, you will not be able to access the
     * original data set, both super.getDataSet() and
     * getEmbeddedDbTester().getDataSet() will return the data set you've created.
     * You can access it as follows
     * <pre><code>
     * getEmbeddedDbTester().createDataSetFromFile(EmbeddedDbTester.DBUNIT_TEST_DATA_SET_NAME);
     * </code></pre>
     * <p>
     * You may use {@link EmbeddedDbTester#createDataSetFromFile(String)} to create
     * a data set from a file stored in the default location like this:
     * <code>
     * return getEmbeddedDbTester().createDataSetFromFile("your_data_set.xml");
     * </code>
     *
     * @see org.dbunit.DatabaseTestCase#getDataSet()
     */
    protected IDataSet getDataSet() throws Exception {	// NOPMD
    	return getEmbeddedDbTester().getDataSet();
    }

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
	 * @throws DataSetException
	 * @throws DatabaseUnitRuntimeException
	 *
	 * @see IEnhancedDatabaseTester#replaceDatabase(String)
     */
    protected final void replaceDatabase(final String dataSetFile) throws FileNotFoundException, DatabaseUnitRuntimeException, DataSetException {
    	getEmbeddedDbTester().getEnhancedTester().replaceDatabase(dataSetFile);
    }

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
     * @throws DatabaseUnitRuntimeException thrown by {@link IDatabaseTester#onSetup()}
     *
	 * @see IEnhancedDatabaseTester#replaceDatabase(IDataSet)
     */
    protected final void replaceDatabase(final IDataSet newDataSet) throws DatabaseUnitRuntimeException {
    	getEmbeddedDbTester().getEnhancedTester().replaceDatabase(newDataSet);
    }

    /**
     * Removes all data from the given test table.
     * This is useful e.g. if you want to verify that your code behaves correctly
     * when there are no data.
	 * @param tableName Table name in the form used by the underlying tester,
	 * 	i.e. either fully qualified (schema.table) or unqualified (only table) -
	 * 	likely same as in your data set XML file
     * @throws SQLException
     *
     * @see IEnhancedDatabaseTester#clearTable(String)
     */
    protected final void clearTable(final String tableName) throws SQLException {
    	getEmbeddedDbTester().getEnhancedTester().clearTable(tableName);
    }

	/**
	 * Returns the database tester used internally for all operations by this class
	 * @throws Exception
	 */
    public final EmbeddedDbTester getEmbeddedDbTester() throws DatabaseUnitRuntimeException {
		try {
			return (EmbeddedDbTester) getDatabaseTester();
		} catch (Exception e) {
			throw new DatabaseUnitRuntimeException(
					"Failed to create the  inner tester", e);
		}
	}

	/**
	 * Used by the parent class.
	 * @see org.dbunit.DBTestCase#newDatabaseTester()
	 */
	protected final IDatabaseTester newDatabaseTester() throws Exception {	// NOPMD
		return new EmbeddedDbTester();
	}

	/**
	 * Returns a data source for accessing the underlying test database.
	 * A shortcut for {@link IEnhancedDatabaseTester#getDataSource()}.
	 * @throws DatabaseUnitRuntimeException
	 */
	public final DataSource getDataSource() throws DatabaseUnitRuntimeException {
		return getEmbeddedDbTester().getDataSource();
	}

	/**
	 * Creates a connection to the underlying test database.
	 * @throws SQLException
	 * @throws DatabaseUnitRuntimeException
	 *
	 * @see #getConnection()
	 * @see IDatabaseConnection#getConnection()
	 */
	public Connection getSqlConnection() throws DatabaseUnitRuntimeException, SQLException {
		return getEmbeddedDbTester().getSqlConnection();
	}

}