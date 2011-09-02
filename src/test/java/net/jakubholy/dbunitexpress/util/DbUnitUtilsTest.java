/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.util;

import java.util.Set;

import junit.framework.TestCase;
import net.jakubholy.dbunitexpress.IEnhancedDatabaseTester;
import net.jakubholy.dbunitexpress.EmbeddedDbTester;

import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.ITable;

/**
 * @author jakub.holy@ibacz.eu
 *
 */
public class DbUnitUtilsTest extends TestCase {

	private static final String TEST_TABLE = "my_test_schema.my_test_table";

//	private static final Logger LOG = LoggerFactory.getLogger(DbUnitUtilsTest.class);

	private final transient EmbeddedDbTester dbTester = new EmbeddedDbTester();
	private transient IEnhancedDatabaseTester enhancedTester;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		dbTester.setDataSet( dbTester.createDataSetFromFile("data-DbUnitUtils-initial.xml") );
		dbTester.onSetup();
		enhancedTester = dbTester.getEnhancedTester();
	}

	/**
	 * Verify that primary keys are found and possible duplicate rows detected.
	 * @throws Exception
	 */
	public void testFindPkDuplicates_1column() throws Exception {

		final ITable dbTable = enhancedTester.getDataSet().getTable(TEST_TABLE);
		final DefaultTable newTable = new DefaultTable(dbTable.getTableMetaData());
		newTable.addRow(new Object[]{new Integer(100), "row #100"});
		newTable.addRow(new Object[]{new Integer(222), "row #222"});
		newTable.addRow(new Object[]{new Integer(100), "row #100, a duplicate"});

		final Set duplicates = DbUnitUtils.findPkDuplicates(newTable, dbTester.getConnection());

		assertNotNull("May return en empty set but not null", duplicates);
		assertEquals("# of duplicates in " + duplicates
				, 1, duplicates.size());
		final String duplicate = "100";
		assertTrue("Expected the duplicate '" + duplicate + "', actuals: " + duplicates
				, duplicates.contains(duplicate));
	}

}
