# Filesystem Plugin

The filesystem plugin exposes agent and client filesystems to Sandpolis.

## Mounting

Remote filesystems may be mounted on clients or agents with FUSE. Once
established, the mount is permanent until explicitly closed by the user.

By default, the entire filesystem is mounted, but can be configured to only
expose a particular subtree.

### Agent mount

An agent's filesystem may be mounted to a mountpoint on another agent or on a
client's machine.

### Client mount

A client's filesystem may be mounted to a mountpoint on an agent's machine.

## Permissions list

| Permission        | Description                                     |
| ----------------- | ----------------------------------------------- |
| `agent.fs.mount`  | Rights to mount an agent's filesystem           |
| `agent.fs.read`   | Rights to read an agent's filesystem            |
| `agent.fs.write`  | Rights to write an agent's filesystem           |
| `client.fs.mount` | Rights to mount the current client's filesystem |
| `client.fs.read`  | Rights to read the current client's filesystem  |
| `client.fs.write` | Rights to write the current client's filesystem |
