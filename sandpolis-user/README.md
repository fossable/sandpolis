## `sandpolis-user`

Each Sandpolis server maintains a global list of users in its database. When
client instances connect to a server, they must login as a valid user.

## User sessions

Once a user is logged in successfully, a session token (JWT) is returned with a
lifetime of 20 minutes. If a client determines the user is not idle, the token
can be automatically renewed.
