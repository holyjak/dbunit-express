/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.exception;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import net.jakubholy.dbunitexpress.exception.derby.DerbyExceptionInterpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide an instance of {@link IExceptionInterpreter} appropriate for
 * the underlying database.
 *
 * @since 1.2.0
 */
public final class ExceptionInterpreterFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionInterpreterFactory.class);

	private static IExceptionInterpreter dummyInterpreter = new DummyExceptionInterpreter();

	private ExceptionInterpreterFactory(){}

	/**
	 * Returns a non-null dummy interpreter - to be used before the appropriate
	 * one can be determined to avoid null pointer exception.
	 * Since the interpreter doesn't actually explain anything you should
	 * replace it with an appropriate one as soon as possible.
	 */
	public static IExceptionInterpreter getDefaultInterpreter() {
		return dummyInterpreter;
	}

	/**
	 * Returns an exception interpreter appropriate for the underlying database
	 * as determined by the connection.
	 * @param connection (optional) a connection to the underlying DB, necessary
	 * to determine the appropriate interpreter
	 * @return An interpreter appropriate for the DB or a dummy one.
	 */
	public static IExceptionInterpreter getInterpreter(final Class driverClass) {
		if (!Driver.class.isAssignableFrom(driverClass)) {
			throw new IllegalArgumentException("The argument " + driverClass +
					" is not a JDBC Driver class (doesn't implement " +
					Driver.class + ")");
		}

		if (driverClass.getName().startsWith("org.apache.derby.")) {
			return new DerbyExceptionInterpreter();
		} else {
			LOG.info("getInterpreter({}): unknown driver, returning the " +
					"dummy interpreter"
					, driverClass);
			return dummyInterpreter;
		}
	}

	/**
	 * Returns an exception interpreter appropriate for the underlying database
	 * as determined by the connection.
	 * @param connection (optional) a connection to the underlying DB, necessary
	 * to determine the appropriate interpreter
	 * @return An interpreter appropriate for the DB or a dummy one.
	 */
	public static IExceptionInterpreter getInterpreter(final Connection connection) {
		IExceptionInterpreter interpreter;

		if (connection == null) {
			LOG.info("getInterpreter: The argument Connection is null thus the " +
					"appropriate interpreter can't be determined, returning " +
					"the dummy one instead.");
			interpreter = null;
		} else {
			try {
				final String dbName = connection.getMetaData().getDatabaseProductName();
				interpreter = getForDatabase(dbName);
			} catch (SQLException e) {
				LOG.error("getInterpreter: Failed to access the connection's " +
						"metadata, therefore an appropriate interpreter can't " +
						"be determined, returning the dummy one instead.", e);
				interpreter = null;
			}
		}

		return (interpreter == null)? dummyInterpreter : interpreter;
	}

	private static IExceptionInterpreter getForDatabase(final String dbName) {
		if (dbName.toLowerCase().indexOf("derby") >= 0) {
			return new DerbyExceptionInterpreter();
		} else {
			LOG.debug("getForDatabase({}): No interpreter for this DB exists."
					, dbName);
			return null;
		}
	}

}
