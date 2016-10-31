Public API
==========
This is a specification of the public API.


Entries
-------
.. http:get:: /v1/entry

    Fetch all entries and edges in the database.

   .. sourcecode:: js

      {
            "nodes": [ENTRIES],
            "edges": [EDGES]
      }

   :>json array nodes: An array of `Entry`_ objects
   :>json array edges: An array of `Edge`_ objects

Entry
-----
.. http:get:: /v1/entry/{id}

   :arg id: An entry id.

    Retrieve information of a specific entry.

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

Taxonomy
~~~~~~~~
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

