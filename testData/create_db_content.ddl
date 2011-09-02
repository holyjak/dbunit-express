-- DDL file defining the schemas and tables to create in the test database.
-- Read and executed by net.jakubholy.testing.dbunit.DatabaseCreator#createDbSchemaFromDdl(Connection)
-- see net.jakubholy.testing.dbunit.DatabaseCreator#main.

-- Replace the text below with whatever you need.
create schema my_test_schema;

create table my_test_schema.my_test_table (
	id int primary key
	, some_text varchar(225));