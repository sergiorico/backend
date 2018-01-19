.. _installing:

Installing
==========

How to setup the server.

Prerequisites
-------------

 - An email account, credentials and server settings.

Compiling
---------

 - `git clone https://github.com/serpconnect/backend`
 - `cd backend`
 - `mvn compile package`
 - The compiled server is now in `target/connect-X.Y.Z.zip`

Deploying
---------

 - Assuming `connect-X.Y.Z.zip` and `application.properties` are in current dir.
 - `unzip connect-X.Y.Z.zip`
 - `cp application.properties connect-X.Y.Z`
 - `cd connect-X.Y.Z`
 - `java -jar connect.jar`
 - The server is now running using the external configuration. If no config. file is present the embedded is used instead.
