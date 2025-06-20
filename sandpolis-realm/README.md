## `sandpolis-realm`

Sandpolis networks are partitioned into _realms_ which provide strong data
separation. Server and client instances can participate in multiple realms
simultaneously while agent instances belong to one realm at a time.

As an example, you can have _work_ and _home_ realms that are completely
isolated (other than running on the same server).

### Realm membership

Users can become members of a realm with an associated set of permissions.

### Default realm

All users are members of a special realm called _default_.

### Realm authentication

All connections to a server instance must be authenticated with a realm using a
TLS certificate.

There are four types of certificate:

#### Realm cluster certificate

Each realm has a single "root" cluster certificate that signs new server,
client, and agent certificates.

#### Realm server certificate

This certificate is used by clients and agents to verify the server is part of
the cluster.

#### Realm client certificate

This certificate is used to authenticate with servers as a client instance. The
server verifies the client certificate was issued by the cluster certificate.

#### Realm agent certificate

Also a clientAuth certificate, but is distinct from regular realm client
certificates so that it's impossible to use an agent cert to authenticate as a
client instance.
