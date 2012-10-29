---
layout: layout
title: DbUnit Express
---

DbUnit Express
==============


Creating a a [DbUnit][1] test case may be time consuming. With this project you can have your DbUnit test including an embedded database with structures and data ready in a few minutes. (At least if you are fast enough in writing SQL and preparing your data&nbsp;:-) ). 

 [1]: http://www.dbunit.org/ "http://www.dbunit.org/"

DbUnit Express is a thin wrapper around DbUnit (unit testing of code interacting with a database) intended to make starting with DB testing as quick and as easy as possible by introducing convention over configuration, by using an embedded Derby database out of the box, and by providing:

* An [EmbeddedDbTester\[Rule\].java][23], which creates a connection to the database, sets some useful defaults, and provides additional utility methods and improved error reporting
* The utility [DatabaseCreator.java](https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/DatabaseCreator.java) for creating and initializing the embedded DB based on a .ddl file
* Sample [create\_db\_content.ddl][2] and [dbunit\-test\_data\_set.xml][0] for creating the database and filling it with test data
* Various utility classes and methods
* Instructions for using them

 [2]: https://raw.github.com/jakubholynet/dbunit-express/master/testData/create_db_content.ddl "https://raw.github.com/jakubholynet/dbunit-express/master/testData/create_db_content.ddl"
 [0]: https://raw.github.com/jakubholynet/dbunit-express/master/testData/dbunit-test_data_set.xml

Everything is well commented to help you to start using it quickly. 

    *TODO:* ToC
    1 DbUnit Express subproject
    1.1 Basic usage
    1.1.1 Connecting to the test database
    1.1.1.1 Obtaining a java.sql.Connection or javax.sql.DataSource
    1.1.1.2 Connecting via JDBC (embedded Derby)
    1.2 Examples
    1.3 What you should not miss
    1.3.1 Why to use this?
    1.3.2 Custom configuration and customizations
    1.3.2.1 Custom database, driver, and settings via dbunit-express.properties
    1.4 Get it!
    1.5 JavaDoc
    1.6 Justification of the defaults
    1.6.1 Why non-flat XML data set?
    1.6.2 Why Derby?
    1.7 FAQ
    1.7.1 The code I test uses table names without schema but DbUnit Express requires qualified names, what to do?
    1.8 Tips on advanced usage
    1.8.1 Prepare data with Jailer the DB sampler
    1.8.2 Define your own convention for data set names, e.g. per-test-class-data-set
    1.8.3 Use custom database (f.ex. in-memory)
    1.8.4 DRY: Get rid of setUp by using a parent test class or JUnit Rules
    1.8.5 Use dbunit-express with other data set formats (flat, csv, ...)
    1.8.6 Disable DbUnit tests when a system property is set
    1.9 Difference between dbunit-express and DbUnit
    1.10 Version history
    1.10.1 NEXT VERSION
    1.10.2 VERSION 1.3.0 (Sep 2011)
    1.10.3 VERSION 1.2.0 (Apr 2010)
    1.10.4 VERSION 1.1.0 (Feb 2010)
    1.10.5 VERSION 1.0.2 (Nov 2009)
    1.10.6 VERSION 1.0.1
    1.11 Appendices
    1.11.1 Using dbunit-express without Maven
    1.11.2 Using custom JUnit version (default: latest between 4.9 and 5)
    // ToC

Basic usage
----------

Note: you may prefer to download the *-dist.zip* file over the standard Maven downloads as it contains example .ddl and data XML to get you started faster. 

1. With Maven, add the latest [net.jakubholy.testing:dbunit-express](http://search.maven.org/#search|gav|1|g%3A%22net.jakubholy.testing%22%20AND%20a%3A%22dbunit-express%22) to your pom. Otherwise read "Using dbunit-express without Maven" below.
2. **Create the folder** *testData*/ under your project's root folder. 
3. (optional) **Prepare .ddl**: Copy testData/[create\_db\_content.ddl][2] into the new testData/ and modify it to fit your data schema needs. 
4. (optional) **Create&amp;Initialize DB**: dbunit-express can create DB tables etc. from the .ddl file. Either configure dbunit-express to initialize the DB automatically (below) or do it manually by running `DatabaseCreator#main` from your project's root folder. 
5. **Prepare test data**: Copy dbunit-express-\*-dist.zip/testData/[dbunit\-test\_data\_set.xml][0] into your new testData/ and modify it to fit your data needs. Any table mentioned in the file will be emptied prior to running test, if it has any rows defined than those will be also inserted.
    * You may use e.g. [Jailer][3] (good [tutorial][4]) to produce a subset of your database in DbUnit XML format (unfortunately only Flat XML) or another tool supporting it such as [QuantumDB][5].
6. **Write a TestCase**: 
    1.  JUnit 4.9+ - use preferably the EmbeddedDbTesterRule with @Rule and a public field so that onSetup is called automatically as in [ExampleJUnit4WithRuleTest.java][6] (but perhaps without passing
        a custom data file to it)
    2.  JUnit 4.0 - 4.8: Use EmbeddedDbTester explicitly as in [SimpleNonExtendingEmbeddedDbJUnit4Test.java][7] 
    3.  JUnit 3.x: Subclass AbstractEmbeddedDbTestCase as in [SimpleEmbeddedDbTest.java][8] (or, if you can't subclass it, use dbunit-express in a stand-alone mode as in [SimpleNonExtendingEmbeddedDbJUnit3Test.java][9])  You may want to override its getDataSet() if you want to use st. else than testData/dbunit-test\_data\_set.xml (for example supplying data set produced by createDataSetFromFile(name)).
7. **Run the test** 
* * *

### Connecting to the test database

Your code under test may get access to the database via a Connection or a DataSource. 

#### Obtaining a java.sql.Connection or javax.sql.DataSource

You can obtain a `javax.sql.DataSource` or a `java.sql.Connection` to access the test database via the methods `getDataSource()` and `getSqlConnection()` on any of the two core classes. 

#### Connecting via JDBC (embedded Derby)

You can connect to the embedded Derby database manually using any standard JDBC tool if you provide it the driver included in the derby-.jar. 

The connection URL is something like: 

    jdbc:derby:/absolute/path/to/testData/testDB
    

(you could also try the relative path, *jdbc:derby:testData/testDB*) 

The credentials for the connection are: 

*   user name: "sa" 
*   password: "" (an empty string) 

* * *

## Examples

Example DbUnit test code: 

    public class SimpleEmbeddedDbTest extends net.jakubholy.dbunitexpress.AbstractEmbeddedDbTestCase {
       public void testSomething() {
          // 1. Call the code that changes something in the DB
          ...
          // 2. Verify the results are as expected
          org.dbunit.dataset.ITable allRowsTbl = getConnection().createQueryTable( "allRows"
             , "select some_text from my_test_schema.my_test_table order by id asc"  );
          Object actual = resultTable.getValue(0,  "some_text" );
          assertEquals(  actual, "some text #1, xml must be escaped like in & , >" )
       }.
    }

Notice that the database is cleared and loaded with test data automatically during the implicit call to setUp(). 

Another complete test using JUnit 4 and Java 5+. You only need to create a .ddl and define test data in a XML (templates provided). 

    public class MyDerbyDbTest {
    
     // @Rule public EmbeddedDbTesterRule testDb = new EmbeddedDbTesterRule(); // with this you don't neet to call onSetup
     private EmbeddedDbTester testDb = new EmbeddedDbTester();
     
     @Before void setUp() throws Exception { testDb.onSetup();}
    
     @Test public void testIt() throws Exception {
    
     new MyTestedClass( testDb.getSqlConnection() ).insertGeek(1, "Jakub Holy");
    
     testDb.createCheckerForSelect("SELECT id, name FROM myschema.geeks")
       .assertRowCount(1)
       .assertNext(new Object[]{ 1, "Jakub Holy"});
    }}
    

See the two sample test classes provided with this project for more and more complex examples. 

## What you should not miss

You should have a look at the following to grab all the knowledge you may need: 

* **Review the example tests** - [SimpleEmbeddedDbTest.java][8] and [SimpleNonExtendingEmbeddedDbJUnit3Test.java][9] or [SimpleNonExtendingEmbeddedDbJUnit4Test.java][7]
* **Check the class-level JavaDoc** of [AbstractEmbeddedDbTestCase.java][22] and [EmbeddedDbTester.java][23] to learn when and how to use each one of them and what they provide to you

### Why to use DbUnit Express?

The added value of this project is that provides classes pre-configured to interact with an embedded Derby database and the only thing you need to do is to provide a DDL file, execute the DatabaseCreator to create the database, and enter data into the predefined data set XML file. In addition to that it also provides some nice utility methods and classes such as [RowComparator][10] and [IEnhancedDatabaseTester][11] and it overrides the default DbUnit setting to use fully qualified table names (in the form schema.table), which gives you more freedom. 

If any of the configuration doesn't suite you, you can always override it - use a custom database, different data files etc.

### Custom configuration and customizations

You may change the DB, driver, etc. via `dbunit-express.properties`, see below. 

You may also change what data set is used for initializing the database by either overriding `EmbeddedDbTester.createDefaultDataSet()` or setting the desired data set with `EmbeddedDbTester.setDataSet(IDataSet)` or `overriding AbstractEmbeddedDbTestCase.getDataSet()`. 

Also check the tips and FAQ sections below. 

#### Custom database, driver, and settings via dbunit-express.properties

If you have *dbunit-express.properties* on your classpath then dbunit-express will read the following properties from it (showing the default values here): 

       ## Connection properties:
       #dbunit.driverClass=org.apache.derby.jdbc.EmbeddedDriver
       #dbunit.connectionUrl=jdbc:derby:testData/testDB
       #dbunit.username=sa
       #dbunit.password=
       ## Should the DB be initialized automatically from the .ddl if the tables do not exist?
       ## (Works currently only for Derby)
       #dbunit-express.autoInitializeDb=false
    

## Get it!

**Requirements**: Java 1.4, JUnit 3.8 or higher (though Java 5+ and JUnit 4.9+ are recommended)  
**Latest release**: [1.3.0, available in Maven Central][12] 

## JavaDoc

* [v1.3.0 JavaDoc](http://www.jarvana.com/jarvana/view/net/jakubholy/testing/dbunit-express/1.3.0/dbunit-express-1.3.0-javadoc.jar!/index.html?net/jakubholy/dbunitexpress/EmbeddedDbTester.html) via Jarvana
* [v1.1.0 JavaDoc](http://jeeutils.sourceforge.net/dbunit-embeddedderby/javadoc/1.1.0)

## Justification of the defaults

### Why non-flat XML data set?

DbUnit Express uses the standard XML data set, which is more verbose (you have to name the columns and use `<value>...</value>` for the values) than the flat XML (`<tableName column1Name="value" ... />`) mainly because it works well with null values.

In the case of the flat XML either the first row may not have null in any column or you must provide an external DTD for it - thus becoming similarly verbose and falling into the trap of xml &lt;=> dtd references (in theory it should be easy to refer to a DTD next to the XML, in practice I've wasted many hours trying to get this to work). An alternative is a replacement data set, but I hadn't success with it either (for a date column). The standard XML has no such issues and its verbosity is a small price for that. 

### Why Derby?

By default the project uses an embedded [Derby][13] database though you can configure it to use also another one. The reasons for choosing Derby as the embedded DB were: 

*   Derby 10.6+ is [included in JDK 1.6+][14] under the name [Java DB][15] (but without any changes to the source codes) and thus people should be familiar with it 
*   Originally created by IBM, the SQL it supports is the closest to IBM DB2 you can get 
*   It can store the data both only [in memory][16] and on the disk (useful for their examination when a test fails) 

You may be interested in a [matrix comparing HSQLDB (H2) and Derby][17] (and other DBs). 

## FAQ

### The code I test uses table names without schema but DbUnit Express requires qualified names, what to do?

If the code you test uses unqualified table names (as in "select * from mytable") then it implicitely relies on the default schema of the target database and you can just do the same thing with the test database. 

For Derby the default schema is SA (the user name) so you would use it wherever a schema name is required, namely in your .ddl and data .xml files. For example: 

    -- create_db_content.ddl
    create schema SA;
    CREATE TABLE SA.YOUR_TABLE ( ... );

and: 

    <!-- dbunit-test_data_set.xml -->
    <dataset>
       <table name="SA.YOUR_TABLE"> ...

Then it is ok if your code uses: 

    ResultSet rs = statement.executeQuery("select * from your_table"); // no schema here

* * *

## Tips on advanced usage

### Prepare data with Jailer the DB sampler

[Jailer][3] is an open-source GUI application for extracting a subset of database data that is complete regarding the referential integrity constraints. 

Don't be scared by all the notion on its page. Just read the tutorial, read the points below and start using it. It's actually quite easy. 

Noteworthy: 

*   Can anonymize data - define a filter (Edit - Filter Editor). 
*   Can export into DbUnit flat XML (`<employee> <empno>123</empno> <fname>John</fname> ... </employee>`). 
*   Normally exports all related data, for example not only the selected employee but also all other employees in the same department. You may define a restriction that disables the inverse relationships direction (from department to employee) to prevent this. 
*   Jailer can introspect you database for relationships but also lets you define them/other manually. 
*   Jailer cannot handle tables without primary keys (v2.5.10). The primary key (PK) doesn't need to exist in the DB, you may define it in Jailer's data model but there must be one, i.e. a unique and non-null column. 
*   During DB introspection, Jailer needs to create some tables in the DB. You can have them created in a specific schema other than the user's defult one or, in most DBs, you can have them created as temporary tables. See the FAQ. 

Links: 

*   A good [tutorial][4] that introduces you into the tool in few minutes. 

### Define your own convention for data set names, e.g. per-test-class-data-set

By default DbUnit Express expects you to use dataset `testData/dbunit-test\_data\_set.xml`. However you might for example prefer each test to have its own data set, named for example `<test class name>-test.xml`. 

The easiest solution without repeating yourself is to create a custom subclass of `EmbeddedDbTester` that derives the name to use from the calling class and passes the data set name to the original tester: 

    public class PerTestDataSetEmbeddedDbTester extends EmbeddedDbTester/*Rule*/ {
        
        public PerTestDataSetEmbeddedDbTester() throws DatabaseUnitRuntimeException {
    		super('/' + getCallingClass().replace('.', '/') + "-data.xml");
       }
    
       private static String getCallingClass() {
            final StackTraceElement[] callStack = new Exception().getStackTrace();
    		for (int i = 0; i > callStack.length; i++) {
    			final String clazzName = callStack[i].getClassName();
    			if (!clazzName.equals(PerTestDataSetEmbeddedDbTester.class.getName())) {
    				return clazzName;
    			}
    		}
            throw new IllegalStateException("No calling class on the stack trace?!");
    	}
    }
    

Notice that if the data set cannot be found in the default location, i.e. testData/, then it is searched for on the classpath, so it is perfectly OK to have it next to the test class. 

### Use custom database (f.ex. in-memory) 

By default DbUnit Express uses a file based Derby database. This is extremely useful when your tests fail and you need to see how has the data changed to understand why they fail. However there may be cases where you would prefer to use another database or a pure in-memory database. 

To use an [in-memory Derby][18], set the `connectionUrl` property in the `dbunit-embedded.properties` file described above like this: 

    dbunit.connectionUrl=jdbc:derby:memory:myDb;create=true

Notice that the `DatabaseCreator` does work for the in-memory Derby just fine. 

### DRY: Get rid of setUp by using a parent test class or JUnit Rules

To avoid repeating calls to onSetUp(), the simplest solution is to create a parent class for all your DB tests and call them from there (this works with JUnit4 annotation-based tests too). You can even use it to set your own standard for datafile naming, so it may be a good idea to do it always. 

If you have JUnit 4.9+ there is even a simple solution - use EmbeddedDbTesterRule instead of EmbeddedDbTester, stored in a public field annotated with @Rule: 

    ...
       @Rule public EmbeddedDbTesterRule testDb = new EmbeddedDbTesterRule(); // the rule runs its onSetup automatically
       
       @Test public void testIt() { /* Use it, e.g. testDb.getDataSource() ... */ }
      ...
    

Credits to Xavier Dury for this idea. 

### Use dbunit-express with other data set formats (flat, csv, ...)

As explained above, dbunit-express uses the less error-prone non-flat XML data set format. If you want to use [data sets in a different format][19] it is well possible - just use EmbeddedDbTester[Rule].setDataSet(..) to supply whatever DataSet implementation instance you want. 

It's best combined with a parent class common to all tests or a custom subclass of EmbeddedDbTester[Rule]. You could for example create st. like this: 

    public class CustomEmbeddedDbTester extends EmbeddedDbTesterRule {
           
               private static ddlExecuted = false;
               
               public static ViaEmbeddedDbTester withFlatDataSet(String name) {
                   return new CustomEmbeddedDbTester(name, true);
               
               }
               public static ViaEmbeddedDbTester withXmlDataSet(String name) {
                   return new CustomEmbeddedDbTester(name, false);
               }
               
               private CustomEmbeddedDbTester(String name, boolean flatXml) {
                   InputStream xml = EmbeddedDbTester.findConfigFileStream(name);
                   DataSet ds = flatXml? new FlatXmlDataSetBuilder().build(xml) : new XmlDataSet(xml);
                   super.setDataSet(ds dataSetType);
                   
                   // Init the DB if not done yet (assumes an in-memory DB):
                   if (!ddlExecuted) {
                       new DatabaseCreator().loadDdl("create_db_content.ddl");
                       ddlExecuted = true;
                   }
               }
           
           }
    

And use it as: 

    @Rule public EmbeddedDbTesterRule testDb = CustomEmbeddedDbTester.withFlatDataSet("tax-calculcation-test-ds.xml");
    

(It would be even more clever to derive the type of the data source from the name and guess the name to be st. like &lt;calling test class name&gt;-test.(xml|flatxml|csv|...) but that would be to much code for this short tip :-))

### Disable DbUnit tests when a system property is set

You may want to have the possibility to disable the DB tests, f.ex. if you are using an external database and not all have it installed and configured. One way to do it is to use JUnit's an function in onSetup.

For example:

    public class CustomEmbeddedDbTester extends EmbeddedDbTesterRule {
       
           private static final boolean DISABLED = System.getProperties().containsKey("tests.db.disable");
           
           @Override
           public void onSetup() throws Exception {
              org.junit.Assume.assumeThat(DISABLED, is(false)); // skip the test if disabled
              super.onSetup();
           }
           
           ...
       }

Difference between dbunit-express and DbUnit
--------------------------------------

Dbunit-express is a thin wrapper around DbUnit intended to make starting with DB testing as quick and as easy as possible by introducing

* Convention over configuration - automatically loads (XML) data set file of name derived from the test name if it exists, ...
* Sensible defaults - comes pre-configured for an embedded Derby database
* Convenience methods - such as getDataSource() - extremely useful for testing code using Spring JDBC
* Utilities - RowComparator for easy verification of data in a select, DatabaseCreator for DB initialization from a DDL, replaceDatabase(dataSet), clearTable(name) , JUnit 4 @Rule automatically invoking onSetup(), ...
* Improved error reporting for Derby - some error messages are rather cryptic, I try to provide more understandable ones with advices on how to solve the issues
* The configuration of DbUnit that proved to be most flexible and error-proof - fully qualified table names, XML data set,...
* Easier global configuration that applies to all tests via the configuration file dbunit-express.properites

Similar projects
------------

* [Spring’s Embedded database support](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/jdbc.html#jdbc-embedded-database-support) – automatically create and load an embedded DB (Derby, H2, HSQL). Embedded DB support only.
* [Unitils-dbunit](http://www.unitils.org/tutorial-database.html) - extends DbUnit and introduces annotations for automatic loading of (flat) data set XML derived from the test class name, possibility to load custom data set for a particular method, possibility to compare results with an expected data set etc.

Version history
------------

### NEXT VERSION
- TBD: Merge as much as reasonable of DbUnit Express directly into DbUnit 2.x or 3.0

### VERSION 1.3.0 (Sep 2011)

Cosmetic changes and minor improvements

    (1) Renamed project to dbunit-express (=> changed package to net.jakubholy.dbunitexpress)
    (2) Upgraded to latest DbUnit (2.4.8, was 2.4.7)
    (3) Added slf4j-simple to dependencies so that users don't need to do it - those who don't want it may just exclude it
    (4) When there is a FileNotFoundException for the ddl, report the working directory, check if testData/ self exists
    (5) Added comment to onTearDown() saying that it does nothing and thus doesn't need to be called, updated test classes accordingly
    (6) Added SimpleNonExtendingEmbeddedDbJUnit4Test to show usage with JUnit 4
    (7) Added setDataSet(String) that tries to find a file of the given name in testData/ or on the classpath and loads it
    (8) When it is detected in onSetup() that the test database likely isn't initialized, we advice to use Db Creator and report the current working directory
    (9) Added method findConfigFile(name) to simplify custom DataSet creation
    (10) Added DatabaseCreator.loadDdl(name) to load an additional DDL, you can also use
         new DatabaseCreator().setDdlFile("fileOnClasspathOrInTestData.ddl").doCreateAndInitializeTestDb() to init the DB from a custom DDL
    (11) Added EmbeddedDbTesterRule which can call its onSetup automatically under JUnit 4

Migration from v1.2.0: Rename package from net.jakubholy.testing.derby.embedded to net.jakubholy.dbunitexpress, if you use maven, rename the artifact to dbunit-express.

### VERSION 1.2.0 (Apr 2010)

    A major upgrade release:
    (1) Upgraded to DbUnit 2.4.7
        - includes migration to SLF4j for logging
    (2) Various improvements:
        - RowComparator made fluent - all assert* now return this so that calls can be chained
        - createDataSetFromFile modified to search on the calling class' classpath
            => data set file may be next to a test class
        - replaceDatabase: added logging of duplicated PKs for easier troubleshooting
        - improved JavaDoc, improved logging
        - Improved error reporting, e.g. when the derby db is locked by another process, for failed DB startup
        - Fixed broken DatabaseCreator
        - Added few utility methods to the *TestCase (getSqlConnection) and EmbeddedDbTester (setConn.Prop., resolveConn.Prop., getSqlConnection)
        - Addedd RowComparator.withErrorMessage and, .withOneTimeErrorMessage and more unit tests
        - RowComparator: a custom check other than .equals can be done in the RowComparator's assertNext
            by passing a value of the type ValueChecker
        - RowComparator: added assertNext(.., String[]) to make the possibility of type-insensitive comparison clear + appropriate JavaDoc update

### VERSION 1.1.0 (Feb 2010)

    This is a major reorganization and feature release:
    (1) All functionality moved to EmbeddedDbTester so that extending the AbstractEmbeddedDbTestCase
    	isn't necessary anymore, which is useful e.g. in JUnit 4 or when extending another JUnit
    	derivation. It's also a standard DbUnit's IDatabaseTester.
    (2) It's now possible to change completely the DB used by defining some JDBC
    	properties in dbunit-embedded.properties
    (3) Added utility methods getDataSource, getSqlConnection and a convenience method
    	createCheckerForSelect, some of those implemented in  the EnhancedDatabaseTesterDecorator
    	so that they can be added to any IDatabaseTester.
    (4) Switched from Java util logging to commons-logging for better configurability.
    

### VERSION 1.0.2 (Nov 2009)

    This is a minor feature release:
    (1) Added the method replaceDatabase(IDataSet) to the abstract parent test
    to make it easier to define new data for a method
    (2) Added there the method clearTable(tableName) so that you can delete
    all rows from a particular table and thus test that the tested class can handle
    an empty table
    (3) Updated the sample test case with examples of those new methods
    (4) Added the nested class RowComparator to make checking of the final data easier.
    

### VERSION 1.0.1

    This is a minor feature release, I've only added the method replaceDatabase to the abstract parent test
    to make it possible to replace the content of the test database from a data set file at the
    beginning of a test method that requires other data than the default ones.
    

The latest developer releases (if any) may be found in the snapshot repository, try appending the latest (unreleased) version from the [available versions list][21] to the base repository URL of ttps://oss.sonatype.org/content/repositories/jakubholy-snapshots/net/jakubholy/testing/dbunit-embeddedderby-parenttest/ 

## Appendices

### Using dbunit-express without Maven

Without maven you'll need to add manually the dependencies, check the pom.xml, section  plus transitive dependencies. According to mvn dependency:tree as of version 1.3.0, they're: 

    net.jakubholy.testing:dbunit-express:jar:1.3.0 
    +- org.apache.derby:derby:jar:10.8.1.2:compile 
    +- org.dbunit:dbunit:jar:2.4.8:compile 
    | - commons-collections:commons-collections:jar:3.2.1:compile 
    +- junit:junit:jar:4.9:compile 
    | - org.hamcrest:hamcrest-core:jar:1.1:compile 
    +- org.slf4j:slf4j-api:jar:1.5.6:compile 
    - org.slf4j:slf4j-simple:jar:1.5.6:compile 

### Using custom JUnit version (default: latest between 4.9 and 5)

If you're using Maven and want to use a different JUnit than the one used by default by DbUnit Express, which is 4.9.x, you will likely want to tell Maven to exclude this dependency to avoid conflicts between different JUnit classes. 

Let's see a complete example where we want to use JUnit 4.5 (while also using the full Hamcrest instead of the older and limited core only): 

    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit-dep</artifactId>
        <version>4.5</version>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <artifactId>hamcrest-core</artifactId>
                <groupId>org.hamcrest</groupId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.hamcrest</groupId>
        <artifactId>hamcrest-all</artifactId>
        <version>1.2</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>net.jakubholy.testing</groupId>
        <artifactId>dbunit-express</artifactId>
        <version>1.3.0</version>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <artifactId>junit</artifactId>
                <groupId>junit</groupId>
            </exclusion>
            <exclusion>
                <artifactId>junit</artifactId>
                <groupId>junit-addons</groupId>
            </exclusion>
        </exclusions>
    </dependency>    

 [3]: http://jailer.sourceforge.net/ "http://jailer.sourceforge.net/"
 [4]: http://jailer.sourceforge.net/exporting-data.htm "http://jailer.sourceforge.net/exporting-data.htm"
 [5]: http://quantum.sourceforge.net/ "http://quantum.sourceforge.net/"
 [6]: https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/ExampleJUnit4WithRuleTest.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/ExampleJUnit4WithRuleTest.java"
 [7]: https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/SimpleNonExtendingEmbeddedDbJUnit4Test.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/SimpleNonExtendingEmbeddedDbJUnit4Test.java"
 [8]: https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/SimpleEmbeddedDbTest.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/SimpleEmbeddedDbTest.java"
 [9]: https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/SimpleNonExtendingEmbeddedDbJUnit3Test.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/test/java/net/jakubholy/dbunitexpress/SimpleNonExtendingEmbeddedDbJUnit3Test.java"
 [10]: https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/assertion/RowComparator.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/assertion/RowComparator.java"
 [11]: https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/IEnhancedDatabaseTester.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/IEnhancedDatabaseTester.java"
 [12]: http://repo1.maven.org/maven2/net/jakubholy/testing/dbunit-express/ "http://repo1.maven.org/maven2/net/jakubholy/testing/dbunit-express/"
 [13]: http://db.apache.org/derby/ "http://db.apache.org/derby/"
 [14]: http://weblogs.java.net/blog/2006/06/15/java-db-now-part-suns-jdk "http://weblogs.java.net/blog/2006/06/15/java-db-now-part-suns-jdk"
 [15]: http://download.oracle.com/javadb/10.6.2.1/getstart/getstart-single.html#cgsjavadb "http://download.oracle.com/javadb/10.6.2.1/getstart/getstart-single.html#cgsjavadb"
 [16]: http://db.apache.org/derby/docs/10.6/devguide/cdevdvlp17453.html "http://db.apache.org/derby/docs/10.6/devguide/cdevdvlp17453.html"
 [17]: http://www.h2database.com/html/features.html#comparison "http://www.h2database.com/html/features.html#comparison"
 [18]: http://wiki.apache.org/db-derby/InMemoryBackEndPrimer "http://wiki.apache.org/db-derby/InMemoryBackEndPrimer"
 [19]: http://www.dbunit.org/components.html#dataset "http://www.dbunit.org/components.html#dataset"
 [20]: http://stackoverflow.com/questions/1689242/conditionally-ignoring-tests-in-junit-4|assume* "http://stackoverflow.com/questions/1689242/conditionally-ignoring-tests-in-junit-4|assume*"
 [21]: https://oss.sonatype.org/content/repositories/jakubholy-snapshots/net/jakubholy/testing/dbunit-embeddedderby-parenttest/maven-metadata.xml "https://oss.sonatype.org/content/repositories/jakubholy-snapshots/net/jakubholy/testing/dbunit-embeddedderby-parenttest/maven-metadata.xml"  
 [22]: https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/AbstractEmbeddedDbTestCase.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/AbstractEmbeddedDbTestCase.java"
 [23]: https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/EmbeddedDbTester.java "https://github.com/jakubholynet/dbunit-express/blob/master/src/main/java/net/jakubholy/dbunitexpress/EmbeddedDbTester.java"
 
