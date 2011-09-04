package net.jakubholy.dbunitexpress;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple test to verify that creation of a test DB works.
 *
 */
public class DatabaseCreatorTest extends TestCase {

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseCreatorTest.class);

	/**
	 * verify that creation of a test DB works.
	 * It relies on some knowledge of how the EmbeddedDbTester works
	 * and may thus require to be updated when it changes.
	 */
	public void testCreateAndInitializeTestDb() throws Exception {

        LOG.info("RUNNING TEST testCreateAndInitializeTestDb");

        // Clean up before the test - needed when not fresh run
		shutdownTestDb();
		deleteTmpTestDB();

		final EmbeddedDbTester embeddedDb = EmbeddedDbTester.withPropertiesFile("dbunit-express-tmpTestDB.properties", null);

		assertTestDbDoesntExist(embeddedDb);

		new DatabaseCreator(embeddedDb).doCreateAndInitializeTestDb();

		// Verify the DB does exist now
		embeddedDb.getConnection();
	}

    /**
	 * Shut down the DB prior to deletion it to avoid inconsistency between
	 * memory and disk. It will be automatically restarted upon the
	 * next connection attempt.
	 * @throws IllegalArgumentException
	 */
	private void shutdownTestDb() throws IllegalArgumentException {

		final String originalJdbcUrl = new EmbeddedDbTester()
			.resolveConnectionProperty(
    			PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL );
		final String shutdownJdbcUrl = originalJdbcUrl + ";shutdown=true";

		try {
			LOG.debug("testCreateAndInitializeTestDb: Trying to shutdown " +
					"the test DB by connecting to " +
					shutdownJdbcUrl);
			DriverManager.getConnection(shutdownJdbcUrl);
		} catch (SQLException e) {
			// Shutdown always throws an exception
			LOG.debug("Derby DB shut down: " + e);
		}
	}

	/**
	 * Fail if the test DB exists.
	 * @throws Exception
     * @param embeddedDb
	 */
	private void assertTestDbDoesntExist(EmbeddedDbTester embeddedDb)
			throws Exception {
		try {
            final IDatabaseConnection conn = embeddedDb.getConnection();
            embeddedDb.createCheckerForSelect("select * from sys.SYSSCHEMAS where SCHEMANAME='noSuchSchema'")
                    .assertRowCount(0); // should fail because of non-exist. table if DB not initialized
			fail("The test DB seems to exist already even though this test " +
                    "should have deleted it before trying to (re)create it -" +
                    " guessing from the fact that connecting to it hasn't " +
                    "failed. Unfortunately " +
                    "this way of determining its existence is not reliable " +
                    "if some processes has left it previously in an " +
                    "inconsistent state and the loaded Derby classes are " +
                    "thus confused. \nThe connection: " + conn);
		} catch (DatabaseUnitRuntimeException e) {
			LOG.info("testCreateAndInitializeTestDb: OK - connecting to the " +
					"DB failed as expected, which hopefully indicates that " +
					"it indeed doesn't exist");
		} catch (SQLException e) {
			LOG.info("testCreateAndInitializeTestDb: OK - connecting to the " +
					"DB failed as expected, which hopefully indicates that " +
					"it indeed doesn't exist");
		}
	}

	/**
	 * Delete the test DB if it exists.
	 */
	private void deleteTmpTestDB() {
		final String testDbPath = EmbeddedDbTester.TEST_DATA_FOLDER + File.separator + "tmpTestDB";
		final File testDbFolder = new File(testDbPath);
		if (testDbFolder.exists()) {
			try {
				deleteFile(testDbFolder);
				if (testDbFolder.exists()) {
					fail("The test DB folder still exists even though the " +
							"method that should have deleted it has " +
							"reported success");
				} else {
					LOG.debug("deleteTmpTestDB: The existing test DB has been " +
						"deleted");
				}
			} catch (IOException e) {
				fail("Failed to delete the test DB " + testDbFolder.getAbsolutePath() + ": " + e);
			}
		} else {
			LOG.debug("deleteTmpTestDB: The test DB doesn't exist, no need to delete it.");
		}
	}

	/**
	 * Delete a file/folder even if it is not empty.
	 * It may fail if we can't delete something, e.g. because it's open
	 * by another process or because a new file was added to a folder while
	 * we were cleaning it.
	 * @param file (required)
	 * @throws IOException Deletion not successful
	 */
	private void deleteFile(final File file) throws IOException {
		if (file.isDirectory()) {
			final File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++) {
				deleteFile(children[i]);
			}
		}

		if ( !file.delete() ) {
			throw new IOException("Deletion of the file/folder " + file.getAbsolutePath() +
					" has not succeeded. (Maybe because it's a non-empty folder?)");
		}
	}

    public void test_can_load_ddl_into_existing_db() throws Exception {

        LOG.info("RUNNING TEST test_can_load_ddl_into_existing_db");

        final EmbeddedDbTester inMemoryEmbeddedDb = EmbeddedDbTester.withPropertiesFile(
                "dbex-derby_in_memory.properties", null);
        DatabaseCreator dbCreator = new DatabaseCreator(inMemoryEmbeddedDb);
        // Notice we don't execute DB creation - the in-memory DB is self-creating because our conn. string already
        // contains ;create=true
        dbCreator.loadDdl("DatabaseCreatorTest-additional2.ddl");

        assertTableCreated(inMemoryEmbeddedDb, "new_custom_table2");
    }

    public void test_can_initialize_db_with_custom_ddl() throws Exception {

        LOG.info("RUNNING TEST test_can_initialize_db_with_custom_ddl");

        final EmbeddedDbTester inMemoryEmbeddedDb = EmbeddedDbTester.withPropertiesFile(
                "dbex-derby_in_memory-non_selfcreating.properties", null);
        DatabaseCreator dbCreator = new DatabaseCreator(inMemoryEmbeddedDb);

        // The in-memory DB shouldn't exist right now, let's verify it
        try {
            inMemoryEmbeddedDb.getConnection();
            fail("Should have failed because the test db shouldn't have been created yet");
        } catch (DatabaseUnitRuntimeException e) {}

        // Create it ...
        dbCreator.setDdlFile("DatabaseCreatorTest-additional2.ddl")
                .doCreateAndInitializeTestDb();

        // Verify that now it is ok
        assertTableCreated(inMemoryEmbeddedDb, "new_custom_table2");
    }

    //############################################################################

    private void assertTableCreated(EmbeddedDbTester embeddedDb, String tableName) {
        embeddedDb.createCheckerForSelect("select * from " + tableName)
                .assertRowCount(0);
        // shouldn't fail because of nonexistent table
    }

}
