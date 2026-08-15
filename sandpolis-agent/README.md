## `sandpolis-agent`

The agent instance runs continuously on devices in the Sandpolis network.

This subsystem also deploys them: a client hands a server SSH credentials for a
target host, and the server installs the agent there — uploading a binary,
writing a realm cert, and installing a systemd unit. A host that already
has an agent only gets its realm cert rewritten.
