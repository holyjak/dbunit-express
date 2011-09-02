/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.exception.derby;

import java.sql.SQLException;

import net.jakubholy.dbunitexpress.DatabaseCreator;
import net.jakubholy.dbunitexpress.exception.IExceptionInterpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for the derby database. (Currently using 10.3.2.1.)
 * See http://publib.boulder.ibm.com/infocenter/cscv/v10r1/index.jsp?topic=/com.ibm.cloudscape.doc/rrefexcept71493.html
 *
 * @since 1.2.0
 *
 */
public class DerbyExceptionInterpreter implements IExceptionInterpreter {

	private static final Logger LOG = LoggerFactory.getLogger(DerbyExceptionInterpreter.class);

	/* (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.exception.IExceptionInterpreter#explain(java.lang.Exception)
	 */
	public String explain(Throwable cause) {

		while (cause != null) {
			if (cause instanceof SQLException) {
				return explain((SQLException) cause);
			} else {
				cause = cause.getCause();
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.exception.IExceptionInterpreter#explain(java.sql.SQLException)
	 */
	public String explain(final SQLException cause) {
		if (cause == null) {
			throw new IllegalArgumentException("The argument SQLException cause may not be null.");
		}

		final String sqlState = cause.getSQLState();
		String explanation = null;

		if ("40XL1".equals(sqlState) || "40XL2".equals(sqlState)) {
			// 40XL1: A lock could not be obtained within the time requested
			// 40XL2: A lock could not be obtained within the time requested. The lockTable dump is: <tableDump>.
			explanation =
				"The table is locked, perhaps your test code has " +
				"not cleaned correctly the DB resources that it used " +
				"(such as doing proper commit/rollback if it set autocommit " +
				"off).";
		} else if ("XSDB6".equals(sqlState)) {
			explanation = "Failed to connect to the " +
				"test database, it seems that it is locked by " +
				"another process. Make sure that no other process " +
				"accesses it and remove the .lck files from the " +
				"database folder.";
		} else if ("XJ004".equals(sqlState)) {
			explanation = "Failed to connect to the " +
				"test database, it seems it hasn't been " +
				"created yet - check the detailed failure stack trace. " +
				"You can create the DB by executing " +
				DatabaseCreator.class + ".main(..).";
		} else if ("XJ040".equals(sqlState) || "XSDB6".equals(sqlState)) {
			// XJ040: Failed to start database '<databaseName>', see the next exception for details.
			// XSDB6: Another instance of Derby may have already booted the database <DB path>.
			explanation = "Failed to start the database, see the next exception for details. " +
					"Possible cause - Derby may wrongly believe that its already " +
					"in use by another process if it wasn't shut down " +
					"correctly the last time. In such a case " +
					"delete the lock files (*.lck) from the database " +
					"directory and rerun the test.";
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("explain(sqlState=" + sqlState +
					") - explanation: " + explanation);
		}

		return explanation;
	}

}
