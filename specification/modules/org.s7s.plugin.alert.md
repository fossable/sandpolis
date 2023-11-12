# Alert Plugin

The alert plugin allows various kinds of system events to trigger user notifications.

## Alert Level

Each user has an _alert level_ that determines what alert notifications they will receive.

| Alert Level      | Description |
| ---------------- | ----------- |
| NORMAL           | Indicates normal operations |
| AWAY             | Indicates that the user is away from their computer and any unexpected user activity should be considered suspicious |

## Device Classes

| Device Class     | Description |
| ---------------- | ----------- |
| WORKSTATION      |
| SERVER           |
| EMBEDDED         |

## Messages

| Message          | Sources  | Destinations |
| ---------------- | -------- | ------------ |
| EV_Alert         | `agent`  | `server`     |

### Resource Alerts

### Systemd Alerts

### Other
- Filesystem has been remounted RW