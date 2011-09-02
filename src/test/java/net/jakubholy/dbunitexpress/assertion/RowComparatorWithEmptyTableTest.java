/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.assertion;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.jakubholy.dbunitexpress.test.DatabaseUtils;

import org.dbunit.dataset.DefaultTable;

/**
 * A suite of tests of the RowComparator that operates on an empty test table.
 *
 */
public class RowComparatorWithEmptyTableTest extends TestCase {

	/** Instantiated in setUp with the testTable */
	private RowComparator comparator;

	private DefaultTable testTable;

	protected void setUp() throws Exception {
		super.setUp();
		testTable = DatabaseUtils.createTestTable();
		comparator = new RowComparator(testTable);
	}

	public void testAssertRowCount_empty() throws Exception {
		comparator.assertRowCount(0);
		try {
			comparator.assertRowCount(1);
			fail("Should fail beause row count is 0");
		} catch (AssertionFailedError e) {}
	}

	/**
	 * {@link RowComparator#withErrorMessage(String)} should set the message
	 * so that it is added to any following failed assert until reset.
	 */
	public void testWithErrorMessageIsPersistent() {
		final String customErrorMessage = "customErrorMsg";
		comparator
			.withErrorMessage(customErrorMessage)
			.assertRowCount(0); // should succeed

		try {
			comparator.assertRowCount(Integer.MIN_VALUE); // fails
			fail("The assert should have failed");
		} catch (AssertionFailedError e) {
			assertEquals("The exception's message '" + e.getMessage() +
					"' shall contain the custom error '" + customErrorMessage +
					"' set previously"
					, 0, e.getMessage().indexOf(customErrorMessage));
		}
	}

	/**
	 * {@link RowComparator#withOneTimeErrorMessage(String)} should set the
	 * message so that it is removed after the following assert* no matter
	 * whether it failed or succeeded.
	 */
	public void testWithErrorMessageCanBeReset() {
		final String customErrorMessage = "customErrorMsg";
		comparator
			.withOneTimeErrorMessage(customErrorMessage)
			.assertRowCount(0); // should succeed

		try {
			comparator
				.withErrorMessage(null)
				.assertRowCount(Integer.MIN_VALUE); // fails
			fail("The assert should have failed");
		} catch (AssertionFailedError e) {
			assertEquals("The exception's message '" + e.getMessage() +
					"' shall NOT contain the custom error '" + customErrorMessage +
					"' because we asked for it to be reset"
					, -1, e.getMessage().indexOf(customErrorMessage));
		}
	}

	/**
	 * {@link RowComparator#withOneTimeErrorMessage(String)} should set the
	 * message so that it is removed after the following assert* no matter
	 * whether it failed or succeeded.
	 */
	public void testWithOneTimeErrorMessageWorks() {
		final String customErrorMessage = "customErrorMsg";

		try {
		comparator
			.withOneTimeErrorMessage(customErrorMessage)
			.assertRowCount(-1); // fails
			fail("The assert should have failed");
		} catch (AssertionFailedError e) {
			assertEquals("The exception's message '" + e.getMessage() +
					"' shall start with the custom error '" + customErrorMessage +
					"' set previously"
					, 0, e.getMessage().indexOf(customErrorMessage));
		}
	}

	/**
	 * {@link RowComparator#withOneTimeErrorMessage(String)} should set the
	 * message so that it is removed after the following assert* no matter
	 * whether it failed or succeeded.
	 */
	public void testWithOneTimeErrorMessageGetsReset() {
		final String customErrorMessage = "customErrorMsg";
		comparator
			.withOneTimeErrorMessage(customErrorMessage)
			.assertRowCount(0); // should succeed

		try {
			comparator.assertRowCount(Integer.MIN_VALUE); // fails
			fail("The assert should have failed");
		} catch (AssertionFailedError e) {
			assertEquals("The exception's message '" + e.getMessage() +
					"' shall NOT contain the custom error '" + customErrorMessage +
					"' because it should have been reset after the 1st following assert*"
					, -1, e.getMessage().indexOf(customErrorMessage));
		}
	}

}
