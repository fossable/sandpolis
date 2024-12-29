# Server Instance

Every Sandpolis network must include one server instance at minimum. Servers are
responsible for coordinating interactions among instances and persisting data.

## Server API

| URL                                                | Method | Description                             | Request Body              | Response Body                  |
| -------------------------------------------------- | ------ | --------------------------------------- | ------------------------- | ------------------------------ |
| `/v1/agent/{iid}/bootagent/{agent_id}/launch`      | POST   | Reboot into a boot agent                |                           | PostBootagentLaunchResponse    |
| `/v1/agent/{iid}/bootagent/{agent_id}/uninstall`   | POST   | Remove a boot agent from the system     |                           | PostBootagentUninstallResponse |
| `/v1/agent/{iid}/bootagent`                        | GET    | Find installed boot agents              |                           | GetBootagentResponse           |
| `/v1/agent/{iid}/bootagent`                        | POST   | Install a boot agent on the system      | PostBootagentRequest      | PostBootagentResponse          |
| `/v1/agent/{iid}/power`                            | POST   | Modify the power state on an agent      | PostPowerRequest          | PostPowerResponse              |
| `/v1/agent/{iid}/uninstall`                        | POST   | Uninstall the agent                     | PostAgentUninstallRequest | PostAgentUninstallResponse     |
| `/v1/agent/{iid}/update`                           | POST   | Update the agent                        | PostAgentUpdateRequest    | PostAgentUpdateResponse        |
| `/v1/server/{iid}/banner`                          | GET    | Return the server's banner information  |                           | BannerResponse                 |
| `/v1/server/{iid}/groups/{group_id}/deployers`     | POST   | Create a new deployer                   | PostDeployerRequest       | PostDeployerResponse           |
| `/v1/server/{iid}/groups/{group_id}`               | DELETE | Remove an existing empty group          |                           | DeleteGroupResponse            |
| `/v1/server/{iid}/groups/{group_id}`               | GET    | Return details on the group             |                           | GetGroupResponse               |
| `/v1/server/{iid}/groups`                          | GET    | List existing groups                    |                           | GetGroupResponse               |
| `/v1/server/{iid}/groups`                          | POST   | Create a new group                      | PostGroupRequest          | PostGroupResponse              |
| `/v1/server/{iid}/instances/stream`                | POST   | Open a new unidirectional change stream |                           | PostInstanceStreamResponse     |
| `/v1/server/{iid}/instances`                       | DELETE | Delete a document by OID                |                           | DeleteInstanceResponse         |
| `/v1/server/{iid}/instances`                       | GET    | Return a document by OID                |                           | GetInstanceResponse            |
| `/v1/server/{iid}/instances`                       | PUT    | Update a document by OID                |                           | PutInstanceResponse            |
| `/v1/server/{iid}/listeners/stream`                | POST   | Open a new listener change stream       | PostListenerStreamRequest | PostListenerStreamResponse     |
| `/v1/server/{iid}/listeners/{listeners_id}/stream` | POST   | Open a new listener change stream       | PostListenerStreamRequest | PostListenerStreamResponse     |
| `/v1/server/{iid}/listeners/{listeners_id}`        | DELETE | Remove an existing listener             |                           | DeleteListenerResponse         |
| `/v1/server/{iid}/listeners/{listeners_id}`        | GET    | Return details on the listener          |                           | GetListenerResponse            |
| `/v1/server/{iid}/listeners/{listeners_id}`        | PUT    | Update an existing listener             | PutListenerRequest        | PutListenerResponse            |
| `/v1/server/{iid}/listeners`                       | GET    | List existing listeners                 |                           | GetListenerResponse            |
| `/v1/server/{iid}/listeners`                       | POST   | Create a new listener                   | PostListenerRequest       | PostListenerResponse           |
| `/v1/server/{iid}/network/stream`                  | POST   | Open a new network change stream        | PostNetworkStreamRequest  | PostNetworkStreamResponse      |
| `/v1/server/{iid}/network`                         | GET    | List the network table                  | PostNetworkStreamRequest  | PostNetworkStreamResponse      |
| `/v1/server/{iid}/ping`                            | GET    | Send an application-level ping          |                           | GetPingResponse                |
| `/v1/server/{iid}/plugins/{plugin_id}`             | DELETE | Remove an existing plugin               |                           | DeletePluginResponse           |
| `/v1/server/{iid}/plugins/{plugin_id}`             | GET    | Return details on the plugin            |                           | GetPluginResponse              |
| `/v1/server/{iid}/plugins/{plugin_id}`             | PUT    | Update plugin configuration             | PutPluginRequest          | PutPluginResponse              |
| `/v1/server/{iid}/plugins`                         | GET    | List installed plugins                  |                           | GetPluginResponse              |
| `/v1/server/{iid}/plugins`                         | POST   | Install a new plugin                    | PostPluginRequest         | PostPluginResponse             |
| `/v1/server/{iid}/session/renew`                   | POST   | Renew the current session               |                           | PostSessionRenewResponse       |
| `/v1/server/{iid}/session`                         | DELETE | Destroy an existing session             |                           | DeleteSessionResponse          |
| `/v1/server/{iid}/tunnels/stream`                  | POST   | Open a new tunnel change stream         | PostTunnelStreamRequest   | PostTunnelStreamResponse       |
| `/v1/server/{iid}/tunnels/{tunnel_id}/stream`      | POST   | Open a new tunnel change stream         | PostTunnelStreamRequest   | PostTunnelStreamResponse       |
| `/v1/server/{iid}/tunnels/{tunnel_id}`             | DELETE | Destroy an existing tunnel              |                           | DeleteTunnelResponse           |
| `/v1/server/{iid}/tunnels/{tunnel_id}`             | GET    | Return details on an existing tunnel    |                           | GetTunnelResponse              |
| `/v1/server/{iid}/tunnels/{tunnel_id}`             | PUT    | Update an existing tunnel               | PutTunnelRequest          | PutTunnelResponse              |
| `/v1/server/{iid}/tunnels`                         | GET    | List tunnel information                 |                           | GetTunnelResponse              |
| `/v1/server/{iid}/tunnels`                         | POST   | Create a new tunnel                     | PostTunnelRequest         | PostTunnelStreamResponse       |
| `/v1/server/{iid}/users/stream`                    | POST   | Open a new user change stream           |                           | PostUserStreamResponse         |
| `/v1/server/{iid}/users/{user_id}/stream`          | POST   | Open a new user change stream           |                           | PostUserStreamResponse         |
| `/v1/server/{iid}/users/{user_id}`                 | DELETE | Remove an existing user account         |                           | DeleteUserResponse             |
| `/v1/server/{iid}/users/{user_id}`                 | GET    | Return details on the user account      |                           | GetUserResponse                |
| `/v1/server/{iid}/users/{user_id}`                 | PUT    | Update an existing user account         | PutUserRequest            | PutUserResponse                |
| `/v1/server/{iid}/users`                           | GET    | List existing user accounts             |                           | GetUserResponse                |
| `/v1/server/{iid}/users`                           | POST   | Create a new user account               | PostUserRequest           | PostUserResponse               |
| `/v1/server/{iid}/certs/{cert_id}`                 | DELETE | Revoke an existing certificate          |                           | DeleteCertResponse             |
| `/v1/server/{iid}/certs`                           | POST   | Generate a new certificate              | PostCertRequest           | PostCertResponse               |

## Instance Configuration

```py
# com.sandpolis.server
{
  "cluster": [
    String(format="ipv4"),
  ],
  "storage" : {
    "provider" : String(default="ephemeral"), # The database storage provider
    "mongodb" : {
      "host"     : String(), # The address of the mongodb host
      "username" : String(), # The mongodb user's username
      "password" : String(), # The mongodb user's password
    }
  },
  "geolocation" : {
    "service"    : String(values=["ip-api.com", "keycdn.com"], default="ip-api.com"), # The name of the geolocation service to use
    "key"        : String(), # The service API key
    "expiration" : Number(), # The cache timeout in hours
  }
}
```

### Default admin password

The admin password will be randomized and printed in the server log. All clients
are required to force users to change the admin password and setup multi-factor
authentication before proceeding after the first login.

## Connection Blocking

The server will refuse connections from IP addresses on a configurable
block-list or those that trigger the global rate-limiting policy.

IP addresses on a configurable allow-list are exempt from rate-limiting.

## Permissions

All user accounts are subject to a set of permissions controlling what server
operations are authorized. The inital admin user has complete and irrevocable
permissions. By default, additional user accounts are created without
permissions and consequently are allowed to do almost nothing.

### Permissions list

| Permission               | Description                                                |
| ------------------------ | ---------------------------------------------------------- |
| `server.generate`        | Rights to use the generator                                |
| `server.users.list`      | Right to view usernames and permissions of all other users |
| `server.users.create`    | Right to create new users (of lesser or equal permissions) |
| `server.net.view`        | Right to open the network control panel                    |
| `server.listener.create` | Right to create a new listener on the server               |
| `server.listener.list`   | Right to view all listeners on the server                  |
| `server.group.create`    | Right to create a new authentication group on the server   |
| `server.group.list`      | Right to view all authentication groups on the server      |
| `agent.system.power`     | Right to shutdown, reboot, etc the agent                   |

## Agent Groups

Agent groups are sets of agents that share one or more authentication schemes.
Every group has exactly one owner and zero or more (user) members.

### Password Authentication Scheme

The agent may provide a simple password embedded in the agent's configuration.
The server compares the password to each agent group until it finds a match. If
a match is found, the agent is becomes authenticated to the matching agent
group. Otherwise, the connection is closed if more than 5 attempts were made on
that connection.

Password authentication may be upgraded to the certificate authentication scheme
for all subsequent connections.

### Token Authentication Scheme

The agent may provide an 8 character alphanumeric time-based token periodically
generated by the server from an agent group's secret key. Since a user must type
the token in manually, the server will attempt to configure the certificate
authentication scheme for all subsequent connections.

### Certificate Authentication Scheme

The agent may provide an X509 "client" certificate signed by an agent group's
secret key during the initial connection attempt. If the agent certificate was
found to be valid, the connection is automatically authenticated without any
additional message exchanges.

#### Agent Certificate Expiration

The default lifetime for an agent certificate is six months. The following
section implies an agent must connect to a server at least once every 1.5 months
otherwise it loses its ability to authenticate.

#### Agent Certificate Renewal

Once 75% of the lifetime of an agent certificate elapses, the server attempts to
issue a new certificate and installs it on the agent.

## Agent Generators

A `Generator` is a routine which produces some installation artifact according
to the parameters set out in an authentication group. The installation artifact
can then be used to install an agent on a remote system.

### Deployers

On execution, deployers set up the agent base directory according to its
configuration and executes the agent. If the target directory already contains
an installation, the old installation is entirely overwritten.

### Packager

A packager is responsible for creating a deployer binary according to the
parameters set out in an authentication group.

### Distributor

A distributor is responsible for transferring and executing generated deployer
artifacts to remote systems.

#### SSH Distributor

The SSH deployer first determines the remote system type and invokes an
appropriate packager to generate an installer. The installer is then transferred
to the remote host and executed.
