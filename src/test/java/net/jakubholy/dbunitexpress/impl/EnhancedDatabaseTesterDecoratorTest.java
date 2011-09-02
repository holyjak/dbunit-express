/* ===========================================================================
 * IBA CZ Confidential
 *
 * © Copyright IBA CZ 2009 ALL RIGHTS RESERVED
 * The source code for this program is not published or otherwise
 * divested of its trade secrets.
 *
 * =========================================================================== */

package net.jakubholy.dbunitexpress.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.jakubholy.dbunitexpress.EmbeddedDbTester;
import net.jakubholy.dbunitexpress.IEnhancedDatabaseTester;
import net.jakubholy.dbunitexpress.assertion.RowComparator;

import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.dataset.DataSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jakub.holy@ibacz.eu
 *
 */
public class EnhancedDatabaseTesterDecoratorTest extends TestCase {

	private static final String TEST_TABLE = "my_test_schema.my_test_table";

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedDatabaseTesterDecoratorTest.class);

	private transient EmbeddedDbTester dbTester = new EmbeddedDbTester();
	private transient IEnhancedDatabaseTester enhancedTester;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		dbTester.setDataSet( dbTester.createDataSetFromFile("data-enhancedTester-initial.xml") );
		dbTester.onSetup();
		enhancedTester = dbTester.getEnhancedTester();
	}

	/**
	 * Test method for {@link net.jakubholy.dbunitexpress.impl.EnhancedDatabaseTesterDecorator#replaceDatabase(org.dbunit.dataset.IDataSet)}.
	 */
	public void testReplaceDatabaseIDataSet() throws Exception {
		verifyInitialData();

		// Verify that duplicates are not allowed
		final Connection conn = enhancedTester.getSqlConnection();
		try {
			conn.createStatement().executeUpdate(
					"insert into my_test_schema.my_test_table (id) "
						+ " values(1)");
			fail("The insert should have failed because there already is a " +
					"row with id=1; perhaps you have forgot to define a " +
					"primary key constrain for that column?");

		} catch (SQLException e) {
			if (e.getErrorCode() != 30000) {
				LOG.error("Failed for oter reason than expected", e);
				fail("The insert of a row with an existing ID failed as " +
						"expected but for other reason than the duplicate key " +
						"value, which would be error # 30000 " +
						"(for Derby DB 10.3.2.1): is # " + e.getErrorCode() +
						", exception " + e.getMessage());
			}
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				LOG.warn("Failed to close the connection", e);
			}
		}

		// TEST
		enhancedTester.replaceDatabase(
				dbTester.createDataSetFromFile("data-enhancedTester-replacement.xml") );

		// VERIFICATION
		createTestTableChecker().assertRowCount(2)
			.assertNext("1", "replacement data - row 1 (same id as an originally loaded row)")
			.assertNext("123", "replacement data - row 123");
	}

	/**
	 * Test method for {@link net.jakubholy.dbunitexpress.impl.EnhancedDatabaseTesterDecorator#clearTable(java.lang.String)}.
	 */
	public void testClearTable() throws Exception {
		verifyInitialData();

		enhancedTester.clearTable(TEST_TABLE);

		createTestTableChecker().assertRowCount(0);
	}

	/**
	 * Test method for {@link net.jakubholy.dbunitexpress.impl.EnhancedDatabaseTesterDecorator#getDataSource()}.
	 */
	public void testGetDataSource() throws Exception {
		verifyInitialData();
		final DataSource dataSource = enhancedTester.getDataSource();
		assertNotNull("Shall return a DataSource", dataSource);
		final Connection conn = dataSource.getConnection();
		assertConnectionWorks(conn);
	}

	/**
	 * Test method for {@link net.jakubholy.dbunitexpress.impl.EnhancedDatabaseTesterDecorator#getSqlConnection()}.
	 */
	public void testGetSqlConnection() throws Exception {
		assertConnectionWorks(enhancedTester.getSqlConnection());
	}

	/**
	 * Verify that the expected data are present in the test DB as defined in
	 *  data-enhancedTester-initial.xml.
	 * @throws DatabaseUnitRuntimeException
	 * @throws SQLException
	 * @throws AssertionFailedError
	 * @throws DataSetException
	 */
	private void verifyInitialData() throws DatabaseUnitRuntimeException,
			SQLException, AssertionFailedError, DataSetException {
		createTestTableChecker().assertRowCount(3)
			.assertNext("1", "original row 1")
			.assertNext("2", "original row 2")
			.assertNext("3", (String) null);
	}

	/**
	 * Create a checker for the content of the test table, entries
	 * ordered by id.
	 * @throws DatabaseUnitRuntimeException
	 * @throws SQLException
	 */
	private RowComparator createTestTableChecker()
			throws DatabaseUnitRuntimeException, SQLException {
		final RowComparator checker = dbTester.createCheckerForSelect(
				"select id, some_text from " + TEST_TABLE + " order by id");
		return checker;
	}

	/**
	 * Verify that the provided connection can read the test DB.
	 * @param conn (required)
	 * @throws SQLException
	 */
	private void assertConnectionWorks(final Connection conn)
			throws SQLException {
		assertNotNull("The retrieved Connection shouldn't be null", conn);
		final ResultSet results = conn.createStatement().executeQuery(
				"select count(*) numRows from " + TEST_TABLE);
		assertTrue( "The query shall return 1 row", results.next() );
		assertEquals( "There shall be 3 rows in the test table according to " +
				"data-enhancedTester-initial.xml (or you might need to update the test)"
				, 3, results.getInt("numRows") );
		// Shall we close all the stuff?
		conn.close();
	}

}
