package net.jakubholy.dbunitexpress;

import org.junit.Rule;
import org.junit.Test;

public class EmbeddedDbTesterRuleTest {

    @Rule
    public EmbeddedDbTesterRule testDb = new EmbeddedDbTesterRule("EmbeddedDbTesterRuleTest-data.xml");

    /**
     * onSetup should have been executed automatically thanks to the @Rule and thus
     * our data should be in the DB.
     */
    @Test
    public void shold_execute_onSetup_automatically() throws Exception {
        testDb.createCheckerForSelect("select some_text from my_test_schema.my_test_table")
                .withErrorMessage("No data found => onSetup wasn't executed as expected")
                .assertRowCount(1)
                .assertNext("EmbeddedDbTesterRuleTest data");
    }
}
