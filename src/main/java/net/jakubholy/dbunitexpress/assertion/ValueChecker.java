/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.assertion;

import junit.framework.AssertionFailedError;

/**
 * Used for complex comparisons of an expected and an actual database value.
 * If passed to the RowComparator#assertNext(String, Object[]) or
 * RowComparator#assertNext(Object[]), it's assertAcceptable will be used to
 * check the actual value.
 * <p>
 * This is useful if you cannot check an exact value but for example a range
 * (such as a date is more than X but less than Y).
 *
 * <h4>Example</h4>
 * <pre><code>
 * // Comparing row [name, age]:
 * aRowComparator.assertNext(new Object[]{"John X"
 * 	, new ValueChecker() {
 * 		public void assertAcceptable(final Object actual) throws AssertionFailedError {
 * 			if (((Integer)actual).intValue() &lt; 30)
 * 				throw new AssertionFailedError("The guy shall be older than 30!");
 * 		}
 * 	}
 * })
 * </code></pre>
 *
 * @since 1.2.0
 *
 */
public interface ValueChecker {

	/**
	 * Throws an AssertionFailedError if the actual value is not as expected.
	 * It should contain a descriptive message of the problem.
	 * @param actual (optional) the actual value of the tested column
	 * @throws AssertionFailedError
	 */
	void assertAcceptable(final Object actual) throws AssertionFailedError;

}
