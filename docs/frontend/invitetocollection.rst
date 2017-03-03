Invite to collection
====================

It is now possible to invite any person to a collection. 

Inviting existing members
-------------------------

If the person is a user that exists in the database an email will be sent with a link to the users pending invitations page.
 
Inviting non-existing members
-----------------------------

If the person doesn't exist in the database an email will be sent to the person asking the user to create an account. If the 
person creates an account with the invited email within 1 week the user will have a pending invitation to the collection at 
the users pending invitations page. This is achieved by creating a temporary user with the email-address and with trust level
unregistered. This temporary user have the pending invite linked to its account. If the user ever registers with that email 
the users account credits will be merged with the temporary account. A periodic thread will run every 12 hours and clean up
unregistered users which are older than one week old to avoid over flooding the database.

Invite responses
----------------

When a user accepts or rejects an invitation the user who invited the new user will get an email of which action was taken.
If a temporary user gets deleted it will send a reject email.