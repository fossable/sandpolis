# Deployer

Deployer instances are responsible for installing, updating, or removing agent
and probe instances.

If an existing agent or probe was originally installed by a package manager, it
cannot be updated or removed by a deployer.

## Instance Configuration

```py
{
  "agent_type"   : String(), # The type of agent to install
  "callback"     : {
    "address"    : String(), # The callback address
    "identifier" : String(), # The callback identifier
  },
  "install"      : {
    "install_dir"  : String(), # The installation's base directory
    "autorecover"  : String(), # Whether the agent can disregard elements of the config in case of failure
    "autostart"    : Boolean(), # Whether the agent should be started on boot
  },
  "kilo"         : {
    "modules" : [
      {
        "group"       : String(), # The artifact's maven group identifier
        "artifact"    : String(), # The artifact's identifier
        "filename"    : String(), # The artifact's filename
        "version"     : String(), # The artifact's version string
        "hash"        : String(), # The artifact's SHA256 hash
      }
    ]
  }
}
```

## Callbacks Connections

If the install/update operation fails, and callbacks are configured, the
deployer will establish an encrypted "callback" connection with a server and
transfer details on the error.
