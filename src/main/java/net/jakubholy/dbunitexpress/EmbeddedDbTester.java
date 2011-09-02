/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import junit.framework.Assert;
import net.jakubholy.dbunitexpress.assertion.RowComparator;
import net.jakubholy.dbunitexpress.exception.ExceptionInterpreterFactory;
import net.jakubholy.dbunitexpress.exception.IExceptionInterpreter;
import net.jakubholy.dbunitexpress.impl.EnhancedDatabaseTesterDecorator;
import net.jakubholy.dbunitexpress.util.DbUnitUtils;

import org.dbunit.DatabaseUnitException;
import org.dbunit.DatabaseUnitRuntimeException;
import org.dbunit.IDatabaseTester;
import org.dbunit.IOperationListener;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.NoSuchTableException;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The core class for unit tests that need to access a database, by default
 * an embedded (Derby) database, as simply as possible.
 * It relies on DbUnit to do most of the work, including reloading the
 * database with the test date before each test.
 * <p>
 * You will want to use this class if you prefer not to subclass the
 * {@link AbstractEmbeddedDbTestCase}, for instance if using Junit 4.
 * <p>
 * The class uses an embedded Derby DB for test data.
 * The necessary tables must be already created
 * - you may achieve that by creating a DDL file {@link DatabaseCreator#DDL_FILE_PATH}
 * (normally testData/create_db_content.ddl) and either executing
 * {@link DatabaseCreator#main(String[])} or calling {@link DatabaseCreator#createAndInitializeTestDb()}.
 * The database is expected to be in ./testData/testDB/ (a path relative to the
 * folder where your unit tests are executed from), which will be taken care of
 * if you use the DatabaseCreator to create the DB from a DDL.
 * </p><p>
 * If you want to load data from another file than the default one, you can
 * either use {@link #setDataSet(IDataSet)} or override {@link #createDefaultDataSet()}.
 * </p><p>
 * The added value of this class is that it's pre-configured to interact with
 * an embedded Derby database and the only thing you need to do is to provide
 * a DDL file, execute the {@link DatabaseCreator} to create the database,
 * and enter data into the predefined data set XML file. In addition to that it
 * also provides some nice utility methods and classes such as
 * {@link RowComparator} and {@link #getEnhancedTester()} and it overrides
 * the default DbUnit setting to
 * use fully qualified table names (in the form schema.table).
 * </p>
 *
 * <h3>How to use</h3>
 * Follow the 6 easy steps at the <a href="http://sourceforge.net/apps/mediawiki/jeeutils/index.php?title=DbUnit_Test_Skeleton#How_to_use_it">
 * homepage</a>.
 * Summary:
 * <ol>
 * 	<li>Add the necessary dependencies to your project (see the web page or the embedded pom.xml)
 * 	<li>Create the folder testData/ under your project's root folder.
 * 	<li>(optional) Prepare .ddl: Copy DbUnitTestSkeleton/testData/create_db_content.ddl into the new testData/ and modify it to fit your data schema needs.
 * 	<li>(optional) Create&amp;Initialize DB: Run DatabaseCreator#main from your project's root folder to create and initialize the test DB from the .ddl file.
 * 	<li>Write a TestCase either using {@link EmbeddedDbTester} or subclassing the
 * {@link AbstractEmbeddedDbTestCase} and implement your tests there.
 * (See SimpleEmbeddedDbTest.java for inspiration.) You may want to override its getDataSet()
 * if you want to use st. else than testData/dbunit-test_data_set.xml.
 * 	<li>Prepare test data: Copy DbUnitTestSkeleton/testData/dbunit-test_data_set.xml
 * into your new testData/ and modify it to fit your data needs. Any table mentioned
 * in the file will be emptied prior to running test, if it has any rows
 * defined then those will be also inserted.
 * 	<li>Run the test.
 * </ol>
 *
 * <h3>Example of usage</h3>
 * See the <a href="http://jeeutils.svn.sourceforge.net/viewvc/jeeutils/trunk/DbUnitTestSkeleton/src/main/test/net/jakubholy/dbunitexpress/SimpleEmbeddedDbTest.java?view=markup">
 * SimpleEmbeddedDbTest.java</a> which is a part of this project to see
 * a complete example.
 *
 * <h4>Verify data in DB</h4>
 * <pre><code>
 * // Inside a test class ...
 *
 * private EmbeddedDbTester testDb = new EmbeddedDbTester();
 *
 * public void setUp() {
 * 	testDb.onSetup();
 * }
 *
 * public void tearDown() {
 * 	testDb.onTearDown();
 * }
 *
 * public void testMyMethodXY() {
 * 	// ...
 * 	// after invoking the method under test:
 * 	ITable rowsToInsertTbl = testDb.getConnection().createQueryTable("rowToInsert", "SELECT MY_EXAMPLE_COLUMN FROM ... WHERE ...");
 * 	assertEquals("There shall be exactly 1 row for the given id.", 1, rowsToInsertTbl.getRowCount());
 * 	String dbVal = (String) rowsToInsertTbl.getValue(0, "MY_EXAMPLE_COLUMN");
 *  assertEquals("Value expected in the MY_EXAMPLE_COLUMN", "hello", dbVal);
 * 	}
 * </code></pre>
 *
 * <h3>Custom configuration and customizations</h3>
 * By default we use an embedded Derby database stored at a specific location
 * as defined by this class' constants. You may change the driver, database etc.
 * by providing a configuration of your own in a properties file, see
 * {@link #CUSTOM_CONFIG_FILE}.
 * <p>
 * You may also change what data set is used for initializing the database by
 * either overriding {@link #createDefaultDataSet()} or setting the
 * desired data set with {@link #setDataSet(IDataSet)}.
 *
 * See <a href="http://sourceforge.net/apps/mediawiki/jeeutils/index.php?title=DbUnit_Test_Skeleton">DbUnit_Test_Skeleton subproject of jeeutils at SourceForge</a>
 *
 * @see #onSetup()
 * @see #getEnhancedTester()
 * @see #createCheckerForSelect(String)
 * @see #getConnection()
 * @see #getDataSource()
 *
 * @see DatabaseCreator#createAndInitializeTestDb()
 * @see DatabaseCreator#createDbSchemaFromDdl(java.sql.Connection)
 * @see DatabaseCreator#DDL_FILE_PATH
 * @see DatabaseCreator#main(String[])
 *
 * @author jholy
 * @since 1.1.0
 */
public class EmbeddedDbTester implements IDatabaseTester {

	/**
	 * File's Subversion info (version etc.).
	 * It's replacement upon commit must be enabled in svn properties.
	 */
	public static final String SVN_ID = "$Id: EmbeddedDbTester.java 110 2011-07-25 15:41:26Z malyvelky $";

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedDbTester.class);

	//############# TEST DATA CONFIG ################
    /** Name of the folder storing our test data set XML, DB initialization DDL and the Derby db itself. */
	public static final String TEST_DATA_FOLDER = "testData";

	/**
	 * Name of our test data set XML file loaded by default into the database
	 * by this class unless overridden by a subclass.
	 * The file should be stored in the folder {@link #TEST_DATA_FOLDER}.
	 */
    public static final String DBUNIT_TEST_DATA_SET_NAME = "dbunit-test_data_set.xml"; // NOPMD
    //############# /TEST DATA CONFIG ################

	//############# JDBC CONNECTION INFO ################
	/**
	 * Path to the directory storing the test derby DB.
	 * May be absolute or relative to the test execution directory.
	 */
    private static final String DERBY_DB_PATH = TEST_DATA_FOLDER + File.separator + "testDB";

	/** Test DB connection info - url. */
	private static final String DEFAULT_JDBC_URL = "jdbc:derby:" + DERBY_DB_PATH;

	/** Test DB connection info - driver class name. */
	private static final String DEFAULT_JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	/** Test DB connection info - user name. */
	private static final String DEFAULT_JDBC_USERNAME = "sa";

	/** Test DB connection info - password. */
	private static final String DEFAULT_JDBC_PASSWORD = "";
	//############# /JDBC CONNECTION INFO ################

    /** DBUnit internal stuff that takes care of connecting to the DB. */
    private IEnhancedDatabaseTester tester;	// NOPMD

    private transient IDataSet testDataSet = null;

    /**
     * An optional file for defining other then the default connection to a test
     * database, it may include any of the following properties:
     * <ul>
     * 	<li> {@link PropertiesBasedJdbcDatabaseTester#DBUNIT_CONNECTION_URL}
     * 	<li> {@link PropertiesBasedJdbcDatabaseTester#DBUNIT_DRIVER_CLASS}
     * 	<li> {@link PropertiesBasedJdbcDatabaseTester#DBUNIT_PASSWORD}
     * 	<li> {@link PropertiesBasedJdbcDatabaseTester#DBUNIT_USERNAME}
     * </ul>
     * If a property isn't defined in the file then the value of the
     * appropriate DEFAULT_JDBC_* constant is taken.
     */
    public static final String CUSTOM_CONFIG_FILE = "dbunit-express.properties";

    private String customConfigFile = CUSTOM_CONFIG_FILE;

    private Properties connectionProps;	// NOPMD

	private transient DatabaseOperation setUpOperation = DatabaseOperation.CLEAN_INSERT;

	private transient DatabaseOperation tearDownOperation = DatabaseOperation.NONE;

	private IExceptionInterpreter exceptionInterpreter;

    /** For testing only. */
    static EmbeddedDbTester withPropertiesFile(String propertiesFileOnPath, String dataSet) {
        EmbeddedDbTester testDb = new EmbeddedDbTester(propertiesFileOnPath, dataSet);
        return testDb;
    }

    private EmbeddedDbTester(String configProperties, String xmlFileName) {
        if (configProperties != null) {
            this.customConfigFile = configProperties;
        }

        if (xmlFileName != null) {
            setDataSet(xmlFileName);
        }

        connectionProps = loadConnectionConfig();
        exceptionInterpreter = ExceptionInterpreterFactory.getDefaultInterpreter();
    }

    /**
     * Create a new embedded DB tester ready to use, optionally configured by
     * properties defined in the file {@value #CUSTOM_CONFIG_FILE} if
     * it's somewhere on the classpath.
     * <p>
     * Do not forget to call its {@link #onSetup()} before using it in a test
     * to modify or read the test database.
     */
    public EmbeddedDbTester() {
        this(null, null);
    }

    /**
     * Create a new embedded DB tester ready to use with a non-standard data
     * set file, optionally configured by
     * properties defined in the file {@value #CUSTOM_CONFIG_FILE} if
     * it's somewhere on the classpath.
     * <p>
     * Do not forget to call its {@link #onSetup()} before using it in a test
     * to modify or read the test database.
     *
     * @param xmlFileName (required) a XML file defining DbUnit data set
     * 	either in the testData folder or anywhere on the classpath
     *
     * @see #setDataSet(String)
     */
    public EmbeddedDbTester(final String xmlFileName) throws DatabaseUnitRuntimeException {
    	this(null, xmlFileName);

    }

	/**
	 * Load the connection configuration from {@value #CUSTOM_CONFIG_FILE}
	 * if available. Fail-safe.
	 * @return the loaded properties (empty if not found or an exception occurred)
	 */
	private Properties loadConnectionConfig() {
		final Properties properties = new Properties();
		final URL configUrl = EmbeddedDbTester.class.getResource(
                "/" + customConfigFile);

        if (configUrl != null) {
    		LOG.info("Loading test DB configuration from " + configUrl);
    		try {
				properties.load( configUrl.openStream() );
			} catch (IOException e) {
				LOG.warn("Failed to read DB configuration from " +
						configUrl + ": " + e, e);
			}
    	}
    	return properties;
	}

	/**
	 * This method let you set or override any of the connection properties,
	 * which are normally loaded from the {@link #CUSTOM_CONFIG_FILE} if it
	 * exists. A property set in this file or via this method has a higher
	 * priority than the default values. See the custom config file's JavaDoc
	 * to learn about the possible properties.
	 * <p>
	 * Beware that changing the properties has any effect on a database
	 * tester only before it is created. Via {@link #createAndInitDatabaseTester()}
	 * you will always get a new one based on the current values of all the
	 * properties but {@link #getWrappedTester()} only initializes its tester
	 * during the furst invocation.
	 * <p>
	 * To be used only for very special cases, such as in the
	 * {@link DatabaseCreator}.
	 * @param key (required)
	 * @param value (required)
	 * @return the previous value for that key or null
	 */
	protected final String setConnectionProperty(final String key, final String value) {
		return (String) connectionProps.setProperty(key, value);
	}

	/**
	 * Returns the value of the given connection property that would be used
	 * when creating a new database tester via {@link #createAndInitDatabaseTester()}.
	 * Based on the loaded/set connection properties and the default values.
	 * @param key (required) see the JavaDoc of {@link #CUSTOM_CONFIG_FILE} for
	 * 	possible values
	 * @return the property's value (normally should never be null)
	 * @throws IllegalArgumentException IF there is no such connection property
	 *
	 * @see #setConnectionProperty(String, String)
	 */
	protected final String resolveConnectionProperty(final String key) throws IllegalArgumentException {

		String value = connectionProps.getProperty(key);

		if (value == null) {
			if (PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS.equals(key)) {
				value = DEFAULT_JDBC_DRIVER;
			} else if (PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL.equals(key)) {
				value = DEFAULT_JDBC_URL;
			} else if (PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME.equals(key)) {
				value = DEFAULT_JDBC_USERNAME;
			} else if (PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD.equals(key)) {
				value = DEFAULT_JDBC_PASSWORD;
			} else {
				throw new IllegalArgumentException("The property '" + key +
						"' is not known. The supported properties are: " +
						PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS + "," +
						PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL + "," +
						PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME + "," +
						PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD);
			}
		}

		return value;
	}

    /**
     * Create the internally used database tester, which supports fully
     * qualified table names and uses an embedded Derby database.
     * <p>
     * We also set the system properties that define the test DB connection
     * based on the getCustomJdbc* methods or the DEFAULT_JDBC_*
     * constants like {@link #DEFAULT_JDBC_URL} if the methods return null.
     */
	protected final IEnhancedDatabaseTester createAndInitDatabaseTester() {

        final String driverClassName = resolveConnectionProperty(
        		PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS);
		System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS,
				driverClassName);
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL,
        		resolveConnectionProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL));
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME,
        		resolveConnectionProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME));
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD,
        		resolveConnectionProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD));
        //System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_SCHEMA, "MY_DEFAULT_SCHEMA" );

		System.setProperty("derby.locks.monitor", "true");			// required to enable deadlockTrace
		System.setProperty("derby.locks.deadlockTrace", "true");	// print the lock table upon deadlock/timeout

		try {
			final Class driverClass = Class.forName(driverClassName);
			exceptionInterpreter = ExceptionInterpreterFactory.getInterpreter(driverClass);
		} catch (ClassNotFoundException e1) {
			LOG.warn("createAndInitDatabaseTester: The driver class '" +
					driverClassName + "' cannot be found.");
		}

		IDatabaseTester actualTester;
		try {
			actualTester = new QualifiedNamesPropertiesTester(exceptionInterpreter);
		} catch (Exception e) {
			throw new RuntimeException("Error in constructor", e);
		}

		return new EnhancedDatabaseTesterDecorator(actualTester);
	} /* createDatabaseTester */

	/**
	 * A DatabaseTester that expects table names to be fully qualified, i.e.
	 * including a schema name. This makes it possible to use tables from
	 * different schemas in the same test.
	 */
	private static final class QualifiedNamesPropertiesTester extends
			PropertiesBasedJdbcDatabaseTester {

		private final IExceptionInterpreter exceptionInterpreter;

		public QualifiedNamesPropertiesTester(final IExceptionInterpreter exceptionInterpreter) throws Exception {
			super();
			this.exceptionInterpreter = exceptionInterpreter;
		}

		public IDatabaseConnection getConnection() throws Exception { // NOPMD
			try {
		        final IDatabaseConnection conn = super.getConnection();
		        conn.getConfig().setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, Boolean.TRUE);
		        return conn;
			} catch (SQLException e) {

				final String explanation = exceptionInterpreter.explain(e);

				if (explanation != null) {
					throw new DatabaseUnitRuntimeException(explanation, e);
				}

				throw e;
			}
		}

	} /* class QualifiedNamesPropertiesTester */

	/**
	 * Get the internally used tester implementation instance, if it is not
	 * set yet it will be created first.
	 * @see #createAndInitDatabaseTester
	 */
	protected final IEnhancedDatabaseTester getWrappedTester() {
		if (tester == null) {
			tester = createAndInitDatabaseTester();
		}
		return tester;
	}

	/**
	 * With this method a subclass may replace the internally used wrapper
	 * by calling it e.g. in the constructor.
	 */
	protected final void setWrappedTester(final IEnhancedDatabaseTester actualTester) {
		this.tester = actualTester;
	}

	/**
	 * Returns an enhanced manager of the test database that provides
	 * additional utility methods, for example for replacing its data.
	 *
	 * @see IEnhancedDatabaseTester#clearTable(String)
	 * @see IEnhancedDatabaseTester#replaceDatabase(String)
	 */
	public IEnhancedDatabaseTester getEnhancedTester() {
		return getWrappedTester();
	}

	/**
     * Create the default data set used to fill the test DB by reading the
     * default XML {@value #DBUNIT_TEST_DATA_SET_NAME}. Used if no
     * custom data set is provided via {@link #setDataSet(IDataSet)}.
     * Override if you want to load data from a different file(s)
     * or if you prefer a different format of the data sat (e.g. FlatXmlDataSet).
     */
    protected IDataSet createDefaultDataSet() throws DatabaseUnitRuntimeException, DataSetException {
		return createDataSetFromFile(DBUNIT_TEST_DATA_SET_NAME);
	}

    /**
     * Create a data set from the given XML file stored either in the default
     * location of {@value #TEST_DATA_FOLDER} or anywhere on the classpath.
     * @param xmlFileName (required) a XML file defining DbUnit data set
     * 	either in the testData folder or anywhere on the classpath
     * @return a new data set created from that file
     * @throws DatabaseUnitRuntimeException If the file cannot be found
     * @throws DataSetException If there is a problem with the data set format
     */
    public final IDataSet createDataSetFromFile(final String xmlFileName)
    throws DatabaseUnitRuntimeException, DataSetException {

        InputStream dataSetStream = findConfigFileStream(xmlFileName);

    	final XmlDataSet result = new XmlDataSet(dataSetStream);

    	// Log the data set
    	if ("true".equalsIgnoreCase(System.getProperty("dbunit.embeddeddb.dumpDataSet"))) {
    		final StringWriter writer = new StringWriter();
    		try {
				XmlDataSet.write(result, writer);
				LOG.info("createDataSetFromFile: loaded data set is:\n{}", writer);
			} catch (IOException e) {
				LOG.warn("createDataSetFromFile: Failed to dump the data " +
						"set due to " + e, e);
			}
    	}

    	return result;
    }

    /**
     * Try to find a file in all locations: in the testData/folder,
     * co-located with the calling class, on system classpath.
     * <p>
     *     Usually used to locate a data set file.
     * </p>
     * @param xmlFileName (required)
     * @return URL for the file
     * @trows DatabaseUnitRuntimeException when not found
     */
    public  static InputStream findConfigFileStream(String xmlFileName) throws DatabaseUnitRuntimeException {
        final URL dataSetUrl = findConfigFile(xmlFileName);

        try {
            return dataSetUrl.openStream();
        } catch (IOException e) {
            throw new DatabaseUnitRuntimeException(
                    "findConfigFileStream: Failed to find the file " +
                    dataSetUrl, e);
        }
    }

    static URL findConfigFile(String xmlFileName) {
        if (xmlFileName == null) {
            throw new IllegalArgumentException("String xmlFileName may not be null");
        }

        URL dataSetUrl;

        final String defaultPath =
            TEST_DATA_FOLDER + File.separator + xmlFileName;
        final File defaultFile = new File(defaultPath);

        if (defaultFile.canRead()) {
            try {
                dataSetUrl = defaultFile.toURI().toURL();
                LOG.info("findConfigFile: Loading " +
                        "file {} (found in the default location)"
                        , defaultFile.getAbsolutePath());
            } catch (MalformedURLException e) {
                throw new DatabaseUnitRuntimeException(e); // shouldn't happen...
            }
        } else {
            dataSetUrl = findOnClasspath(xmlFileName, defaultPath);
            LOG.info("findConfigFile: Loading " +
                    "the file {} found on the classpath at {}"
                    , xmlFileName, dataSetUrl);
        }
        return dataSetUrl;
    }

    /**
	 * Find a data set file on the class-path or fail.
	 * @param xmlFileName (required) the file to search for
	 * @param defaultPath (required) the location where the file was looked for
	 * 	originally (for logging)
	 * @return URL of the resource found (never null)
	 * @throws DatabaseUnitRuntimeException If the file cannot be found or opened
	 */
	private static URL findOnClasspath(final String xmlFileName, final String defaultPath)
			throws DatabaseUnitRuntimeException {

		URL dataSetOnCpUrl = null;

		// Try callers' classpath
		final List callers = extractCallStackClasses(new Exception().getStackTrace());
		for (final Iterator iterator = callers.iterator(); iterator.hasNext();) {
			final Class clazz = (Class) iterator.next();
			if ((dataSetOnCpUrl = clazz.getResource(xmlFileName)) != null) {
				break;
			}
		}

		// Try system classpath (not within a class' package)
		if (dataSetOnCpUrl == null) {
			dataSetOnCpUrl = Thread.currentThread().getContextClassLoader()
				.getResource(xmlFileName);
		}

		if (dataSetOnCpUrl == null) {
			final String msg = "The data set file '" +
				xmlFileName + "' can't be  found neither in the default " +
				defaultPath + " nor on the classpath of the [visible] call " +
						"stack classes [" + callers + "]; Notice that the " +
				"default search location is relative and thus depends on " +
				"the folder where you execute the tests from. Mavenists: " +
				"don't forget that non-java files uch as .xml are ignored " +
				"under /src/*/java/ and must be under src/*/resources/.";
			LOG.warn("findOnClasspath: " + msg);
			throw new DatabaseUnitRuntimeException(msg);
		} else {
			return dataSetOnCpUrl;
		}
	}

	/**
	 * Returns a list of classes in the order they're in the provided call stack.
	 * Classes that cannot be loaded (due to class loader isolation) are skipped
	 * and no class is added more than once.
	 * Also this class itself, java.lang.*, sun.reflect.*, and junit.framework.*
	 * classes are skipped.
	 * @param callStack (required) a call stack obtained e.g. via
	 * 	<code>new {@link Exception#getStackTrace()}</code>
	 * @return a non-null list of classes on the call stack that we can access
	 */
	private static List extractCallStackClasses(final StackTraceElement[] callStack) {

		if (callStack == null) {
			throw new IllegalArgumentException("The argument StackTraceElement[] callStack may not be null");
		}

		final List classes = new LinkedList();

		for (int i = 0; i < callStack.length; i++) {
			final String clazzName = callStack[i].getClassName();
			if (!(
					clazzName.startsWith(EmbeddedDbTester.class.getPackage().getName())
					|| clazzName.startsWith("java.lang.")
					|| clazzName.startsWith("sun.reflect.")
					|| clazzName.startsWith("junit.framework.")
				)) {
				try {
					final Class clazz = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
					if (!classes.contains(clazz)) {
						classes.add(clazz);
					}
				} catch (ClassNotFoundException e) {
					LOG.debug("extractCallStackClasses: the class '{}' on the " +
							"call stack couldn't be accessed from here, skipping; cause: {}"
							, clazzName, e);
				}
			}
		}

		return classes;
	}

	/**
	 * Returns a data source for accessing the underlying test database.
	 * A shortcut for {@link IEnhancedDatabaseTester#getDataSource()}.
	 * @throws DatabaseUnitRuntimeException
	 */
	public DataSource getDataSource() throws DatabaseUnitRuntimeException {
		return getWrappedTester().getDataSource();
	}

	/**
	 * Creates a connection to the underlying test database.
	 * @throws SQLException
	 * @throws DatabaseUnitRuntimeException
	 *
	 * @see #getConnection()
	 * @see IDatabaseConnection#getConnection()
	 */
	public Connection getSqlConnection() throws DatabaseUnitRuntimeException, SQLException {
		return getWrappedTester().getSqlConnection();
	}

	/**
	 * Create a new RowComparator for comparing results of a SQL select over
	 * the test database with your expectations. See an example of usage in
	 * the class' JavaDoc.
	 *
	 * @param sqlSelect (required) a SQL SELECT statement on the test DB
	 * @return a new comparator loaded with results of the sqlSelect
	 * @throws DatabaseUnitRuntimeException
	 *
	 * @see RowComparator#RowComparator(IDatabaseTester, String)
	 * @see RowComparator#assertRowCount(int)
	 * @see RowComparator#assertNext(Object[])
	 */
	public RowComparator createCheckerForSelect(final String sqlSelect) throws DatabaseUnitRuntimeException {
        try {
            return new RowComparator(getWrappedTester(), sqlSelect);
        } catch (SQLException e) {
            throw new DatabaseUnitRuntimeException("RowComparator creation failed for sql " + sqlSelect
                    , e);
        }
    }

	// ####################################################### INTERFACE METHODS

	/**
	 * Returns a connection to the underlying test database, which can
	 * be also used to obtain a normal SQL connection.
	 * @see IDatabaseConnection#getConnection()
	 * @see org.dbunit.IDatabaseTester#getConnection()
	 */
	public IDatabaseConnection getConnection() throws Exception {	// NOPMD
		return getWrappedTester().getConnection();
	}

	/**
     * Initiate the database by clearing tables and filling them with data from the data set.
     * Only tables that are mentioned in the data set are cleared of data.
     * <p>
     * The default set up operation and test data sets are used unless you
     * they have been changed via their setters (only rarely needed)
     * <p>
     * We've added some better error reporting in the case that the test DB
     * hasn't been created yet.
     *
     * @see org.dbunit.DatabaseTestCase#setUp()
     */
    public void onSetup() throws Exception {	// NOPMD
        try {
            final IDatabaseTester databaseTester = getWrappedTester();
            Assert.assertNotNull( "DatabaseTester is not set", databaseTester );
            databaseTester.setSetUpOperation( setUpOperation );
            databaseTester.setDataSet( getDataSet() );
            databaseTester.onSetup();
        } catch (NoSuchTableException e) {
            String currentDir = System.getProperty("user.dir");
            throw new DatabaseUnitException("No such table exception - " +
            		"have you created & initialized the embedded DB? " +
                    "(You can do so by calling DatabaseCreator.createAndInitializeTestDb or .main.)" +
                    "\n- the directory where this test is executed from is " + currentDir +
            		"\n- the missing table is '" + e.getMessage() + "'", e);
        } catch (Exception e) {
        	final String explanation = exceptionInterpreter.explain(e);

        	if (explanation == null) {
	        	if (e instanceof SQLException) {	// NOPMD
	        		LOG.warn("If the initial DB cleanup failed because of nonexistant schema " +
	        			"(usually same as logged-in user name, for derby SA), set the System property " +
	        			"PropertiesBasedJdbcDatabaseTester.DBUNIT_SCHEMA to an existing schema. Error: " +
	        			e.getMessage(), e);
	        	}

	        	throw new DatabaseUnitException(e);
        	} else {
        		final String jdbcUrl = resolveConnectionProperty(
        				PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL);
        		throw new DatabaseUnitException(explanation +
        				" Test DB URL: " + jdbcUrl
        				, e);
        	}
        }
	}

	/**
     * As of now this method doesn't do anything and so you can ignore it.
	 */
	public void onTearDown() throws Exception {	// NOPMD
		getWrappedTester().setTearDownOperation(tearDownOperation);
		getWrappedTester().onTearDown();
	}

	/**
	 * Get the test data set used for (re-)initializing the test DB.
	 * If none has been set via {@link #setDataSet(IDataSet)} then a default
	 * D.S. is created.
	 * @return the testDataSet
	 *
	 * @see IDatabaseTester#getDataSet()
	 * @throws DatabaseUnitRuntimeException Failure creating the data set
	 */
	public IDataSet getDataSet() throws DatabaseUnitRuntimeException {
		if (testDataSet == null) {
			try {
				testDataSet = createDefaultDataSet();
			} catch (DataSetException e) {
				throw new DatabaseUnitRuntimeException(
						"Failed to create the default test data set", e);
			}
		}
		return testDataSet;
	}

	/**
	 * If you do not want the default test data set to be used
	 * (read from {@value #TEST_DATA_FOLDER}/{@value #DBUNIT_TEST_DATA_SET_NAME})
	 * but prefer some other one, you can set it via this method.
	 *
	 * @param testDataSet the data to insert into the DB before each test
	 *
	 * @see IDatabaseTester#setDataSet(IDataSet)
	 */
	final public void setDataSet(final IDataSet testDataSet) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDataSet: entry for set="
					+ DbUnitUtils.describe(testDataSet));
		}
		this.testDataSet = testDataSet;
	}

	/**
	 * If you do not want the default test data set to be used
	 * (read from {@value #TEST_DATA_FOLDER}/{@value #DBUNIT_TEST_DATA_SET_NAME})
	 * but prefer some other one, you can set it via this method.
     * <p>
     *     It is a good practice - if you plan multiple tests - to have a separate data set for each,
     *     co-located with the test class. So for MyTest you would have e.g. MyTest-data.xml in the same folder
     *     and call testDb.setDataSet("MyTest-data.xml");
     *     It may be a good idea to create a parent test class that automatically calls
     *     setDataSet(getClass().getName() + "-data.xml");
     * </p>
	 *
	 * @param xmlFileName (required) a XML file defining DbUnit data set
     * 	either in the testData folder or anywhere on the classpath
	 * @throws DataSetException
	 * @throws DatabaseUnitRuntimeException
	 *
	 */
	final public void setDataSet(final String xmlFileName) throws DatabaseUnitRuntimeException {
		try {
			setDataSet(
					createDataSetFromFile(xmlFileName));
		} catch (DataSetException e) {
			throw new DatabaseUnitRuntimeException("Failed to create a " +
					"data set from the file '" +  xmlFileName +
					"' (on the classpath or in the default testData folder)."
					, e);
		}
	}

	/**
	 * This method is meaningless for this particular tester and thus throws
	 * an exception. Notice that if necessary you may still use unqualified names,
	 * which will then use the default schema for the test DB (SA for Derby).
	 *
	 * @throws UnsupportedOperationException This tester requires table names
	 * to be always fully qualified
	 * (i.e. schema.table) and thus setting a schema has no effect
	 * and shouldn't be used.
	 * @see org.dbunit.IDatabaseTester#setSchema(java.lang.String)
	 */
	public void setSchema(final String schema) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This tester requires table" +
				" names to be always fully qualified (i.e. schema.table)" +
				"and thus setting a schema has no effect and shouldn't be used. If the code you test doesn't use " +
				"schema then you must use the default schema of your database (SA for Derby) in your .ddl and " +
				"data XMLs.");

	}

	/**
	 * Normally you shouldn't need this.
	 * {@inheritDoc}
	 * @see org.dbunit.IDatabaseTester#setSetUpOperation(org.dbunit.operation.DatabaseOperation)
	 */
	public void setSetUpOperation(final DatabaseOperation setUpOperation) {
		this.setUpOperation = setUpOperation;
	}

	/**
	 * Normally you shouldn't need this.
	 * {@inheritDoc}
	 * @see org.dbunit.IDatabaseTester#setTearDownOperation(org.dbunit.operation.DatabaseOperation)
	 */
	public void setTearDownOperation(final DatabaseOperation tearDownOperation) {
		this.tearDownOperation = tearDownOperation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#setOperationListener(org.dbunit.IOperationListener)
     * @since 2.4.4
	 */
	public void setOperationListener(final IOperationListener operationListener) {
		getWrappedTester().setOperationListener(operationListener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.dbunit.IDatabaseTester#closeConnection(org.dbunit.database.IDatabaseConnection)
	 * @deprecated since 2.4.4 define a user defined {@link #setOperationListener(IOperationListener)} in advance
	 */
	public void closeConnection(final IDatabaseConnection connection)
			throws Exception {
		getWrappedTester().closeConnection(connection);
	}

}
