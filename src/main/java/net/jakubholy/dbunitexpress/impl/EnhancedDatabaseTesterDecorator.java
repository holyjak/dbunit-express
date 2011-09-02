/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import net.jakubholy.dbunitexpress.IEnhancedDatabaseTester;
import net.jakubholy.dbunitexpress.util.DbUnitAsDataSourceAdapter;
import net.jakubholy.dbunitexpress.util.DbUnitUtils;

import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.IDatabaseTester;
import org.dbunit.IOperationListener;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.statement.SimplePreparedStatement;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decorator that adds the additional methods defined by
 * {@link IEnhancedDatabaseTester} to any {@link IDatabaseTester}.
 *
 * @author jholy
 *
 */
public class EnhancedDatabaseTesterDecorator implements IEnhancedDatabaseTester {

	private final transient IDatabaseTester actualTester;

	public static final String SVN_ID = "$Id: EnhancedDatabaseTesterDecorator.java 85 2010-03-09 13:55:25Z malyvelky $";

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedDatabaseTesterDecorator.class);

	/**
	 * Create a new enhanced tester that delegates all default operations
	 * to the provided actual tester.
	 * @param actualTester (required)
	 */
	public EnhancedDatabaseTesterDecorator(final IDatabaseTester actualTester) {
		if (actualTester == null) {
			throw new IllegalArgumentException("The argument IDatabaseTester actualTester may not be null");
		}
		this.actualTester = actualTester;
	}

	/*
	 * (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.IEnhancedDatabaseTester#replaceDatabase(java.lang.String)
	 */
	public void replaceDatabase(final String dataSetFile)
			throws FileNotFoundException, DatabaseUnitRuntimeException, DataSetException {
		LOG.debug("replaceDatabase('{}'): entry...", dataSetFile);
				// The data to load into the db replacing the old ones
				final IDataSet newDataSet = new CompositeDataSet(new IDataSet[]{
			             new XmlDataSet(new FileInputStream(dataSetFile))
			     });
				replaceDatabase( newDataSet );
			} /* replaceDatabase(fileName) */

	/*
	 * (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.IEnhancedDatabaseTester#replaceDatabase(org.dbunit.dataset.IDataSet)
	 */
	public void replaceDatabase(final IDataSet newDataSet) throws DatabaseUnitRuntimeException {

		LOG.debug("replaceDatabase({}): entry...", newDataSet);

		// Clean and load the database
		try {
		    final IDatabaseTester databaseTester = getActualTester();
		    databaseTester.setSetUpOperation( DatabaseOperation.CLEAN_INSERT );
		    databaseTester.setDataSet( newDataSet );
		    databaseTester.onSetup();
		} catch (Exception e) {

			IDatabaseConnection connection = null;
			try {
				connection = getConnection();
			} catch (Exception connExc) {
				LOG.warn("replaceDatabase: unable to access the DB", connExc);
			}

			LOG.error("replaceDatabase: failed; " +
					DbUnitUtils.describeWithDuplicates(newDataSet, connection) +
					" You may want to enable DEBUG log for " +
					SimplePreparedStatement.class + " to see the data being " +
					"inserted or use P6Spy."
					, e);
			throw new DatabaseUnitRuntimeException("Replacing the DB failed, " +
					"check the log for detailed information. Cause: " + e
					, e);
		}
	} /* replaceDatabase(IDataSet) */

	/*
	 * (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.IEnhancedDatabaseTester#clearTable(java.lang.String)
	 */
	public void clearTable(final String tableName) throws SQLException {

		LOG.debug("clearTable('{}'): entry...", tableName);

		final String sql = "delete from " + tableName;
		Statement stmt = null;	// NOPMD
		try {
			stmt = getActualTester().getConnection()
				.getConnection().createStatement();
			final int deleteCnt = stmt.executeUpdate(sql);
			LOG.debug("clearTable(" + tableName + "): deleted rows: " + deleteCnt);
		} catch (Exception e) {
			LOG.error("clearTable: Failure to delete rows from the table '" + tableName + "'", e);
			throw new SQLException("Failure to delete rows from the table '" +
					tableName + "', reason:" + e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	} /* clearTable */

	/*
	 * (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.IEnhancedDatabaseTester#getDataSource()
	 */
	public DataSource getDataSource() throws DatabaseUnitRuntimeException {
		try {
			return new DbUnitAsDataSourceAdapter(
					getActualTester());
		} catch (Exception e) {
			throw new DatabaseUnitRuntimeException("Failed to create a " + IDatabaseTester.class +
					" for this DbUnit test, please check check your environment " +
					"and condifuration."
					, e);
		}
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#closeConnection(org.dbunit.database.IDatabaseConnection)
	 * @deprecated since 2.4.4 define a user defined {@link #setOperationListener(IOperationListener)} in advance
	 */
	public void closeConnection(final IDatabaseConnection connection)
			throws Exception {	// NOPMD
		getActualTester().closeConnection(connection);
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#getConnection()
	 */
	public IDatabaseConnection getConnection() throws Exception {	// NOPMD
		return getActualTester().getConnection();
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#getDataSet()
	 */
	public IDataSet getDataSet() {
		return getActualTester().getDataSet();
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#onSetup()
	 */
	public void onSetup() throws Exception {	// NOPMD
		getActualTester().onSetup();
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#onTearDown()
	 */
	public void onTearDown() throws Exception {	// NOPMD
		getActualTester().onTearDown();
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#setDataSet(org.dbunit.dataset.IDataSet)
	 */
	public void setDataSet(final IDataSet dataSet) {
		getActualTester().setDataSet(dataSet);
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#setSchema(java.lang.String)
	 * @deprecated since 2.4.3 Should not be used anymore. Every concrete {@link IDatabaseTester} implementation that needs a schema has the possibility to set it somehow in the constructor
	 */
	public void setSchema(final String schema) {
		getActualTester().setSchema(schema);
	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#setSetUpOperation(org.dbunit.operation.DatabaseOperation)
	 */
	public void setSetUpOperation(final DatabaseOperation setUpOperation) {
		getActualTester().setSetUpOperation(setUpOperation);

	}

	/* (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#setTearDownOperation(org.dbunit.operation.DatabaseOperation)
	 */
	public void setTearDownOperation(final DatabaseOperation tearDownOperation) {
		getActualTester().setTearDownOperation(tearDownOperation);
	}

	/**
	 * @return the actual, decorated tester
	 */
	private IDatabaseTester getActualTester() {
		return actualTester;
	}

	/*
	 * (non-Javadoc)
	 * @see net.jakubholy.dbunitexpress.IEnhancedDatabaseTester#getSqlConnection()
	 */
	public Connection getSqlConnection() throws SQLException, DatabaseUnitRuntimeException {
		try {
			return actualTester.getConnection().getConnection();
		} catch (Exception e) {
			throw new DatabaseUnitRuntimeException(e);
		}
	}

	public void setOperationListener(final IOperationListener operationListener) {
		getActualTester().setOperationListener(operationListener);
	}

}
