/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.assertion;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import net.jakubholy.dbunitexpress.exception.ExceptionInterpreterFactory;
import net.jakubholy.dbunitexpress.exception.IExceptionInterpreter;

import org.dbunit.Assertion;
import org.dbunit.DBTestCase;
import org.dbunit.DatabaseTestCase;
import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for comparing actual and expected values of the test
 * database' rows using DbUnit. It loads data from the test database into
 * an internal result table ({@link ITable}) and provides convenient
 * methods for checking its content.
 * <p>
 * You may also consider using directly the assert methods for ITable
 * and IDataSet in {@link Assertion}, perhaps together with
 * {@link DefaultTable#DefaultTable(String, Column[])}.
 *
 * <h4>Standard usage</h4>
 * You would use it as follows from a subclass of {@link DatabaseTestCase}:
 * <pre><code>
 * final Product[] expected = new Product[] { ... };
 * RowComparator cmp = new RowComparator(getDatabaseTester()
 * 		, "select productId, price from eshop.products order by productId");
 * cmp.assertRowCount(3);
 * cmp.assertNext(new Object[]{1, 1000.00});	// requires Java 5's autoboxing
 * cmp.assertNext(new Object[]{2, 333.50});
 * cmp.assertNext("3", "99.0"); // using the convenience String comparison
 * </code></pre>
 *
 *
 * <h4>Subclassing for convenience</h4>
 * You can create a subclass to make verification of
 * your data/objects more convenient, something like
 * <pre><code>
 * private class ProductRowComparator extends RowComparator {
 *
 * 	private final Product[] expected;
 *
 * 	public ProductRowComparator(final IDatabaseTester databaseTester, final Product[] expected) {
 * 		super(databaseTester, "select productId, price from eshop.products order by productId");
 * 		this.expected = expected.clone();
 * 		java.util.Arrays.sort(this.expected); // sort by productId
 * 	}
 *
 * 	public void assertRowsEqual()  throws AssertionFailedError, DataSetException {
 * 		super.assertRowCount(expected.length);
 * 		for (int i = 0; i < expected.length; i++) {
			assertNext(new Object[]{ expected[i].getProductId(), expected[i].getPrice() });
		}
 * 	}
 *
 * }
 * </code></pre>
 * You would than use it as follows:
 * <pre><code>
 * final Product[] expected = new Product[] { ... };
 * new ProductRowComparator(getDatabaseTester(), expected).assertRowsEqual();
 * </code></pre>
 *
 */
public class RowComparator {


	private static final Logger LOG = LoggerFactory.getLogger(RowComparator.class);

	private String[] columnNames;	// NOPMD
	private ITable resultTable;		// NOPMD
	private int currentRow = -1;	// NOPMD

	private IExceptionInterpreter exceptionInterpreter =
		ExceptionInterpreterFactory.getDefaultInterpreter();

	private CustomErrorMessage errorMessage = new CustomErrorMessage();

	/**
	 * A new comparator that creates the internal resultTable ({@link ITable})
	 * from the provided SQL and accesses the test database via the
	 * provided tester.
	 * @param databaseTester (required) necessary to read the test DB;
	 * 	see {@link DBTestCase#getDatabaseTester()}
	 * @param sql (required) the SQL SELECT to execute
	 * @throws SQLException
	 * @throws DatabaseUnitRuntimeException
	 */
	public RowComparator(final IDatabaseTester databaseTester, final String sql) throws SQLException, DatabaseUnitRuntimeException {
		initResultTableAndColumns(databaseTester, sql);
	}

	/**
	 * Check expected values against the provided {@link ITable}.
	 * @param resultTable (required)
	 * @throws DataSetException
	 */
	public RowComparator(final ITable resultTable) throws DataSetException {
		initResultTableAndColumns(resultTable);
	} /* constructor(ITable) */

	/**
	 * You must call {@link #initResultTableAndColumns(ITable)} or
	 * {@link #initResultTableAndColumns(IDatabaseTester, String)} if using this constructor.
	 */
	protected RowComparator() { /* for subclasses to override */ }

	/**
	 * Use the SQL to create an ITable and call {@link #initResultTableAndColumns(ITable)}.
	 * @param databaseTester
	 * @param sql (required)
	 * @throws SQLException
	 * @throws DatabaseUnitRuntimeException by {@link IDatabaseTester#getConnection()}
	 */
	protected final void initResultTableAndColumns(final IDatabaseTester databaseTester
			, final String sql) throws SQLException, DatabaseUnitRuntimeException {
		if (databaseTester == null) {
			throw new IllegalArgumentException("The argument databaseTester: IDatabaseTester may not be null.");
		}

		try {
			exceptionInterpreter = ExceptionInterpreterFactory.getInterpreter(
					getConnection(databaseTester).getConnection());
		} catch (Exception e) {
			LOG.warn("initResultTableAndColumns: Failed to access the " +
					"connection/metadata and thus will not be able to " +
					"instantiate the appropriate ExceptionInterpreter.", e);
		}

		try {
			final ITable resultTable = getConnection(databaseTester).
				createQueryTable("rowComparatorTbl", sql);
			initResultTableAndColumns(resultTable);
		} catch (Exception e) {
			final String explanation = exceptionInterpreter.explain(e);
			if (explanation == null) {
				throw new DatabaseUnitRuntimeException(e);
			} else {
				throw new DatabaseUnitRuntimeException(explanation, e);
			}
		}

	} /* initResultTableAndColumns(String) */

	/** Get a connection to the test database; subclasses may override. */
    protected IDatabaseConnection getConnection(final IDatabaseTester databaseTester) throws Exception {
        return databaseTester.getConnection();
    }

	/**
	 * Set {@link #resultTable} and use its metadata to initialize {@link #columnNames}.
	 * @param resultTable (required)
	 * @throws DataSetException
	 */
	protected final void initResultTableAndColumns(final ITable resultTable) throws DataSetException {
		if (resultTable == null) {
			throw new IllegalArgumentException("ITable resultTable may not be null");
		}
		this.resultTable = resultTable;
		this.columnNames = extractColumnNames();
	} /* initResultTableAndColumns(ITable) */

	/**
	 * Extract column names from {@link #resultTable}.
	 * @throws DataSetException
	 * @see ITable#getTableMetaData()
	 * @see ITableMetaData#getColumns()
	 */
	private String[] extractColumnNames() throws DataSetException {
		final Column[] columns = resultTable.getTableMetaData().getColumns();
		final String[] columnNames = new String[columns.length];	// NOPMD

		for (int i = 0; i < columns.length; i++) {
			columnNames[i] = columns[i].getColumnName();	// NOPMD
		}
		return columnNames;
	} /* extractColumnNames */

	/**
	 * See {@link #assertNext(String, Object[])}.
	 */
	public RowComparator assertNext(final Object[] expectedValues) throws AssertionFailedError, DataSetException {
		return assertNext(null, expectedValues);
	}

	/**
	 * See {@link #assertNext(String, String[])}.
	 */
	public RowComparator assertNext(final String[] expectedValues) throws AssertionFailedError, DataSetException {
		return assertNext(null, expectedValues);
	}

	/**
	 * Compare the next row (or the 1st one if first use) with the actual data,
	 * ignoring differences in type between the expected and actual values.
	 * (This as achieved by converting the actual values to String via
	 * .toString() before comparing them.)
	 * <p>
	 * If you do not care about checking the actual value types this method
	 * may be more convenient to use because it's easy to construct strings.
	 * Notice that {@link #assertNext(String, Object[])} fails if the
	 * type of the expected and actual value differs (unless the expected
	 * values are passed as an array of Strings).
	 * @param msg (optional) A message to add to the AssertionFailedErrors produced
	 * @param expectedValues (required) the values to be expected in the columns;
	 * the values must be in the same order as columns in the internal {@link ITable}.
	 *
	 * @see #assertNext(String, Object[])
	 */
	public RowComparator assertNext(final String msg, final String[] expectedValues) throws AssertionFailedError, DataSetException {
		return assertNext(msg, (Object[]) expectedValues);
	}

	/**
	 * Compare the next row (or the 1st one if first use) in this.{@link #resultTable} with
	 * the provided expected values and throw an assertion failure if they differ.
	 * <p>
	 * If the row differs in a column then an AssertionFailedError is thrown
	 * including the row number, name of the column and the expected and actual
	 * values.
	 * <p>
	 * The type of the actual and expected values is taken into account when
	 * comparing them unless expectedValues is actually a String[]. See
	 * {@link #assertNext(String, Object[])} for type-insensitive comparison.
	 *
	 * <h4>Using a ValueChecker</h4>
	 * You have two options for comparing the actual and expected values: either
	 * providing the exact expected value, which will be tested via .equals()
	 * or providing an instance of a {@link ValueChecker} to which to delegate
	 * the check. See an example in its JavaDoc.
	 *
	 * @param msg (optional) A message to add to the AssertionFailedErrors produced
	 * @param expectedValues (required) the values to be expected in the columns;
	 * the values must be in the same order as columns in the internal {@link ITable}.
	 *
	 * @see ITable#getTableMetaData()
	 * @see ITableMetaData#getColumns()
	 * @see ValueChecker
	 */
	public RowComparator assertNext(final String msg, final Object[] expectedValues) throws AssertionFailedError, DataSetException {

		try {
			final String errorMsg = ((msg == null)? "" : msg) +		// NOPMD
					errorMessage.getTextDecorated(" [","] ", "");

			checkAssertNextParams(++currentRow, expectedValues);

			final boolean stringComparison = expectedValues.getClass() // NOPMD
				.getComponentType().equals(String.class);

			// Do the test
			for (int i = 0; i < columnNames.length; i++) {
				Object actual = resultTable.getValue(currentRow, columnNames[i]);
				if (stringComparison && actual != null) {
					actual = actual.toString();
				}

				final Object expected = expectedValues[i];

				final String columnUnequalMsg = errorMsg + " (row (starting from 0) " + currentRow +
						", column '" + columnNames[i] + "')" +
						createTypeDifferenceInfo(actual, expected);

				if (expected instanceof ValueChecker) {
					checkWithChecker(columnUnequalMsg, (ValueChecker) expected, actual);
				} else {
					Assert.assertEquals(columnUnequalMsg, expected, actual);
				}
			}
		} finally {
			errorMessage.assertDone();
		}

		return this;
	}

	/**
	 * Check the value of a column using a {@link ValueChecker}.
	 * @param message (optional)
	 * @param expectedChecker (required) the checker
	 * @param actual (optional) the actual value of the current row and column
	 * @throws AssertionFailedError
	 */
	private void checkWithChecker(final String message,
			final ValueChecker expectedChecker, final Object actual)
			throws AssertionFailedError {
		try {
			expectedChecker.assertAcceptable(actual);
		} catch (AssertionFailedError e) {
			throw new AssertionFailedError(message +
					" Failed ValueChecker test: " +
					e.getMessage());
		} catch (ClassCastException e) {
			final String actualType = (actual == null)?
					"(the actual value is null)" : actual.getClass().getName();
			throw new DatabaseUnitRuntimeException(
					"ClassCastException in a checker - likely the actual " +
					"value is of a different type than expected; its type is: " +
					actualType + "; additional info: " + message
					, e);
		}
	}

	/**
	 * Check that the parameters for {@link #assertNext(String, Object[])}
	 * are legal and throw an exception if not
	 * @param updatedCurrentRow the number of the row that should have been chekced
	 * @param expectedValues
	 * @throws IllegalArgumentException
	 */
	private void checkAssertNextParams(final int updatedCurrentRow, final Object[] expectedValues)
			throws IllegalArgumentException, AssertionFailedError {

		Assert.assertTrue("There is no next row, the row count is " + resultTable.getRowCount()
				, updatedCurrentRow < resultTable.getRowCount());

		if (expectedValues == null) {
			throw new IllegalArgumentException("Object[] expectedValues may not be null");
		}
		if (columnNames.length != expectedValues.length) {
			throw new IllegalArgumentException("columnNames.length (" + columnNames.length +
					") shall be same as expectedValues.length (" + expectedValues.length + ")");
		}
	}

	/**
	 * Create a message with the classes of the actual and expected objects
	 * if their types differ.
	 * @param actual (optional)
	 * @param expected (optional)
	 */
	private String createTypeDifferenceInfo(final Object actual, final Object expected) {
		String typeInfo = "";
		if (expected != null && actual != null
				&& !expected.getClass().equals(actual.getClass())
				&& !ValueChecker.class.isAssignableFrom(expected.getClass())) {
			typeInfo = " Expected type: " + expected.getClass().getName() +
				", actual type: " + actual.getClass().getName();
		}
		return typeInfo;
	}

	/**
	 * Assert that the values of the next row are same as the three ones provided
	 * here after being converted to strings via toString().
	 * This is a convenience method that makes asserting particular values
	 * easier (because you don't need to create an Object array and wrap your
	 * primitives) if string comparison is sufficient for you.
	 * <p>
	 * To be used only if the underlying table has exactly three columns.
	 * @param expectedFirst (optional) the expected value of the first column
	 * @param expectedSecond (optional) the expected value of the 2nd column
	 * @param expectedThird (optional) the expected value of the 3rd column
	 * @throws AssertionFailedError
	 * @throws DataSetException
	 */
	public RowComparator assertNext(final String expectedFirst, final String expectedSecond, final String expectedThird) throws AssertionFailedError, DataSetException {
		return assertNext(null, new String[]{expectedFirst, expectedSecond, expectedThird});
	}

	/**
	 * Same as {@link #assertNext(String, String, String)} but for a two-columns table.
	 * @param expectedFirst (optional)
	 * @param expectedSecond (optional)
	 * @throws AssertionFailedError
	 * @throws DataSetException
	 */
	public RowComparator assertNext(final String expectedFirst, final String expectedSecond) throws AssertionFailedError, DataSetException {
		return assertNext(null, new String[]{expectedFirst, expectedSecond});
	}

	/**
	 * Same as {@link #assertNext(String, String, String)} but for a one-column table.
	 * @param expectedFirst (optional)
	 * @throws AssertionFailedError
	 * @throws DataSetException
	 */
	public RowComparator assertNext(final String expectedFirst) throws AssertionFailedError, DataSetException {
		return assertNext(null, new String[]{expectedFirst});
	}

	/**
	 * Assert that the number of rows in this.{@link #resultTable} is as expected.
	 */
	public RowComparator assertRowCount(final int expected) throws AssertionFailedError {
		Assert.assertEquals(errorMessage.getTextForPrepend() + "There shall be " +
				expected + " rows in total. The SQL or test data is likely incorrect."
				, expected, resultTable.getRowCount());
		errorMessage.assertDone();
		return this;
	} /* assertRowCount */

	/** Return the internal result table. */
	public ITable getResultTable() {
		return resultTable;
	} /* getResultTable */

	/**
	 * Use this method to set a custom error message that will be added to
	 * any subsequent failure message. Call with null to reset.
	 * <p>
	 * Example:
	 * <pre><code>
	 * new RowComparator(aTester, "select * from schema.table")
	 * 	.withErrorMessage("The DB should be empty now")
	 * 	.assertRowCount(0);
	 * </code></pre>
	 * @param customErrorMessage (optional)
	 * @return this
	 *
	 * @see #withOneTimeErrorMessage(String)
	 */
	public RowComparator withErrorMessage(final String customErrorMessage) {
		this.errorMessage.set(customErrorMessage, false);
		return this;
	}

	/**
	 * Set a custom error message that will be added to the next assert failure
	 * message, if it fails, and reset thereafter. While {@link #withErrorMessage(String)}
	 * is persistent and applies to any assert* until it is reset, this one will
	 * be reset immediately after the first call to an assert*.
	 * @param customErrorMessage (optional)
	 * @return this
	 */
	public RowComparator withOneTimeErrorMessage(final String customErrorMessage) {
		this.errorMessage.set(customErrorMessage, true);
		return this;
	}



    /**
     * Print the actual results of the query, for troubleshooting. (It might be little slow.)
     * @param sqlChecker
     * @throws DataSetException
     */
    public void printSqlResults() throws DataSetException {
        ITable resultTable = this.getResultTable();
        List<String> columns = new ArrayList<String>();
        for (Column column : resultTable.getTableMetaData().getColumns()) {
            columns.add(column.getColumnName());
        }
        StringBuilder tableCsv = new StringBuilder();

        tableCsv.append(columns).append('\n');

        for (int rowIdx = 0; rowIdx < resultTable.getRowCount(); rowIdx++) {
            List<Object> values = new ArrayList<Object>(columns.size());
            for (String column : columns) {
                values.add(resultTable.getValue(rowIdx, column));
            }
            tableCsv.append(values).append('\n');
        }

        System.out.format("Select results:\n%s", tableCsv);
    }

	/**
	 * A custom error message provided by the user,
	 * it can be either one time only (applies to the following assert*)
	 * or permanent (applies until reset).
	 */
	private static class CustomErrorMessage {

		private String text = null;
		private boolean oneTime = false;

		public void set(final String text, final boolean oneTime) {
			this.text = text;
			this.oneTime = oneTime;
		}

		/**
		 * Call at the end of each assert - it will reset the message text
		 * if it was set to be one-time only.
		 * @return true if the message was reset
		 */
		public boolean assertDone() {
			if (oneTime) {
				this.text = null;
			}
			return oneTime;
		}

		/**
		 * Return the message text formatted for insertion in the front
		 * of an assert failure message or "" if null.
		 */
		public String getTextForPrepend() {
			return getTextDecorated("", " Details: ", "");
		}

		/**
		 * Return the message text with the decorations or, if it is null, the
		 * ifNull variant
		 * @param prepend (required) see return
		 * @param append (required) see return
		 * @param ifNull (required) see return
		 * @return IF text is null THEN ifNull ELSE prepend + text + append
		 */
		public String getTextDecorated(final String prepend, final String append, final String ifNull) {
			return (text == null)? ifNull : prepend + text + append;
		}

		public String toString() {
			return text;
		}
	}

}
