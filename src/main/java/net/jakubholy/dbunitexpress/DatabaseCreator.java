/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for initializing the embedded Derby test database.
 * The database must be created and filled with tables needed for testing
 * before it can be used in unit tests.
 * <p>
 * It creates a test Derby database at the default location and
 * fills it with data structures defined in a DDL file expected at
 * the default location {@value #DDL_FILE_PATH}.
 * <p>
 * You should run this as a java console application (i.e. the main method)
 * to create the DB.
 *
 * @see #createDbSchemaFromDdl(Connection)
 */
public class DatabaseCreator {

	/** Name of the DDL file. */
	public static final String DDL_FILE_NAME = "create_db_content.ddl";

	/** Path to the DDL file, normally below {@link EmbeddedDbTester#TEST_DATA_FOLDER}. */
	public static final String DDL_FILE_PATH = EmbeddedDbTester.TEST_DATA_FOLDER + File.separator + DDL_FILE_NAME;

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseCreator.class);

	/**
	 * Run a DDL on the (empty) test database to create the schemas and tables
	 * to fill with test date.
	 * The DDL is read from the file {@link #DDL_FILE_PATH}.
	 * <p>
	 * WARNING: The code cannot handle all the stuff you may use in a DDL.
	 * More exactly everything that you can pass to {@link Statement#execute(String)}
	 * is OK, in addition to that we can handle empty lines and comment lines
	 * starting with '--'.
	 *
	 * @param connection (required) A connection to the test db; e.g. {@link AbstractEmbeddedDbTestCase#getDatabaseTester()}#getConnection()#getConnection().
	 * @throws FileNotFoundException The DDL file not found
	 * @throws IOException Failed to read the DDL
	 * @throws SQLException Failure during execution of the DDL
	 * @throws Exception
	 */
    public static void createDbSchemaFromDdl(final Connection connection) throws FileNotFoundException, IOException, Exception, SQLException {
    	if (connection == null) {
    		throw new IllegalArgumentException("The argument connection: java.sql.Connection may not be null.");
    	}
        LOG.info("createDbSchemaFromDdl: Going to initialize the test DB by creating the schema there...");
        final String sql = readDdlFromFile();

        LOG.info("createDbSchemaFromDdl: DDL read: " + sql);
		executeDdl(connection, sql);

        LOG.info("createDbSchemaFromDdl: done");
    } /* createDbSchemaFromDdl */

    /**
     * Execute the ddlStatements on the connection.
     * @param connection (required) connection to the target database.
     * @param ddlStatements (required) DDL statements separated by semi-colon (';').
     * It shouldn't contain any -- comments as JDBC may not be able to ignore them as appropriate.
     * @throws SQLException
     */
	private static void executeDdl(final Connection connection, final String ddlStatements)
			throws SQLException {

		final java.sql.Statement ddlStmt = connection.createStatement();
		try {
	        final String[] statements = ddlStatements.split(";");

	        for (int i = 0; i < statements.length; i++) {
	        	if (statements[i].trim().length() > 0) {
		            LOG.info("createDbSchemaFromDdl: Adding batch stmt: " + statements[i]);
		            ddlStmt.addBatch(statements[i]);
	        	}
	        }

	        ddlStmt.executeBatch();
		} finally {
			try {
				ddlStmt.close();
			} catch (SQLException e) {
				LOG.warn("Failed to close the statement", e);
			}
		}
	} /* executeDdl */

    /**
     * Read the DDL from the file {@link #DDL_FILE_PATH} into a String.
     * @return the DDL
     * @throws FileNotFoundException
     * @throws IOException
     */
	private static String readDdlFromFile()
			throws FileNotFoundException, IOException {
		// DbUnit can't handle line comments within ddl => skip
        final FileReader fileReader = new FileReader(getDdlFile());
        final BufferedReader sqlReader = new BufferedReader(fileReader);
        final StringBuffer sqlBuffer = new StringBuffer();
        String line;
        while ((line = sqlReader.readLine()) != null) {
        	line = line.trim();
            if (line.length() > 0 && !line.startsWith("--")) {
                sqlBuffer.append(line).append('\n');
            }
        }

        try {
        	sqlReader.close();
        } catch (IOException e) {
        	LOG.info("readDdlFromFile: Failed to close the DDL input stream", e);
        }

		return sqlBuffer.toString();
	} /* readDdlFromFile */

	private static File getDdlFile() {
		return new File(DDL_FILE_PATH);
	}

    /**
     * Run as a java console program to create and initialize the embedded Derby DB.
     * DB connection info is taken over from the {@link AbstractEmbeddedDbTestCase}.
     * <p>
     * To retry, simply delete the database (normally testData/testDB/).
     *
     * @param args ignored
     * @throws Exception
     *
     * @see #createAndInitializeTestDb
     */
    public static void main(String[] args) throws Exception {
    	createAndInitializeTestDb();
	} /* main */

    /**
     * Create and from a DDL initialize the embedded Derby DB.
     * DB connection info is taken over from the {@link AbstractEmbeddedDbTestCase}
     * (where it is hard-coded).
     * <p>
     * To retry, simply delete the database (normally testData/testDB/).
     *
     * @throws Exception
     *
     * @see #createDbSchemaFromDdl(java.sql.Connection)
     * @see #DDL_FILE_PATH
     * @see EmbeddedDbTester#createAndInitDatabaseTester()
     */
	public static void createAndInitializeTestDb() throws Exception,
			FileNotFoundException, IOException, SQLException {

		// Get a tester that we will use to access the DB;
    	// thus we're sure we access the same DB the tests will access
    	final EmbeddedDbTester embeddedDb = new EmbeddedDbTester();

    	// Modify the DB URL to tell Derby to create the DB if not existing
    	final String oldDbConnUrl = embeddedDb.resolveConnectionProperty(
    			PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL );

    	if (oldDbConnUrl == null) {
    		// This shouldn't happen but let's be safe...
    		throw new IllegalStateException("The required property " +
    				PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL +
    				" is not set (yet).");
    	}

    	// Create st. like "jdbc:derby:testData/testDB;create=true"
    	final String selfCreatingDbConnUrl = oldDbConnUrl + ";create=true"; // not very foolproof...

    	embeddedDb.setConnectionProperty(
    			PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL
    			, selfCreatingDbConnUrl);

		// Get a connection - the connection properties will be applied now
    	final IDatabaseConnection dbUnitConnection =
    		embeddedDb.createAndInitDatabaseTester().getConnection();

    	// Initialize the DB content
    	try {
    		DatabaseCreator.createDbSchemaFromDdl( dbUnitConnection.getConnection() );
    	} catch (SQLException e) {
    		final String msg = "DDL execution failed. DB URL: '"
    				+ selfCreatingDbConnUrl + "' (created in the current work dir: " +
    				System.getProperty("user.dir") + "). DDL file: " +
    				getDdlFile().getAbsolutePath();
			LOG.error("createAndInitializeTestDb" + msg, e);
    		throw new DatabaseCreatorFailure(msg, e);
    	} finally {
    		try {
				dbUnitConnection.close();
			} catch (SQLException e) {
				LOG.warn("Failed to close the connection", e);
			}
    	}
	} /* createAndInitializeTestDb */

	private static class DatabaseCreatorFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public DatabaseCreatorFailure(String message, Throwable cause) {
			super(message, cause);
		}



	}

}
