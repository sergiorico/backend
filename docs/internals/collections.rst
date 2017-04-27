.. _collections:

Collections
===========

A collection is a neo4j object with a ``:collection`` label. All users that are members of the collection has a relation of type ``:MEMBER_OF`` pointing to the collection. The user who created the collection is considered the owner and has an additional relationship with type ``:OWNER``. It is possible to invite any email to a collection. 

Inviting existing members
-------------------------
If the email already exists in the database an email will be sent and accept/reject links will be added to user's invitations page.
 
Inviting non-existing members
-----------------------------
If the email doesn't exist in the database an email will be sent to the email asking the owner to create an account. If the 
person creates an account with the invited email within 1 week the user will have a pending invitation to the collection at 
the users pending invitations page. 

This is achieved by creating a temporary user with the email-address and with trust level
unregistered. This temporary user has the pending invite linked to its account. If the user ever registers with that email 
the users account credits will be merged with the temporary account. A periodic thread will run every 12 hours and clean up
unregistered users which are older than one week old to avoid flooding the database (`CleanupUsers.java`).

Invite responses
----------------

When a user accepts or rejects an invitation the user who invited the new user will get an email of which action was taken.
If a temporary user gets deleted it will send a reject email.

Leaving a collection
--------------------
A collection member can leave the collection voluntarily or be kicked by the collection owner.

If a collection owner leaves the collection the collection and all related relations are destroyed.

 - The collection node itself is detached and deleted.
 - Pending invites (relations) to that collection are deleted.
 - Entries in the collection are removed and will be deleted if they no longer are attached to any collection.
 