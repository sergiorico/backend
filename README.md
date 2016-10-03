SERP connect backend
====================

requires:
 - apache maven [dl](https://maven.apache.org/download.cgi)
 - java sdk ~1.8
 - neo4j >2.6

Using `mvn`:
 - to run (dev): `mvn compile exec:java -Dpippo.mode=dev`
 - to build: `mvn package`
 - to test: `mvn test`

Default port is `8080`

Configure here: `src/main/resources/conf/application.properties`

Edit email templates here: `src/main/resources/conf/messages_en.properties`

Deploying (production mode):
 - `mvn package` --> `target/connect-X.Y.Z.zip`
 - `unzip connect-X.Y.Z`
 - `java -jar connect-X-Y-Z/connect-X-Y-Z.jar`

Got a secret config. file? Just copy it to the same folder as the jar:
 - `cp /from/safehouse/application.properties connect-X.Y.Z`


Overview
========
Server doesn't support HTTPS, so put it behind nginx or something. HTTPS must be
used to protect the password (sent during login) and session cookie.


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

