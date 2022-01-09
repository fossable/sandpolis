//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

rootProject.name = "sandpolis"

// Core modules
include("core:ext:apt")
include("core:ext:freedesktop")
include("core:ext:fuse")
include("core:ext:homebrew")
include("core:ext:launchd")
include("core:ext:osquery")
include("core:ext:pacman")
include("core:ext:qemu")
include("core:ext:systemd")
include("core:ext:uefi")
include("core:foundation")
include("core:instance")
include("core:protocol")

// Instance modules
include("instance:agent")
include("instance:bootagent")
include("instance:client")
include("instance:deployer")
include("instance:installer")
include("instance:probe")
include("instance:server")

// Core plugins
include("plugin:desktop")
include("plugin:desktop:agent")
include("plugin:desktop:client")
include("plugin:device")
include("plugin:device:agent")
include("plugin:filesystem")
include("plugin:filesystem:agent")
include("plugin:shell")
include("plugin:shell:agent")
include("plugin:shell:client")
include("plugin:snapshot")
include("plugin:snapshot:agent")
include("plugin:snapshot:server")
include("plugin:upgrade")
include("plugin:upgrade:agent")
