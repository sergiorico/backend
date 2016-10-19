Some notes on session management:

- A session is mainly a cookie (`JSESSIONID`) that has some data on the server
- Data on server is so far only "email" _or_ "resetemail" - used for authentication
- Server uses in-memory session store (sessions are destroyed on reboot/update)

Pippo handles all session stuff (store, creation and destruction), and we only interact
with the session by invoking `rc.setSession("..", "..")` or `rc.getSession("...")`.
