/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.assertion;

import java.util.Date;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.jakubholy.dbunitexpress.test.DatabaseUtils;

import org.dbunit.dataset.DefaultTable;

/**
 * Basic tests of the row comparator using an in-memory table
 * (not a real database).
 */
public class RowComparatorTest extends TestCase {

	private DefaultTable testTable;

	protected void setUp() throws Exception {
		super.setUp();
		testTable = DatabaseUtils.createTestTable();
	}

	public void testAssertNextStringStringString() throws Exception {
		final Date date = new Date();
		testTable.addRow(new Object[]{"s1", new Integer(1), date});
		final RowComparator comparator = new RowComparator(testTable);

		comparator.assertRowCount(1);
		comparator.assertNext("s1", "1", date.toString());
	}

	public void testAssertNextStringStringString_nulls() throws Exception {
		testTable.addRow(new Object[]{null, null, null});
		final RowComparator comparator = new RowComparator(testTable);

		comparator.assertRowCount(1);
		comparator.assertNext(null, null, null);
	}

	public void testAssertRowCount_2Rows() throws Exception {
		testTable.addRow(new Object[]{"s1", new Integer(1), new Date()});
		testTable.addRow(new Object[]{"s2", new Integer(2), new Date()});
		final RowComparator comparator = new RowComparator(testTable);
		comparator.assertRowCount(2);
		try {
			comparator.assertRowCount(0);
			fail("Should fail beause row count is 2");
		} catch (AssertionFailedError e) {}
	}

	/**
	 * The comparator permits a string-based comparison when all values are
	 * converted into string before comparing them, this happens if the expected
	 * values is a String array. Otherwise the class of the actual and expected
	 * value is taken into account.
	 * @see RowComparator#assertNext(String[])
	 * @see RowComparator#assertNext(Object[])
	 * @see #testAssertNextWithObjectComparisonFailsIfClassesDiffer()
	 */
	public void testAssertNextWithStringIgnoresTypeDifferences() throws Exception {

		testTable.addRow(new Object[]{"s2", new Integer(2), null});

		final RowComparator comparator = new RowComparator(testTable);

		comparator.assertNext(new String[]{"s2", "2", null});
	}

	/**
	 * Object-based comparison performed by {@link RowComparator#assertNext(Object[])}
	 * fails if the classes of the expected and the actual value differ as
	 * opposed to the string-based comparison in {@link RowComparator#assertNext(String[])},
	 * which ignores this difference.
	 * @see #testAssertNextWithStringIgnoresTypeDifferences()
	 */
	public void testAssertNextWithObjectComparisonFailsIfClassesDiffer() throws Exception {

		testTable.addRow(new Object[]{"s2", new Integer(789456), new Date(0)});

		final RowComparator comparator = new RowComparator(testTable);

		try {
			comparator.assertNext(new Object[]{"s2", "789456", new Date(0)});
			fail("The assertion should have failed because we expect the 2nd " +
					"column to be a String while in reality it is an Integer.");
		} catch (AssertionFailedError e) {
			// Failed as expected
			assertTrue("It seems the test failed for other reason than " +
					"expected, i.e. not because of the type difference of " +
					"column 2 (value 789456); error: " + e.getMessage()
					, e.getMessage().indexOf("789456") >= 0);
			assertTrue("The error message should report the differing types;" +
					" the actual error: [" + e.getMessage() + "]"
					, e.getMessage().indexOf("Expected type: java.lang.String, " +
							"actual type: java.lang.Integer") >= 0);

		}
	}

	public void testComparisonWithValueCheckerWorks() throws Exception {

		testTable.addRow(new Object[]{"s2", new Integer(-123), new Date(0)});

		final RowComparator comparator = new RowComparator(testTable);

		final ValueChecker acceptingChecker = new ValueChecker() {
			public void assertAcceptable(Object actualValue)
					throws AssertionFailedError {
				return;
			}
		};

		// Should succeed:
		comparator.assertNext(
			"This should have succeeded because the ValueChecker accepts that value of the date"
			, new Object[]{"s2", new Integer(-123), acceptingChecker});
	}

	public void testComparisonWithRejectingValueCheckerFails() throws Exception {

		testTable.addRow(new Object[]{"s2", new Integer(-123), new Date(0)});
		final RowComparator comparator = new RowComparator(testTable);

		final ValueChecker rejectingChecker = new ValueChecker() {
			public void assertAcceptable(Object actualValue)
			throws AssertionFailedError {
				throw new AssertionFailedError("This evil checker rejects everything!");
			}
		};

		try {
			comparator.assertNext(
				"Failed because the ValueChecker rejected the date's value"
				, new Object[]{"s2", new Integer(-123), rejectingChecker});
			fail("Should have failed because the ValueChecker rejects the date");
		} catch (AssertionFailedError e) {
			assertTrue("The failure message shall contain the message " +
					"produced by the checker; actual: [" + e.getMessage() + "]"
					, e.getMessage().indexOf("This evil checker rejects everything!") >= 0);
		}
	}

}
