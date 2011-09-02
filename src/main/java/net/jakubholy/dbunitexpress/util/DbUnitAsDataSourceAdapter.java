/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import org.dbunit.DBTestCase;
import org.dbunit.IDatabaseTester;

/**
 * DataSource implementation that obtains connections from
 * DbUnit's {@link IDatabaseTester}. Used in DbUnit unit tests
 * to create DataSource interfacing with DbUnit and its test DB.
 * <p>
 * Mostly useful for testing classes that access DB via DataSource
 * and not directly via a Connection, for example Spring's JdbcTemplate and
 * friends.
 *
 * @since 1.1.0
 */
public class DbUnitAsDataSourceAdapter implements javax.sql.DataSource {

	private final IDatabaseTester databaseTester;	// NOPMD

	/**
	 * @param databaseTester (required) supplied by the actually running
	 * {@link DBTestCase}
	 */
	public DbUnitAsDataSourceAdapter(final IDatabaseTester databaseTester) {
		this.databaseTester = databaseTester;
	}


	public Connection getConnection() throws SQLException {
		if (databaseTester == null) {
			throw new IllegalStateException("The instance attribute databaseTester: IDatabaseTester " +
					"must be set prior to calling this. The instance is " +
					"necessary to create connections.");
		}

		try {
			return databaseTester.getConnection().getConnection();
		} catch (Exception e) {
			final String msg = "Failed to obtain a connection from " +
			"the DbUnit's DatabaseTester " + databaseTester;
			throw new SQLException(msg + ": " + e);
		}
	} /* getConnection */


	// ############################################################################# no op methods

	public Connection getConnection(final String theUsername, final String thePassword)
			throws SQLException {
		throw new UnsupportedOperationException("This implementation supports only getConnection()");
	} /* getConnection(user,psw) */

	public PrintWriter getLogWriter() throws SQLException {
		return null; // null means that loggin is disabled
	} /* getLogWriter */

	public int getLoginTimeout() throws SQLException {
		return 0; // zero is the default value
	} /* getLoginTimeout */

	public void setLogWriter(final PrintWriter out) throws SQLException {
		// no operation, not supported
	} /* setLogWriter */

	public void setLoginTimeout(final int seconds) throws SQLException {
		// no operation, not supported
	} /* setLoginTimeout */

	/***
	 * @since 1.6
	 */
	public boolean isWrapperFor(final Class iface) throws SQLException {
		return false;
	}

    	/***
         * @since 1.6
         */
        public Object unwrap(Class iface) throws SQLException {
		throw new UnsupportedOperationException("This is not a wrapper");
    	}

}
