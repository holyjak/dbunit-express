package net.jakubholy.dbunitexpress;

import net.jakubholy.dbunitexpress.assertion.RowComparator;
import org.junit.Before;
import org.junit.Test;

/**
 * An example of an annotation-based, JUnit 4 simple unit test verifying data in the test database,
 * using directly the {@link EmbeddedDbTester} instead of extending the
 * {@link net.jakubholy.dbunitexpress.AbstractEmbeddedDbTestCase}.
 * <p>
 *
 * @see SimpleEmbeddedDbTest
 */
public class SimpleNonExtendingEmbeddedDbJUnit4Test {

    private final EmbeddedDbTester testDb = new EmbeddedDbTester();

    @Before
	public void setUp() throws Exception {
		testDb.onSetup();
	}

	/**
	 * Test that DB is correctly loaded with the expected data prior to running the test.
	 * <p>
	 * <h4>Prerequisites</h4>
	 * <ol>
	 * 	<li>The database has been created
	 *  <li>and the schemas and tables have been defined within the DB
	 * </ol>
	 */
    @Test
	public void testIt_withRowComparator() throws Exception {

		// setUp has filled the DB with data, we will just verify they're as expected.
		final String sql = "select * from my_test_schema.my_test_table order by id asc";
		final RowComparator rowComparator = testDb.createCheckerForSelect(sql);

        // Variant 1: data type sensitive comparison:
		rowComparator.assertRowCount(3)
                .assertNext(new Object[]{ new Integer(1), "some text #1, xml must be escaped like in & , >"})
		        .assertNext(new Object[]{new Integer(2), "some xml entities may be here like in &, <>"});

		// Variant 2: We can also make the call simple if comparison of toStrings is enough:
		rowComparator.assertRowCount(3).assertNext("3", (String) null);

	}
}