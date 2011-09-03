package net.jakubholy.dbunitexpress;

import org.dbunit.DatabaseUnitRuntimeException;
import org.junit.rules.ExternalResource;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * The core class for unit tests that need to access a test database,
 * implemented as JUnit Rule and thus self-initializing before each test.
 * <p>
 *     See {@link EmbeddedDbTester} for details of usage
 * </p>
 *
 * <h3>Example of usage</h3>
 * Store the tester in a public, non-static field annotated with @Rule:
 * <pre><code>
 *     // Inside a test class ...
 *     
 *     @Rule public EmbeddedDbTester testDb = new EmbeddedDbTesterRule(); //("dataSetName.xml");
 *
 *     @Test
 *     public void your_test() {
 *         // The test DB is now initialized with data from your data set ...
 *         testDb.getDataSource();
 *         // do something ...
 *     }
 *
 * </code></pre>
 *
 * @see EmbeddedDbTester
 */
public class EmbeddedDbTesterRule extends EmbeddedDbTester implements TestRule {

    private class DbInitializer extends ExternalResource {
        @Override
        protected void before() throws Throwable {
                EmbeddedDbTesterRule.this.onSetup();
        };

        /*@Override
        protected void after() {
                myServer.disconnect();
        };*/
    }

    private DbInitializer initializer = new DbInitializer();

    public EmbeddedDbTesterRule() {
        super();
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

    public EmbeddedDbTesterRule(String xmlFileName) throws DatabaseUnitRuntimeException {
        super(xmlFileName);
    }

    /** Ignore - for internal use by JUnit's Rule handling. */
    public final Statement apply(Statement statement, Description description) {
        return initializer.apply(statement, description);
    }
}
