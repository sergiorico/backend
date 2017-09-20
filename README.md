SERP Connect
============

[![CircleCI](https://circleci.com/gh/emenlu/connect.svg?style=svg)](https://circleci.com/gh/emenlu/connect)

SERP Connect is a project that connects research results in software engineering with challenges the industry have identified using a shared taxonomy. In addition to search & match capabilities this web tool also features collections, possibility to experiment with taxonomy extensions and powerful visualizations which aid presentations and provide overview.

This repository contains the server (backend) of the project. It also contains the documentation hosted on [read-the-docs](http://serpconnect.rtfd.io).

# Get Involved

 - **Contributing**: Pull requests are welcome!
   - Read [`CONTRIBUTING.md`](.github/CONTRIBUTING.md)
   - Submit github issues for features, bugs or documentation problems
 - **Discuss**: Talk to us and others over at [slack](https://serp-group.slack.com)

# Installation

A full explanation on installing connect and the dependencies is included in the [getting-started](http://serpconnect.readthedocs.io/en/latest/dev-docs/getting-started.html) documentation.

```bash
git clone https://github.com/emenlu/connect
cd connect
mvn compile exec:java -Dpippo.mode=DEV
```

### Requirements

 - Apache maven (mvn) [link](https://maven.apache.org/download.cgi)
 - Java SDK (1.8+)
 - neo4j database (2.3.X)
   - ubuntu/debian: `apt-get neo4j=2.3.11` [read-this-first](http://debian.neo4j.org/)
   - tarballs/zip: find 2.3.X [here](https://neo4j.com/download/other-releases/)

### Misc
Default port is `8080`, can be changed in the configuration file (application.properties).

The config file is located: `src/main/resources/conf/application.properties`

Email templates are located: `src/main/resources/conf/messages_en.properties`

We are using some maven plugins(?) to handle testing, execution and packaging:
 - to run (dev,default=prod): `mvn compile exec:java -Dpippo.mode=dev`
 - to clean: `mvn clean` (do this when in doubt)
 - to build: `mvn package`
 - to test: `mvn verify`

Deploying (production mode):
 - `mvn package` --> `target/connect-X.Y.Z.zip`
 - `unzip connect-X.Y.Z`
 - `cd connect-X.Y.Z`
 - `java -jar connect-X-Y-Z.jar`

Got a secret config. file? Just copy it to the same folder as the jar:
 - `cp /from/safehouse/application.properties connect-X.Y.Z`

Neo4j may require some tinkering to get working. You should try to get it running and log into the web interface. This will force you to input a password. **Write username and password into the application.properties file**.

Notes
=====

## Production
Server doesn't support HTTPS, so put it behind nginx or something. HTTPS must be
used to protect the password (sent during login) and session cookie.

Before running `mvn verify` make sure your neo4j server is dead. We have included a version to facilitiate testing during development, which is automatically started and shut down by the `mvn verify` command.

Remember to [read-the-docs](http://serpconnect.readthedocs.io/en/latest/dev-docs/getting-started.html)

## Accounts
Accounts are stored in the neo4j graph, labeled as `:user`. The account identity
the email used to sign up (and later verify account). Passwords are hashed with
scrypt @ standard settings (N = 16384, R = 8, P = 1) and transmitted in plain-text.

There are 4 account levels: Admin, Verified, User, Registered. After sign-up accounts
are considered 'Registered' and users must click the verification link to be upgraded
to 'User' level. An 'Admin' can promote anyone to 'Verified' level, removing approval
requirement for submitted entries. An 'Admin' can also promote anyone to 'Admin' level,
which unlocks entry approval view, all entry editing (edit entry without direct access),
and the user level change view.

### Reset Password
The user who requests a reset password will get a confirmation email with a link
to reset the password. When the link is pressed the user will get redirected
to a form where the user can change his/her password by entering the new password
twice. The user will then be logged in and redirected to the users profile page.

## Entries
There are two types of entries (`:entry`): `:research` and `:challenge`. Entry nodes
contain the properties specific to the entry type. They also have relations to their
entities. The relationship type is the taxonomy classification, e.g `:ASSESSING`.

## Entities
An entity is the free text sample of at least one entry. Many entries can classify
the same entity, even with different classifications.

## Collections
Collections are the main and only way of organizing entries. Each users has a default
collection and can create new collections. All members of a collection can invite new
members. Collections are mainly identified by their ID but also have a human-friendly
name. Armed with a collection ID anyone can get the associated entries, but only
members of the collection can see other members.

Dependencies:
 - (web server) [pippo](https://pippo.ro) [javadoc](http://www.javadoc.io/doc/ro.pippo/pippo-core/0.8.0)
 - (neo4j driver) [jcypher](https://github.com/Wolfgang-Schuetzelhofer/jcypher) [wiki](https://github.com/Wolfgang-Schuetzelhofer/jcypher/wiki)
 - (password hasher) [scrypt](https://github.com/wg/scrypt)
 - (logging framework) [slf4j](https://github.com/qos-ch/slf4j)
 - (testing) [restassured](https://github.com/rest-assured/rest-assured/wiki)
