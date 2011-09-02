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

		shutdownTestDb();

		deleteTestDb();

		final EmbeddedDbTester embeddedDb = new EmbeddedDbTester();

		assertTestDbDoesntExist(embeddedDb);

		DatabaseCreator.createAndInitializeTestDb();

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
	 * @param embeddedDb
	 * @throws Exception
	 */
	private void assertTestDbDoesntExist(final EmbeddedDbTester embeddedDb)
			throws Exception {
		try {
			final IDatabaseConnection conn = embeddedDb.getConnection();
			fail("The test DB seems to exist already even though this test " +
					"should have deleted it before trying to (re)create it -" +
					" guessing from the fact that connecting to it hasn't " +
					"failed. Unfortunately " +
					"this way of determining its existence is not reliable " +
					"if some processes has left it previously in an " +
					"inconsistent state and the loaded Derby classes are " +
					"thus confused. The connection: " + conn);
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
	private void deleteTestDb() {
		final String testDbPath = EmbeddedDbTester.TEST_DATA_FOLDER + File.separator + "testDB";
		final File testDbFolder = new File(testDbPath);
		if (testDbFolder.exists()) {
			try {
				deleteFile(testDbFolder);
				if (testDbFolder.exists()) {
					fail("The test DB folder still exists even though the " +
							"method that should have deleted it has " +
							"reported success");
				} else {
					LOG.debug("deleteTestDb: The existing test DB has been " +
						"deleted");
				}
			} catch (IOException e) {
				fail("Failed to delete the test DB " + testDbFolder.getAbsolutePath() + ": " + e);
			}
		} else {
			LOG.debug("deleteTestDb: The test DB doesn't exist, no need to delete it.");
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


    public void testLoadCustomDdl() {
        final EmbeddedDbTester embeddedDb = new EmbeddedDbTester();

        DatabaseCreator creator = new DatabaseCreator();
        creator.loadDdl("DatabaseCreatorTest.ddl");

        embeddedDb.createCheckerForSelect("select * from new_custom_table")
                .assertRowCount(0);
        // shouldn't fail because of nonexistent table
    }

}
