API
===
This is the specification of the public API.

Status codes are generally:

 - 200: ok
 - 400: something wrong with the request
 - 401: authentication error
 - 404: not found
 - 500: server or database error

If an endpoint has parameters they are required for the request to success
(otherwise a 400 is thrown). A parameter not found in the URL should be
sent in the request body as ``application/x-www-form-urlencoded``. Some
endpoints require input as JSON. The endpoint description will include a
special JSON Request object if JSON is required.

Project
-------
A project specifies the default taxonomy which collections can extend. Usually each project is hosted on a different website.

.. sourcecode:: js

   {
      "name": "serp",
      "link": "http://serpconnect.cs.lth.se"
   }

Where ``name`` is a unique (across backends) project name and ``link`` a url to the website of the project.

Query projects
~~~~~~~~~~~~~~
.. http:get:: /v1/project

   Get a list of all known projects.

   **Example response**:

   .. sourcecode:: js

      {
          "projects": [PROJECT]
      }

   :>json array projects: An array of `Project`_ objects.

   :statuscode 200: ok, return taxonomy

Create new project
~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/project

   Create a new project listing.

   :param name: unique, alphanumeric name ([a-zA-Z0-9])
   :type name: string
   :param link: url to website of the project
   :type link: string

   :resheader Content-Type: application/json

   **Example response**:

   .. sourcecode:: js

      {
          "name": "serp-test",
          "link": "http://test.serpconnect.cs.lth.se"
      }

   :>json string name: the name you provided
   :>json string link: the link you provided

   :statuscode 200: ok, echo back the project details
   :statuscode 400: name/link missing or incorrect name/already taken
   :statuscode 401: must be logged in
   :statuscode 403: only verified users can create projects

Query project taxonomy
~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/project/(string:name)/taxonomy

   Get a flattened version of the project taxonomy. The flattened graph assumes an implicit "ROOT" node object as the top parent.

   :param name: unique, alphanumeric project name
   :type name: String

   .. sourcecode:: js

      {
          "version": 0,
          "taxonomy": [FACETS]
      }

   :>json integer version: A version identifier.
   :>json array taxonomy: An array of `Facet`_ objects. The flattened taxonomy.

   :statuscode 200: ok, return taxonomy
   :statuscode 404: project not found

Update project taxonomy
~~~~~~~~~~~~~~~~~~~~~~~
.. http:put:: /v1/project/(string:name)/taxonomy

   Update the extended taxonomy.  The request will only pass if
   the version is >= (greater than or equal to) the currently
   stored version.

   :param name: project name
   :type name: string

   .. sourcecode:: js

      {
         "version": 0,
         "taxonomy": [FACETS]
      }

   :<json integer version: Reference to the version this extension is based on.
   :<json array taxonomy: The `Facet`_ nodes of the extended taxonomy.

   :statuscode 400: illegal json, out of date version
   :statuscode 401: must be logged in
   :statuscode 403: must be a admin or creator of project project
   :statuscode 404: no project with that name exists

Graph
-----
A graph consists of entries and edges.

.. http:get:: /v1/entry

   Fetch all entries and edges in the database.

   .. sourcecode:: js

      {
            "nodes": [ENTRIES],
            "edges": [EDGES]
      }

   :>json array nodes: An array of `Entry`_ objects
   :>json array edges: An array of `Edge`_ objects

   :statuscode 200: ok, return graph

Graph Taxonomy
~~~~~~~~~~~~~~
.. http:get:: /v1/entry/taxonomy

   Get a flattened version of the standard SERP taxonomy. The flattened graph assumes an implicit "ROOT" node object as the top parent.

   .. sourcecode:: js

      {
          "version": 0,
          "taxonomy": [FACETS]
      }

   :>json integer version: A version identifier.
   :>json array taxonomy: An array of `Facet`_ objects. The flattened taxonomy.

   :statuscode 200: ok, return taxonomy

Facet
-----
A node in the taxonomy tree is called a facet.

.. sourcecode:: js

   {
      "id": "PLANNING",
      "name": "Test planning",
      "parent": "SCOPE"
   }

Where ``id`` is a (per-taxonomy) unique identifier of this facet,
``name`` is a descriptive name and ``parent`` is the ``id`` of
the parent node (since a taxonomy is a tree).

Edge
----
An edge looks like this:

.. sourcecode:: js

   {
       "source": 9,
       "target": 13,
       "type": "PLANNING"
   }

Where ``source`` is the origin entry node id, ``target`` is the targeted entity node id and ``type`` the (SERP) classification of this relation.

Entry
-----
An entry is either a classified challenge or research result that a user
submitted to the database. Each entry consists of entry-specific information
and a classification. These two pieces of data must be queried separately.
See `Find entry by id`_ and `Get entry taxonomy`_.

Find entry by id
~~~~~~~~~~~~~~~~
.. http:get:: /v1/entry/(int:entry_id)

   Retrieve information of an entry specified by `entry_id`.

   :param entry_id: entry's unique id
   :type entry_id: int
   :resheader Content-Type: application/json

   .. sourcecode:: js

      {
          "id": 55,
          "hash": "YOnPVli1utklw1a3LXiw9pBl6gmpsd4BUabV9I1UyhA=",
          "type": "research",
          "contact": "space_monkey@planet.zoo",
          "reference": "An In-Depth study of the Space Monkey Phenomenon",
          "doi": "doi:xyz",
          "description": null,
          "date": null,
          "pending": false
      }

   :>json integer id: a (recycled) unique id
   :>json string hash: unique hash of this information
   :>json string type: challenge or research
   :>json string contact: not used
   :>json string reference: only valid for research type entries, lists relevant references
   :>json string doi: only valid for research type entries, optional, the DOI of a related paper
   :>json string date: currently broken, a standard javascript date
   :>json boolean pending: is entry pending admin approval

   :statuscode 200: ok, return information
   :statuscode 400: entry_id must be an int
   :statuscode 404: no entry with that id exists at the moment (it might have existed but was deleted)

Get entry taxonomy
~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/entry/(int:entry_id)/taxonomy

   Retrieve the taxonomy of a specific entry.

   :param entry_id: entry's unique id
   :type entry_id: int

   :resheader Content-Type: application/json


   .. sourcecode:: js

      {
          "INFORMATION": [
              "No data currently collected"
          ],
          "SOLVING": [
              "unspecified"
          ],
          "PLANNING": [
              "testing environment trade-off (simulated, real system production)",
              "testing phase trade-off",
              "testing-level trade-off (function, interaction)",
              "automation trade-off"
          ]
      }

   :>json array <key>: each key corresponds to a classification with entities

   :statuscode 200: ok, return entry taxonomy
   :statuscode 400: entry_id must be an int
   :statuscode 404: no entry with that id exists at the moment (it might have existed but was deleted)

Submit new entry
~~~~~~~~~~~~~~~~
.. http:post:: /v1/entry/new

   Submit a new entry.


   :<json string entryType: either ``challenge`` or ``research``
   :<json int collection: unique id of collection to add entry to
   :<json string reference: only required for research entries, a list of references
   :<json string doi: optional for research entries, a DOI of this publication
   :<json string description: only required for challenge entries, describing the challenge
   :<json json serpClassification: the SERP classification
   :<json string date: javascript date text representation

   **Example request json**:

    .. sourcecode:: js

        {
            "entryType": "challenge",
            "collection": 2,
            "description": "how to do software dev without cookies?",
            "date": "Mon Sep 28 1998 14:36:22 GMT-0700 (PDT)",
            "serpClassification": {
                "IMPROVING": ["cookies for software dev"],
                "INFORMATION": ["hungry hungry devs"]
            }
        }

   **Example response**:

    .. sourcecode:: js

       {
           "message": "ok"
       }

   :statuscode 400: bad request
   :statuscode 401: must be logged in to submit new entries
   :statuscode 403: must have verified email addr before submitting entries, must be member of collection

Edit existing entry
~~~~~~~~~~~~~~~~~~~
.. http:put:: /v1/entry/(int:entry_id)

    Edit taxonomy and/or fields of an existing entry. Request is same as `Submit new entry`_, but without a ``collection`` field.

    :param entry_id: unique id of entry
    :type entry_id: int

    **Example request**:

    .. sourcecode:: js

        {
            entryType: "challenge",
            description: "how to do software dev without cookies?",
            date: "Mon Sep 28 1998 14:36:22 GMT-0700 (PDT)",
            serpClassification: {
                "IMPROVING": ["cookies for software dev"],
                "INFORMATION": ["hungry hungry devs"]
            }
        }

   :statuscode 400: entry_id must be an int
   :statuscode 403: must be member of at least one of the collections that own the entry

Account
-------

Authenticate
~~~~~~~~~~~~

.. http:post:: /v1/account/login

   Authenticate user.

   :statuscode 200: ok, user is logged in on the returned session token
   :statuscode 400: email/passw combination is invalid

Register an account
~~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/account/register

   Register new user.

   :statuscode 200: ok, registration email has been sent
   :statuscode 400: email is already registered

Reset password
~~~~~~~~~~~~~~~~~~~~~~
The password reset process is simple:

 * User clicks 'reset my password' and enters email
 * Email is sent to the email address (1)
 * User clicks on link in received email
 * Backend checks token in url, sets session flag and forwards to frontend
 * User enters new password and submits new password
 * User is now logged in and the old password has been replaced

.. http:post:: /v1/account/reset-password

   Send a password reset request. Matches (1) in the description above.

   :statuscode 200: ok

.. http:get:: /v1/account/reset-password?(string:token)

   Consume the reset token and return a new, flagged, session id. Forwards to frontend.

   :param token: a querystring value of the reset token found in the email
   :type token: string

   :statuscode 302: ok, forwarding to frontend
   :statuscode 400: invalid password reset token

Only requests with an attached session id that is considered authenticated (i.e. after `Authenticate`_) are allowed access to routes below.

Check login status
~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/login

   Test if session is authenticated/user is logged in.

   :statuscode 200: ok logged in
   :statuscode 401: no not logged in

Get friends of a user
~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/friends

   :param email: entry's unique email
   :type email: String

   .. sourcecode:: js

   	  ["turtle@rock.gov", "zebra@afri.ca"]

   :>json array emails: an array of emails related to the users email including the users email.

Get collections
~~~~~~~~~~~~~~~
.. http:get:: /v1/account/collections

   Query a list of collections that the currently authenticated user is a member of.

   :param project: include only collections in this project
   :type project: String

   :resheader Content-Type: application/json

   .. sourcecode:: js

      [ { "name": "rick's best systems", "id": 2 } ]

   :>jsonarr name: non-unique name of the collection
   :>jsonarr id: unique id of the collection

Query self
~~~~~~~~~~
.. http:get:: /v1/account/self

   Get an at-a-glance snapshot of stats and data about the current user.

   :resheader Content-Type: application/json

   .. sourcecode:: js

      {
         "email": "zoo@world.gov",
         "trust": "Admin",
         "collections": [COLLECTIONS]
         "entries": [ENTRIES]
      }

   :>json string email: user's email
   :>json string trust: trust level (see :ref:`trust`)
   :>json array collections: An array of collection objects, equivalent to `Get collections`_
   :>json array entries: An array of approved/pending `Entry`_ objects this user has submitted.

Logout
~~~~~~
.. http:post:: /v1/account/logout

   Logout this user and reset the session.

   :statuscode 200: ok

Delete account
~~~~~~~~~~~~~~
.. http:post:: /v1/account/delete

   **WARNING** - Delete the currently authenticated user.

Change password
~~~~~~~~~~~~~~~
.. http:post:: /v1/account/change-password

   Change authentication password. Does not require subsequent requests to re-authenticate.

   :<json string old: old password
   :<json string new: new password

   :statuscode 200: ok
   :statuscode 400: wrong old password

Get collection invites
~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/invites

   Query list of collections have user is invited to. Return equivalent to `Get collections`_.

Query user by email
~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/(string:email)

   Perform `Query self`_ but target a specific user. Returns same output.

   :param email: email of user
   :type email: string

   :statuscode 200: ok
   :statuscode 400: invalid email

Collection
----------

Create new collection
~~~~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/collection/

   Create a new collection.

   :param name: the collection's name (doesn't have to be unique).
   :type name: string

   :statuscode 400: must provide name
   :statuscode 401: must be logged in to create new collections

Get collection graph
~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/graph

   Query the node graph of entries and entities.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      {
         "nodes": [ENTRIES],
         "edges": [EDGES]
      }

   :>json array nodes: An array of `Entry`_ objects.
   :>json array edges: An array of `Edge`_ objects.

   :statuscode 400: id must be an integer
   :statuscode 404: no collection with that id exists

Get statistics
~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/stats

   Query number of members and entries in this collection.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      {
          "members": 2,
          "entries": 9
      }

   :>json int members: number of users, excluding invited, that connected to this collection
   :>json int entries: number of entries that are connected to this collection

   :statuscode 400: id must be an integer
   :statuscode 404: no collection with that id exists

Get collection project
~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/project

   Query the project this collection extends.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      {
         "name": "serp",
         "link": "http://serpconnect.cs.lth.se"
      }

   :statuscode 400: id must be an integer
   :statuscode 404: no collection with that id exists

Get entries
~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/entries

   Query entries in this collection.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      [Entry, Entry, ..., Entry]

   :>jsonarr Entry: An `Entry`_ object.

   :statuscode 400: must provide id, id must be an integer
   :statuscode 404: no collection with that id exists

Only requests with an attached session id, where the user is directly connected to the specified collection, are allowed access to these routes.

Accept an invite
~~~~~~~~~~~~~~~~
.. http:post:: /v1/collection/(int:id)/accept

   Accept an invitation to join a specific collection.

   :param id: collection id
   :type id: int

   :statuscode 400: must provide id, id must be an integer, must be invited to that exception
   :statuscode 404: no collection with that id exists

Only requests with an attached session id, where the user is directly connected to the specified collection, are allowed access to these routes.

Send an invite
~~~~~~~~~~~~~~
.. http:post:: /v1/collection/(int:id)/invite

   Invite a user to a collection.

   :param id: collection id
   :type id: int

   :<json string name: name of the collection

   :statuscode 400: must provide id, id must be an integer
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Leave a collection
~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/collection/(int:id)/leave

   Leave the collection.

   :param id: collection id
   :type id: int

   :statuscode 400: must provide id, id must be an integer
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Remove an entry
~~~~~~~~~~~~~~~
.. http:post:: /v1/collection/(int:id)/removeEntry

   Remove an entry from the collection. If the entry isn't included
   in any other collections it is removed.

   :param id: collection id
   :type id: int

   :<json int entryId: id of entry to remove

   :statuscode 400: must provide id, id must be an integer
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Add an existing entry
~~~~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/collection/(int:id)/addEntry

   Add an existing entry to the collection. This will copy the specified
   entry. The classifications where the facet exists in both taxonomies are copied.

   :param id: collection id
   :type id: int

   :<json int entryId: id of entry to add

   :statuscode 400: must provide id, id must be an integer
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Get members of a collection
~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/members

   Query members in this collection.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      [User, ..., User]

   :>jsonarr User: An `Account`_ object.

   :statuscode 400: must provide id, id must be an integer
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Get the extended taxonomy
~~~~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/taxonomy

   Query the extended taxonomy of this collection. `Facet`_ objects
   returned by this query will reference the standard serp taxonomy,
   which must be queried separately.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      {
         "version": 0,
         "taxonomy": [FACETS]
      }

   :>json integer version: Version identifier. Important for updating the taxonomy.
   :>json array taxonomy: The `Facet`_ nodes of the extended taxonomy.

   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Update the extended taxonomy
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. http:put:: /v1/collection/(int:id)/taxonomy

   Update the extended taxonomy.  The request will only pass if
   the version is >= (greater than or equal to) the currently
   stored version.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      {
         "version": 0,
         "taxonomy": [FACETS]
      }

   :<json integer version: Reference to the version this extension is based on.
   :<json array taxonomy: The `Facet`_ nodes of the extended taxonomy.

   :statuscode 400: illegal json, out of date version
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Reclassify some entities
~~~~~~~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/collection/(int:id)/reclassify

   Replace old facets with new facets for some entities.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      {
         "oldFacetId": "PEOPLE",
         "newFacetId": "STRANGE-PEOPLE",
         "entities": [213, 255]
      }

   :<json string oldFacetId: The facet id that is to be replaced.
   :<json string newFacetId: The replacement facet id.
   :<json array entity: ids of the entities that are to be reclassified.

   :statuscode 400: illegal json
   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Get all the entities
~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/entities

   Get all the entities.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      [
         {
            "id": 222,
            "text": "Regression testing"
         }
      ]

   :>jsonarr id: id of the entity
   :>jsonarr text: user text of the entity

   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Query the classification
~~~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/collection/(int:id)/classification

   Get all the entities grouped by taxonomy facet.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      [
         {
            "facetId": "PEOPLE",
            "text": ["Shifty chimpanzees", "Rectangular red birds"]
         }
      ]

   :>jsonarr facetId: id of the `Facet`_
   :>jsonarr text: text of the entities classified with this facet

   :statuscode 401: must be logged in
   :statuscode 403: must be a member of the collection
   :statuscode 404: no collection with that id exists

Admin
-----

Only requests with an attached session id, where user's trust level is Admin, are allowed access to these routes.

.. http:get:: /v1/admin

   Check if current user (via session token) is an admin.

   :statuscode 200: user is an admin
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin

.. http:get:: /v1/admin/pending

   Get all pending entries.

   .. sourcecode:: js

      [Entry, Entry, ..., Entry]

   :>jsonarr Entry: An `Entry`_ object.

   :statuscode 200: ok, return pending entries
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin

.. http:get:: /v1/admin/collections

   Get all collections that the admin is NOT member of

   .. sourcecode:: js

      [Collection, Collection, ..., Collection]

   :>jsonarr Collection: A `Collection`_ object.

   :statuscode 200: ok, return collections
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin

.. http:post:: /v1/admin/delete-collection

   Delete a collection

   :param entry: ID of collection to delete.
   :type entry: int

   :statuscode 200: ok, collection got deleted
   :statuscode 400: entry is not an int
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin
   :statuscode 404: no such collection exists

.. http:get:: /v1/admin/collections-owned-by

   Return names of all collections user is owner of

   :param email: email of the user

   :statuscode 200: ok, return collections
   :statuscode 400: no email was given
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin


.. http:post:: /v1/admin/accept-entry

   Accept a pending entry.

   :param entry: ID of entry to accept.
   :type entry: int

   :statuscode 200: ok, entry is approved
   :statuscode 400: entry is not an int
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin
   :statuscode 404: no such entry exists

.. http:post:: /v1/admin/reject-entry

   Reject a pending entry.

   :param entry: ID of entry to reject.
   :type entry: int

   :statuscode 200: ok, entry is rejected
   :statuscode 400: entry is not an int
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin
   :statuscode 404: no such entry exists

.. http:post:: /v1/admin/delete-user

   Delete a user with a given email

   :param email: email of the user to be deleted

   :statuscode 200: ok, user got deleted
   :statuscode 400: no email was given
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin

.. http:post:: /v1/admin/delete-entry

   Delete entry with a given entry id

   :param entryId: id of the entry

   :statuscode 200: ok, entry got deleted
   :statuscode 400: entry is not an int
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin
   :statuscode 404: no such entry exists

.. http:put:: /v1/admin/set-trust

   Set trust level of a specific user.

   :param email: Email of user affected user.
   :type email: string

   :param trust: New trust level (Admin, Verified, User, Registered, Unregistered).
   :type trust: string

   :statuscode 200: ok, user has new trust level
   :statuscode 400: invalid trust level, must provide email, must provide trust, no such user exists
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin

.. http:get:: /v1/admin/users

   Get all users.

   .. sourcecode:: js

      [User, User, ..., User]

   :>jsonarr User: An `Account`_ object.

   :statuscode 200: ok, return users
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin

.. http:get:: v1/admin/is-collection-owner

	:param id: id of the collection
	:type id: int

	Return true if the admin is owner of the collection

   :statuscode 200: ok, return boolean
   :statuscode 401: user is not logged in
   :statuscode 403: user is not an admin