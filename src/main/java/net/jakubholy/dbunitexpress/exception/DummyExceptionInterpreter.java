/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.exception;

import java.sql.SQLException;

/**
 * An implementation that does not explain anything and always returns null.
 * Used when an appropriate interpeter can't be obtained.
 *
 * @since 1.2.0
 *
 */
public class DummyExceptionInterpreter implements IExceptionInterpreter {

	public String explain(final Throwable cause) {
		return null;
	}

	public String explain(final SQLException cause) {
		return null;
	}

}
