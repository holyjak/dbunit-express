/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.exception;

import java.sql.SQLException;

/**
 * Tries to explain an exception thrown by the underlying database
 * based on knowledge of the database and it's error codes.
 * Thus there are different implementations for different databases.
 *
 * @since 1.2.0
 *
 * @see SQLException#getSQLState()
 */
public interface IExceptionInterpreter {

	/**
	 * Descend recursively the cause chain to find
	 * the first SQLException and explain it as if calling
	 * {@link #explain(SQLException)} for it.
	 * @param cause (required) an exception that is likely to be
	 * caused by a SQLException somewhere down the cause chain
	 * @return An explanation of the first SQLException or null if either
	 * there is no SQLException or no explanation is known for it
	 *
	 * @see Throwable#getCause()
	 */
	String explain(final Throwable cause);

	/**
	 * Return an explanation of what this error means and what the user
	 * should do about it. The explanation can be used in a log or as
	 * a message of a new, higher-level exception wrapping the original
	 * one.
	 * @param cause (required) the exception to explain
	 * @return An explanation or null if nothing special is known about
	 * this error
	 */
	String explain(final SQLException cause);

}
