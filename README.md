# upgrades-sql-parser

 This project is used to test new versions of SQL parser https://github.com/JSQLParser/JSqlParser

 The only use case is parsing DDL for Check Constraints SQL statements fragments.

 This is only a fragment extracted from the application where DDL is generated dynamically by ORM and compared with real DM metadata.
 The goal of parser is to identify if the DDL SQL is semantically the same before a new DDL is applied to the database.
