The backend exposes a public HTTP API.

Status codes are generally:
```
200 - ok | success
400 - request is incorrect / wrongly formatted
401 - authentication error
404 - not found
500 - server error | database error
```

Response structures:
```javascript
entry = {
    
}

user = {
    "email": "user-provided-unique-email",
    "trust": "trust level: admin | user | verified | registered | unregistered",
    "default": "default-collection-id"
}

collection = {}
```

### admin
```
    // All routes below require admin access --> requests will return 401 if user isn't logged in or isn't admin
   
    Check if user has admin privileges:
        GET /v1/admin --> 200 (text): authorization ok

    Get submitted entries pending approval or dismissal:
        GET /v1/admin/pending --> 200 (json): [entry, ..., entry]

    Accept or reject pending entries:
        Requires a form parameter "entry" to identify the entry modify.
        Both endpoints below will return 401 if "entry" isn't specified.

        POST /v1/admin/accept-entry --> 200 (text): ok

        POST /v1/admin/reject-entry --> 200 (text): ok

    Change a user's trust level:
        Requires two form parameters, "email" and "trust".
        Trust can be "admin", "user", "verified", "registered", "unregistered".

        PUT /v1/admin/set-trust
            200 (text): success
            400 (text): invalid email

    Query all users present in the system:
        GET /v1/admin/users --> 200 (json): [user, ..., user]
```
### entry
```
    GET /v1/entry
    GET /v1/entry/taxonomy
    GET /v1/entry/{id}
    GET /v1/entry/{id}/taxonomy
    PUT /v1/entry/{id}
    POST /v1/entry/new
```

### account
```
    POST /v1/account/login
    POST /v1/account/register
    POST /v1/account/reset-password
    GET /v1/account/reset-password
    GET /v1/account/verify

    GET /v1/account/login
    GET /v1/account/collections
    GET /v1/account/self
    POST /v1/account/logout
    POST /v1/account/delete
    POST /v1/account/change-password
    GET /v1/account/invites
    GET /v1/account/{email}
```

### collections
```
    POST /v1/collection/
    GET /v1/collection/{id}/graph
    GET /v1/collection/{id}/stats
    GET /v1/collection/{id}/entries

    POST /v1/collection/{id}/accept

    POST /v1/collection/{id}/invite
    POST /v1/collection/{id}/leave
    POST /v1/collection/{id}/kick
    POST /v1/collection/{id}/removeEntry
    POST /v1/collection/{id}/addEntry
    GET /v1/collection/{id}/members
``` 
