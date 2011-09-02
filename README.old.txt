	#################################
	# README FOR DbUnitTestSkeleton #
	#################################

### ABOUT ###
From pom.xml:
	The project helps you to setup a DbUnit test using an embedded Derby database
	by providing an  AbstractEmbeddedDbTestCase.java that you extend, instructions
	for using it, and the utility DatabaseCreator.java for creating and initializing the DB.

### USE ON A PROJECT ###

There are two types of db unit tests:
	1. Using an external test DB - for this we have no special support here (as of 2009-06-12).
	2. Using an embedded DB initialized from a .xml by DbUnit: see AbstractEmbeddedDbTestCase and
	'How to prepare the embedded DB for testing' below.
	That's what does this project make easier for you.

1. Add dbunit-embeddedderby-parenttest.jar generated from this project to your project including its dependencies.
	a) With maven you just declare dependency on this project, all other depend. are handled for you.
	b) Without maven you'll need to add manually the dependencies, check the pom.xml, section <dependencies>
	   plus transitive dependencies. According to mvn dependency:tree as of 2009-06-12, they're:

		net.jakubholy.testing:dbunit-embeddedderby-parenttest:jar:1.0.0
		+- org.apache.derby:derby:jar:10.3.2.1
		\- org.dbunit:dbunit:jar:2.2
		   +- junit:junit:jar:3.8.2
		   +- junit-addons:junit-addons:jar:1.4
		   |  +- xerces:xercesImpl:jar:2.6.2
		   |  \- xerces:xmlParserAPIs:jar:2.6.2
		   +- poi:poi:jar:2.5.1-final-20040804
		   +- commons-collections:commons-collections:jar:3.1
		   +- commons-lang:commons-lang:jar:2.1
		   \- commons-logging:commons-logging:jar:1.0.4

2. Create the folder testData/ under your project's root folder.
3. Copy DbUnitTestSkeleton/testData/create_db_content.ddl
	into the new testData/ and modify it to fit your data schema needs.
4. (optional) Run DatabaseCreator#main from your project's root folder to create and initialize
	the test DB from the .ddl file.
5. Subclass AbstractEmbeddedDbTestCase and implement your tests there. (See SimpleEmbeddedDbTest.java
	for inspiration.)
	* You may want to override its getDataSet() if you want to use st. else than testData/dbunit-test_data_set.xml.
6. Copy DbUnitTestSkeleton/testData/dbunit-test_data_set.xml
	into your new testData/ and modify it to fit your data needs.
	Any table mentioned in the file (<table ...>) will be emptied prior to running test,
	if it has any rows defined than those will be also inserted.
7. Run the test.

Note about logging
------------------
	I had troubles to get logging to do what I wanted (namely anything else than hibernate).
	The solution was to create src/main/resources/ with commons-logging.properties that defines
	SimpleLog as the backend to use and with simplelog.properties setting default log
	level to debug.
	Note: The test classes use java logging, this logging setup applies to other classes that
	take part in the test, i.e if the module under test uses commons-logging.

	These .properties files are included in the generated jar.

	When running your tests, you'll need to have these .properties on the classpath in front of
	any other possible commons-logging and simplelog configuration. This is best achieved by
	putting the dbunit-embeddedderby-parenttest.jar to the very beginning of the classpath,
	even in front of the project under test itself.

	I had no luck with a similar setup using Log4j. (Likely due to Log4j/commons-logging class loader magic.)

How to prepare the embedded DB for testing
-----------------------------------------
	(I) The simplest is to adjust testData/create_db_content.ddl and run DatabaseCreator#main.

	(II) Another option using IBM Rational Sw Developer (or maybe Eclipse too) is:

		1. Create a new Data Design project.
		2. Define a connection to the test Derby DB in the Data perspective while in the newly created project
		   (see the Db explorer window in the bottom-left): click on Connections -> New conn.,
		   select Derby -> 10.1 (10.3 not yet supported but this works). Specify the path to
		   <PROJECT NAME>/testData/testDB ; if it doesn't exist, it will be created.
		   - driver: org.apache.derby.jdbc.EmbeddedDriver, works with derby-10.3.2.1.jar
		   - login: user sa, password empty ("")
		3. From <PROJECT NAME>/testData/ copy the DDLs into
		   the data design project into its root folder (may be easier e.g. in Navigator or Project Explorer).
		4. Execute the SQL: right-click on a DDL -> Execute SQL, check "Use an existing connection" and
		   select the testDB. This should be w/o any failures. Delete the output log (r-click > delete all).

		Now the DB is ready for running DbUnit tests against it.

		NOTE: You can regenerate the DDL for creating the test DB by using the same Data
		Design project, adding a connection to the original DB, navigating to the schema/table
		in it and right-click -> Generate DDL file. Don't forget to check "generate full names"
		(including schema name).

		In the case of a DB change you'll perhaps also need to update the DTDs referenced by the
		DbUnit XML. (I've actually updated the XML, generated XSD from it and DTD from that).

### BUILDING THE PROJECT ###
$ svn checkout https://jeeutils.svn.sourceforge.net/svnroot/jeeutils/trunk/DbUnitTestSkeleton
$ mvn package
	=> builds target/*.jar; uses Maven 2
$ mvn dependency:sources
	=> downloads also source codes of dbunit and the other dependencies; useful for development.

PS: Tests will likely fail unless you run DatabaseCreator#main to create&init a dummy test db.

### CONNECTING TO THE EMBEDDED DERBY DATABASE ###
You can connect to the embedded Derby database using any standard JDBC tool
if you provide it the driver included in the derby-<Version>.jar.
The connection URL is either
	jdbc:derby:testData/testDB
which specifies a relative path and is thus expected to be run from the project's folder or
 	jdbc:derby:/absolute/path/to/testData/testDB
(replace with the absolute path for you environment, like /C:/myproject/...)

The credentials for the connection are:
	user name: "sa"
	password: "" (an empty string)
