/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.util;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various utility methods for working with data sets etc.
 *
 * @author jakub.holy@ibacz.eu
 *
 */
final public class DbUnitUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DbUnitUtils.class);

	private DbUnitUtils() {}

	/**
	 * Create a log String with info about the data set including table names
	 * and their row counts.
	 * @param dataSet (optional) - returns "null" if null
	 */
	public static String describe(final IDataSet dataSet) {
		return describeWithDuplicates(dataSet, null);
	}

	/**
	 * Create a log String with info about the data set including table names,
	 * their row counts and rows with a duplicate primary key.
	 * @param dataSet (optional) - returns "null" if null
	 * @param connection (optional) - necessary to fetch primary key info
	 */
	public static String describeWithDuplicates(final IDataSet dataSet, final IDatabaseConnection connection) {
		final StringBuffer result = new StringBuffer();

		if (dataSet == null) {
			result.append("null");
		} else {
			try {
				result.append("DataSet(class=")
						.append(dataSet.getClass())
						.append(") with tables(row count): ");

				for (final ITableIterator iterator = dataSet.iterator(); iterator.next();) {

					final ITable table = iterator.getTable();
					result.append(table.getTableMetaData().getTableName())
						.append('(').append(table.getRowCount()).append(") ");

					if (connection != null) {
						final Set duplicates = findPkDuplicates(table, connection);
						if (!duplicates.isEmpty()) {
							result.append("[duplicated primary keys: ")
								.append(duplicates)
								.append("] ");
						}
					}
				}
			} catch (DataSetException e) {
				LOG.warn("toString(DataSet): failed to access dataset's tables", e);
			}
		}

		return result.toString();
	}

	/**
	 * Find all (single or composed) primary keys that are duplicated in the
	 * table and can thus lead to an integrity violation exception.
	 * @param table (required)
	 * @param connection (required)
	 * @return Set&lt;String&gt; of primary keys that appear more than once in
	 * 	the table; composed primary keys are separated by '|'. Returns an
	 * 	empty set if no duplicates.
	 * @throws DataSetException
	 */
	protected static Set findPkDuplicates(final ITable table, final IDatabaseConnection connection) throws DataSetException {

		final String tableName = table.getTableMetaData().getTableName();
		final Set duplicates = new HashSet();

		Column[] keys = null;

		try {
			keys = getPrimaryKeys(connection, tableName);
		} catch (Exception e) {
			LOG.warn("findPkDuplicates: failed to fetch actual PKs from DB for " + tableName, e);
		}


		if (keys != null && keys.length > 0) {
			final Set rows = new HashSet(table.getRowCount());

			final StringBuffer joinKey = new StringBuffer();
			for (int rowIdx = 0; rowIdx < table.getRowCount(); rowIdx++) {

				for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
					final String keyColName = keys[keyIdx].getColumnName();
					if (joinKey.length() > 0) {
						joinKey.append('|');
					}
					joinKey.append(table.getValue(rowIdx, keyColName));
				}

				if (!rows.add(joinKey.toString())) {
					duplicates.add(joinKey.toString());
					LOG.debug("findPkDuplicates({}): duplicate found for primary key='{}'"
							, tableName
							, joinKey);
				}
				joinKey.setLength(0);
			}

		} else {
			LOG.debug("findPkDuplicates({}): no primary keys on the table"
					, tableName);
		}

		return duplicates;
	}

	/**
	 * Returns the actual primary keys for a table.
	 * @param databaseConnection (required)
	 * @param tableName (required) The table name; if using fully-qualified names
	 * 	it must be fully qualified ("schema.table")
	 * @return the table's primary keys or an empty array, if none defined
	 * @throws SQLException See {@link IDatabaseConnection#createDataSet()}
	 * @throws DataSetException See {@link IDataSet#getTableMetaData(String)} and
	 * 	{@link ITableMetaData#getPrimaryKeys()}
	 */
	public static Column[] getPrimaryKeys(final IDatabaseConnection connection, final String tableName)
			throws DataSetException, SQLException {
		// DatabaseDataSet returns the DatabaseTableMetaData whose
		// .getPrimaryKeys() uses the java.sql.DatabaseMetaData to fetch
		// the actual keys as opposed to a ResultSetTableMetaData, which is
		// created with keys=new Column[0]
		final ITableMetaData tableMetaData = connection
			.createDataSet()
			.getTableMetaData(tableName);
		final Column[] keys = tableMetaData.getPrimaryKeys();
		return (keys == null)? new Column[0] : keys;
	}

}
