.. _performance:

Performance
===========

Benchmarks for performance regressions are yet to be written. The current performance is ok for a modest number of users. As the overhead incurred by `jcypher`_ is very big it is a worthwhile investment to keep the number of API requests as low as possible.

Frontend
--------

The performance of the website is largely determined by database size. Especially home, explore and search pages are sensitive to database size. All pages that fire many small requests will benefit from a faster neo4j driver. Other pages could benefit from moving computation or filterting from the client to the server (e.g. search, graph generation).

Backend
-------

The backend has been profiled during a number of requests to different endpoints and the results all point to the neo4j driver `jcypher`_. Both the driver itself and its dependencies add a big (~100-600ms) overhead in request processing, i.e. time spent before the request hits the wire.

An example:

Finding a user by an email address is a common operation. The cypher query: ``MATCH (u:user {email: {addr}}) RETURN u``. The test was carried out against an endpoint that only did this query. The timings below are the amount of milliseconds spent to perform the database query.

 - jcypher/java: ``11ms``
 - node-neo4j/nodejs: ``3ms``


..  _jcypher: http://jcypher.iot-solutions.net/