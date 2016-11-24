Testing
=======

Connect relies on a number of test steps:

 - maven integration tests (unit tests are supported)
 - circleci continuous integration [link](https://circleci.com/gh/emenlu/connect) on commits and pull requests
 - codecov coverage reports [link](https://codecov.io/gh/emenlu/connect) generated with jacoco

In addition to the integration testing and coverage reporting done by circleci developers are also expected to test their changes locally. The maven commands are:

 - run unit tests only: `mvn test`
 - clean `target/`, run unit tests and then integration tests: `mvn clean verify`

During integration testing an included neo4j server will automatically be started. **An error will be thrown if a unit test case tries to access a route that queries the database.** The database is *not* ephemeral, but lives in the `target/` directory. Most tests will create stuff in the database and will run into problems if entries, collections or users already exists. This is why it is recommended to run `clean` before `verify`.

Writing test cases
------------------
The connect backend is tested using the pippo module `pippo-test` ([link](http://www.pippo.ro/doc/testing.html)).

There are some additional modules that are used to facilitate easier testing of expected behaviour.
 - Some API actions will send an email. It is possible to use a mock `MailClient`, such as `Mailbox` to capture and verify mails. 
 - A `URLParser` class helps with link extraction.

Detecting test files
--------------------
A test file is run during unit testing if it matches `*Test.java` or `Test*.java`.

Integration tests are matched with `IT*.java` and `*IT.java`.
