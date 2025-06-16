## `sandpolis-realm`

Sandpolis networks are partitioned into _realms_ which provide strong data
separation. Server and client instances can participate in multiple realms
simultaneously while agent instances belong to one realm at a time.

As an example, you can have _work_ and _home_ realms that are completely
isolated (other than running on the same server).

### User membership

Users can become members of a realm with an associated set of permissions.

### Default realm

All users are members of a special realm called _default_.

### Realm authentication

All connections to a server instance must be authenticated with a realm using
clientAuth certificates.
