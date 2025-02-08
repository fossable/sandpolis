# Server Instance

Every Sandpolis network must include one server instance at minimum. Servers are
responsible for coordinating interactions among instances and persisting data.

## Server API

| URL                                              | Method | Description                             | Request Body             | Response Body                  |
| ------------------------------------------------ | ------ | --------------------------------------- | ------------------------ | ------------------------------ |
| `/v1/agent/{iid}/bootagent/{agent_id}/launch`    | POST   | Reboot into a boot agent                |                          | PostBootagentLaunchResponse    |
| `/v1/agent/{iid}/bootagent/{agent_id}/uninstall` | POST   | Remove a boot agent from the system     |                          | PostBootagentUninstallResponse |
| `/v1/agent/{iid}/bootagent`                      | GET    | Find installed boot agents              |                          | GetBootagentResponse           |
| `/v1/agent/{iid}/bootagent`                      | POST   | Install a boot agent on the system      | PostBootagentRequest     | PostBootagentResponse          |
| `/v1/server/{iid}/groups/{group_id}/deployers`   | POST   | Create a new deployer                   | PostDeployerRequest      | PostDeployerResponse           |
| `/v1/server/{iid}/groups/{group_id}`             | DELETE | Remove an existing empty group          |                          | DeleteGroupResponse            |
| `/v1/server/{iid}/groups/{group_id}`             | GET    | Return details on the group             |                          | GetGroupResponse               |
| `/v1/server/{iid}/groups`                        | GET    | List existing groups                    |                          | GetGroupResponse               |
| `/v1/server/{iid}/groups`                        | POST   | Create a new group                      | PostGroupRequest         | PostGroupResponse              |
| `/v1/server/{iid}/instances/stream`              | POST   | Open a new unidirectional change stream |                          | PostInstanceStreamResponse     |
| `/v1/server/{iid}/instances`                     | DELETE | Delete a document by OID                |                          | DeleteInstanceResponse         |
| `/v1/server/{iid}/instances`                     | GET    | Return a document by OID                |                          | GetInstanceResponse            |
| `/v1/server/{iid}/instances`                     | PUT    | Update a document by OID                |                          | PutInstanceResponse            |
| `/v1/server/{iid}/network/stream`                | POST   | Open a new network change stream        | PostNetworkStreamRequest | PostNetworkStreamResponse      |
| `/v1/server/{iid}/network`                       | GET    | List the network table                  | PostNetworkStreamRequest | PostNetworkStreamResponse      |
| `/v1/server/{iid}/tunnels/stream`                | POST   | Open a new tunnel change stream         | PostTunnelStreamRequest  | PostTunnelStreamResponse       |
| `/v1/server/{iid}/tunnels/{tunnel_id}/stream`    | POST   | Open a new tunnel change stream         | PostTunnelStreamRequest  | PostTunnelStreamResponse       |
| `/v1/server/{iid}/tunnels/{tunnel_id}`           | DELETE | Destroy an existing tunnel              |                          | DeleteTunnelResponse           |
| `/v1/server/{iid}/tunnels/{tunnel_id}`           | GET    | Return details on an existing tunnel    |                          | GetTunnelResponse              |
| `/v1/server/{iid}/tunnels/{tunnel_id}`           | PUT    | Update an existing tunnel               | PutTunnelRequest         | PutTunnelResponse              |
| `/v1/server/{iid}/tunnels`                       | GET    | List tunnel information                 |                          | GetTunnelResponse              |
| `/v1/server/{iid}/tunnels`                       | POST   | Create a new tunnel                     | PostTunnelRequest        | PostTunnelStreamResponse       |
| `/v1/server/{iid}/certs/{cert_id}`               | DELETE | Revoke an existing certificate          |                          | DeleteCertResponse             |
| `/v1/server/{iid}/certs`                         | POST   | Generate a new certificate              | PostCertRequest          | PostCertResponse               |

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
