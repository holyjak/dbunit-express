/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.exception.derby;

import java.sql.Connection;
import java.sql.Statement;

import net.jakubholy.dbunitexpress.AbstractEmbeddedDbTestCase;

/**
 * Simple verification of some of the possible errors.
 *
 */
public class DerbyExceptionInterpreterTest extends AbstractEmbeddedDbTestCase {

	static {
		// Decrease lock timeout from 20 to the min. of 1 second so that
		// testThatLockedTableDetected() doesn't take so long
		// Unfortunately it will apply also for any following test
		System.setProperty("derby.locks.waitTimeout", "1");
//		//System.setProperty("derby.locks.deadlockTimeout", "1");
	}

	private DerbyExceptionInterpreter interpreter;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		interpreter = new DerbyExceptionInterpreter();
	}

	public void testThatLockedTableDetected() throws Exception {

		// PREPARE
		final Connection connection = getSqlConnection();
		// Forbid autocommit so that lock will be retained until commit/rollback
		connection.setAutoCommit(false);
		try {
			final Statement stmt = connection.createStatement();
			stmt.executeUpdate("UPDATE my_test_schema.my_test_table SET some_text='abc' where id=3");

			// TEST
			// Execute query - shall time out because of a write lock on the table
			try {
				getEmbeddedDbTester().getConnection().createQueryTable(
					"lockedTable", "select * from my_test_schema.my_test_table");
				fail("Should have failed because the table is locked due to an " +
						"uncommited update transaction.");
			} catch (Exception e) {
				final String explanation = interpreter.explain(e);
				assertEquals("The table is locked, perhaps your test code has " +
						"not cleaned correctly the DB resources that it used " +
						"(such as doing proper commit/rollback if it set autocommit " +
						"off).", explanation);
			}
		} finally {
			// CLEANUP !!!
			connection.commit();
			connection.setAutoCommit(true);
			connection.close();
			System.err.println(getClass() + ".testThatLockedTableDetected: " +
					"cleanup finished; conn.closed: " + connection.isClosed());
		}

	}

}
