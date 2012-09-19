package net.jakubholy.dbunitexpress;

import org.junit.Rule;
import org.junit.Test;

public class ExampleJUnit4WithRuleTest {

	/**
	 * Initialize the test and instruct it to use a custom data set file instead of the default dbunit-test_data_set.xml.
	 * The set up of the test DB will be executed automaticaly thanks to the magic of @Rule.
	 */
    @Rule
    public EmbeddedDbTesterRule testDb = new EmbeddedDbTesterRule("EmbeddedDbTesterRuleTest-data.xml");

    @Test
    public void should_execute_onSetup_automatically() throws Exception {
    	// 1. TODO: Invoke the database-using class that you want to test, passing to it the test database
    	// via testDb.getDataSource() or testDb.getSqlConnection()
    	// ex.: new MyUserDao(testDb.getDataSource()).save(new User("Jakub", "Holy"));
    	
    	// 2. Verify the results ...
    	// Here we use a checker to check the content of the my_test_table loaded from the EmbeddedDbTesterRuleTest-data.xml
        testDb.createCheckerForSelect("select some_text from my_test_schema.my_test_table")
                .withErrorMessage("No data found => onSetup wasn't executed as expected")
                .assertRowCount(1)
                .assertNext("EmbeddedDbTesterRuleTest data");
    }
}
