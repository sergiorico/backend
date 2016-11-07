API
===
This is the specification of the public API.

Status codes are generally:

 - 200: ok
 - 400: something wrong with the request
 - 401: authentication error
 - 404: not found
 - 500: server or database error

Entry graph
-----------
.. http:get:: /v1/entry

    Fetch all entries and edges in the database.

   .. sourcecode:: js

      {
            "nodes": [ENTRIES],
            "edges": [EDGES]
      }

   :>json array nodes: An array of `Entry`_ objects
   :>json array edges: An array of `Edge`_ objects

Taxonomy
--------
.. http:get:: /v1/entry/taxonomy

    Get the combined taxonomy for the whole database.

   .. sourcecode:: js
      
      {
          "ADAPTING": ["YES, SIR"],
          "IMPROVING": ["NO, SIR"]
      }

   :>json array <key>: each key maps to the entities/samples used to classify itself

Entry
-----
.. http:get:: /v1/entry/(int:entry_id)

    Retrieve information of a specific entry.

    **Example request**:

    .. sourcecode:: http

        GET /v1/entry/55 HTTP/1.1
        Host: api.serpconnect.cs.lth.se
        Accept: application/json

   **Example response**:

   .. sourcecode:: http

      HTTP/1.1 200 ok
      Vary: Accept
      Content-Type: application/json

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

   :statuscode 200: ok
   :statuscode 404: no entry with that id exists at the moment (it might have existed but was deleted)

.. http:get:: /v1/entry/{id}/taxonomy

   :arg id: An entry id.

    Retrieve the taxonomy of a specific entry.

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


.. http:post:: /v1/entry/new

    :<json string entryType: 

    Submit new entry.

    .. sourcecode:: js

        { response }

    

Edit Entry
~~~~~~~~~~
.. http:put:: /v1/entry/{id}

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
.. http:post:: /v1/collection/

.. http:get:: /v1/collection/{id}/graph

.. http:get:: /v1/collection/{id}/stats

.. http:get:: /v1/collection/{id}/entries

access check 1

.. http:post:: /v1/collection/{id}/accept

access check 2

.. http:post:: /v1/collection/{id}/invite

.. http:post:: /v1/collection/{id}/leave

.. http:post:: /v1/collection/{id}/kick

.. http:post:: /v1/collection/{id}/removeEntry

.. http:post:: /v1/collection/{id}/addEntry

.. http:get:: /v1/collection/{id}/members


Admin
-----
.. http:get:: /v1/admin

    Returns 200 if current session user is admin.

.. http:get:: /v1/admin/pending

    Get all pending entries.

   .. sourcecode:: js

      [ENTRIES]

   :>json array []: An array of `Entry`_ objects.


.. http:post:: /v1/admin/accept-entry

    :integer entry: **Required**. ID of entry to accept.

.. http:post:: /v1/admin/reject-entry

    :integer entry: **Required**. ID of entry to reject.

.. http:put:: /v1/admin/set-trust

    :string email: **Required**. Email of user affected user.
    :string trust: **Required**. New trust level (Admin, Verified, User, Registered, Unregistered).

.. http:get:: /v1/admin/users

    Get all users.

   .. sourcecode:: js

      [USER]

   :> json array []: An array of `Account`_ objects.

