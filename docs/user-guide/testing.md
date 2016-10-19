 - Run unit tests: `mvn test`
 - Run integration tests: `mvn verify`

Unit vs Integration
-------------------
A unit test can access the non-database dependent web server endpoints. An integration test can also access all endpoints.

Writing test cases
------------------
The connect backend is tested using the pippo module `pippo-test` ([link](http://www.pippo.ro/doc/testing.html)).

There are some additional modules that are used to facilitate easier testing of expected behaviour.
 - Some API actions will send an email. It is possible to use a mock `MailClient`, such as `Mailbox` to capture and verify mails. 
 - A `URLParser` class helps with link extraction.

### Unit or Integration test
A test file is run during unit testing if it matches `*Test.java` or `Test*.java`.

Integration tests are matched with `IT*.java` and `*IT.java`.

Integration test phase
---------------
`mvn` creates a neo4j (community) database during the `pre-integration-test` phase. Because of how maven handles tests, the test class names has to contain the letters "IT" (integration test), and can't contain the letters "Test" (unit test). Specifically, unit tests can't access a neo4j database.
