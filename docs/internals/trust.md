All accounts have a trust level that is used throughout the API to determine authorization:

 - Admin (99999): Can do anything, really
 - Verified (9999): Can submit entries directly, bypassing the approval/rejection phase
 - User (999): Can submit entries, but they must be approved/rejected by an admin
 - Registered (99): Can create collections
 - Unregistered (9): ??

A user is initially considered as `registered` until they have verified their email, at
which point they are automatically promoted to `user` status. Only admins can promote
accounts to `verified` or `admin` status (actually, they can set trust level freely).

