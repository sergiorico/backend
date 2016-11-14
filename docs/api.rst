API
===
This is the specification of the public API.

Status codes are generally:

 - 200: ok
 - 400: something wrong with the request
 - 401: authentication error
 - 404: not found
 - 500: server or database error

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

Graph Taxonomy
~~~~~~~~~~~~~~
.. http:get:: /v1/entry/taxonomy

   Get the combined taxonomy for the whole database.

   .. sourcecode:: js
      
      {
          "ADAPTING": ["YES, SIR"],
          "IMPROVING": ["NO, SIR"]
      }

   :>json array <key>: each key maps to the entities/samples used to classify itself

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

    :statuscode 403: must be member of at least one of the collections that own the entry

Account
-------


.. http:post:: /v1/account/login


.. http:post:: /v1/account/register

Reset password
~~~~~~~~~~~~~~~~~~~~~~
.. http:post:: /v1/account/reset-password

.. http:get:: /v1/account/reset-password

access check 1

Check login status
~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/login

Get collections
~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/collections

Query self
~~~~~~~~~~~~
.. http:get:: /v1/account/self

Logout
~~~~~~~~~~~~~~
.. http:post:: /v1/account/logout

Delete account
~~~~~~~~~~~~~~
.. http:post:: /v1/account/delete

Change password
~~~~~~~~~~~~~~~
.. http:post:: /v1/account/change-password

Get collection invites
~~~~~~~~~~~~~~~~~~~~~~
.. http:get:: /v1/account/invites

.. http:get:: /v1/account/{email}

Collection
----------

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

.. http:get:: /v1/collection/(int:id)/entries

   Query connected entries.

   :param id: collection id
   :type id: int

   .. sourcecode:: js

      [Entry, Entry, ..., Entry]

   :>jsonarr Entry: An `Entry`_ object.

.. http:post:: /v1/collection/

   Create a new collection.

   :string name: the collection's name (doesn't have to be unique).

Only requests with an attached session id, where the user is directly connected to the specified collection, are allowed access to these routes.

.. http:post:: /v1/collection/(int:id)/accept

   Accept an invitation to join a specific collection.

   :param id: collection id
   :type id: int
   
Only requests with an attached session id, where the user is directly connected to the specified collection, are allowed access to these routes.

.. http:post:: /v1/collection/(int:id)/invite

   :param id: collection id
   :type id: int
   
.. http:post:: /v1/collection/(int:id)/leave

   :param id: collection id
   :type id: int
   
.. http:post:: /v1/collection/(int:id)/kick

   :param id: collection id
   :type id: int
   
.. http:post:: /v1/collection/(int:id)/removeEntry

   :param id: collection id
   :type id: int
   
.. http:post:: /v1/collection/(int:id)/addEntry

   :param id: collection id
   :type id: int
   
.. http:get:: /v1/collection/(int:id)/members

   Query connected members.

   :param id: collection id
   :type id: int
   

Admin
-----

Only requests with an attached session id, where user's trust level is Admin, are allowed access to these routes.

.. http:get:: /v1/admin

   Returns 200 if current session user is admin.

.. http:get:: /v1/admin/pending

   Get all pending entries.

   .. sourcecode:: js

      [Entry, Entry, ..., Entry]

   :>jsonarr Entry: An `Entry`_ object.

.. http:post:: /v1/admin/accept-entry

   Accept a pending entry.

   :integer entry: **Required**. ID of entry to accept.

.. http:post:: /v1/admin/reject-entry

   Reject a pending entry.

   :integer entry: **Required**. ID of entry to reject.

.. http:put:: /v1/admin/set-trust

   Set trust level of a specific user.

   :string email: **Required**. Email of user affected user.
   :string trust: **Required**. New trust level (Admin, Verified, User, Registered, Unregistered).

.. http:get:: /v1/admin/users

   Get all users.

   .. sourcecode:: js

      [USERS]

   :>json array [USERS]: An array of `Account`_ objects.

