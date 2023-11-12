
### RQ_BuildAgent
Request to build an agent for the given group.

| Field | Type | Description |
|-------|------|-------------|
| `group` | `string` | The group ID |
| `generator_options` | `GeneratorOptions` | Options for the generator component |
| `packager_options` | `PackagerOptions` | Options for the packager component |
| `deployment_options` | `DeploymentOptions` | Options for the deployment component |

### RS_BuildAgent
null

| Field | Description |
|-------|-------------|
| BUILD_AGENT_OK | 0 |
| BUILD_AGENT_FAILED | 1 |

### RQ_CreateGroup
null

| Field | Type | Description |
|-------|------|-------------|
| `name` | `string` | The group's name |

### RS_CreateGroup
null

| Field | Description |
|-------|-------------|
| CREATE_GROUP_OK | 0 |
| CREATE_GROUP_FAILED_ACCESS_DENIED | 1 |

### RQ_DeleteGroup
null

| Field | Type | Description |
|-------|------|-------------|

### RS_DeleteGroup
null

| Field | Description |
|-------|-------------|
| DELETE_GROUP_OK | 0 |
| DELETE_GROUP_FAILED_ACCESS_DENIED | 1 |

### RQ_UpdateGroup
null

| Field | Type | Description |
|-------|------|-------------|

### RS_UpdateGroup
null

| Field | Description |
|-------|-------------|
| UPDATE_GROUP_OK | 0 |
| UPDATE_GROUP_FAILED_ACCESS_DENIED | 1 |

### RQ_CreateListener
null

| Field | Type | Description |
|-------|------|-------------|
| `address` | `string` | The listening address |
| `port` | `int32` | The listening port |

### RS_CreateListener
null

| Field | Description |
|-------|-------------|
| CREATE_LISTENER_OK | 0 |
| CREATE_LISTENER_ACCESS_DENIED | 1 |
| CREATE_LISTENER_INVALID_PORT | 2 |

### RQ_DeleteListener
null

| Field | Type | Description |
|-------|------|-------------|

### RS_DeleteListener
null

| Field | Description |
|-------|-------------|
| DELETE_LISTENER_OK | 0 |
| DELETE_LISTENER_ACCESS_DENIED | 1 |

### RQ_UpdateListener
null

| Field | Type | Description |
|-------|------|-------------|

### RS_UpdateListener
null

| Field | Description |
|-------|-------------|
| UPDATE_LISTENER_OK | 0 |
| UPDATE_LISTENER_ACCESS_DENIED | 1 |

### RQ_Login
Request a login from the server

| Field | Type | Description |
|-------|------|-------------|
| `username` | `string` | The login username |
| `password` | `string` | The password |
| `token` | `int32` | Time-based One-Time Password token |

### RS_Login
null

| Field | Description |
|-------|-------------|
| LOGIN_OK | 0 |
| LOGIN_FAILED | 1 |
| LOGIN_FAILED_EXPIRED_USER | 2 |
| LOGIN_INVALID_USERNAME | 3 |
| LOGIN_INVALID_PASSWORD | 4 |
| LOGIN_INVALID_TOKEN | 5 |

### RQ_Logout
Request that the current user be logged out

| Field | Type | Description |
|-------|------|-------------|

### RS_Logout
null

| Field | Description |
|-------|-------------|
| LOGOUT_OK | 0 |

### RQ_ServerBanner
Request for the server's banner

| Field | Type | Description |
|-------|------|-------------|

### RS_ServerBanner
Response bearing the server's banner

| Field | Type | Description |
|-------|------|-------------|
| `maintenance` | `bool` | Maintenance mode indicates that only superusers will be allowed to login |
| `version` | `string` | The 3-field version of the server |
| `message` | `string` | A string to display on the login screen |
| `image` | `bytes` | An image to display on the login screen |

### RQ_CreateUser
null

| Field | Type | Description |
|-------|------|-------------|
| `username` | `string` | The user's immutable username |
| `password` | `string` | The user's password |
| `email` | `string` | null |
| `phone` | `string` | null |
| `expiration` | `int64` | null |

### RS_CreateUser
null

| Field | Description |
|-------|-------------|
| CREATE_USER_OK | 0 |
| CREATE_USER_ACCESS_DENIED | 1 |
| CREATE_USER_INVALID_USERNAME | 2 |
| CREATE_USER_INVALID_PASSWORD | 3 |
| CREATE_USER_INVALID_EMAIL | 4 |
| CREATE_USER_INVALID_PHONE | 5 |

### RQ_DeleteUser
null

| Field | Type | Description |
|-------|------|-------------|
| `username` | `string` | null |

### RS_DeleteUser
null

| Field | Description |
|-------|-------------|
| DELETE_USER_OK | 0 |
| DELETE_USER_ACCESS_DENIED | 1 |

### RQ_UpdateUser
null

| Field | Type | Description |
|-------|------|-------------|

### RS_UpdateUser
null

| Field | Description |
|-------|-------------|
| UPDATE_USER_OK | 0 |
| UPDATE_USER_ACCESS_DENIED | 1 |
