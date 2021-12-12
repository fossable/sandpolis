//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

rootProject.name = "sandpolis"

// Core modules
include("core:com.sandpolis.core.agent")
include("core:com.sandpolis.core.client")
include("core:com.sandpolis.core.clientagent")
include("core:com.sandpolis.core.clientserver")
include("core:com.sandpolis.core.deployer")
include("core:com.sandpolis.core.foreign")
include("core:com.sandpolis.core.foundation")
include("core:com.sandpolis.core.instance")
include("core:com.sandpolis.core.server")
include("core:com.sandpolis.core.serveragent")

include("core:integration:com.sandpolis.core.integration.apt")
include("core:integration:com.sandpolis.core.integration.freedesktop")
include("core:integration:com.sandpolis.core.integration.fuse")
include("core:integration:com.sandpolis.core.integration.homebrew")
include("core:integration:com.sandpolis.core.integration.launchd")
include("core:integration:com.sandpolis.core.integration.linux")
include("core:integration:com.sandpolis.core.integration.osquery")
include("core:integration:com.sandpolis.core.integration.pacman")
include("core:integration:com.sandpolis.core.integration.qcow2")
include("core:integration:com.sandpolis.core.integration.systemd")
include("core:integration:com.sandpolis.core.integration.uefi")

// Instance modules
if (file("${rootDir}/instance/com.sandpolis.agent.kilo/.git").exists())
	include("instance:com.sandpolis.agent.kilo")
if (file("${rootDir}/instance/com.sandpolis.client.ascetic/.git").exists())
	include("instance:com.sandpolis.client.ascetic")
if (file("${rootDir}/instance/com.sandpolis.client.brightstone/.git").exists()) {
	include("instance:com.sandpolis.client.brightstone")
	include("instance:com.sandpolis.client.brightstone:api")
	include("instance:com.sandpolis.client.brightstone:www")
}
if (file("${rootDir}/instance/com.sandpolis.client.lifegem/.git").exists())
	include("instance:com.sandpolis.client.lifegem")
if (file("${rootDir}/instance/com.sandpolis.server.vanilla/.git").exists())
	include("instance:com.sandpolis.server.vanilla")
if (file("${rootDir}/instance/com.sandpolis.client.lockstone/.git").exists())
	include("instance:com.sandpolis.client.lockstone")
if (file("${rootDir}/instance/com.sandpolis.agent.micro/.git").exists())
	include("instance:com.sandpolis.agent.micro")
if (file("${rootDir}/instance/com.sandpolis.agent.nano/.git").exists())
	include("instance:com.sandpolis.agent.nano")
if (file("${rootDir}/instance/com.sandpolis.agent.boot/.git").exists())
	include("instance:com.sandpolis.agent.boot")
if (file("${rootDir}/instance/com.sandpolis.deployer.rust/.git").exists())
	include("instance:com.sandpolis.deployer.rust")
if (file("${rootDir}/instance/com.sandpolis.deployer.java/.git").exists())
	include("instance:com.sandpolis.deployer.java")
if (file("${rootDir}/instance/com.sandpolis.installer/.git").exists())
	include("instance:com.sandpolis.installer")

// Core plugins
if (file("${rootDir}/plugin/com.sandpolis.plugin.desktop/.git").exists()) {
	include("plugin:com.sandpolis.plugin.desktop")
	include("plugin:com.sandpolis.plugin.desktop:agent:kilo")
	include("plugin:com.sandpolis.plugin.desktop:client:lifegem")
}
if (file("${rootDir}/plugin/com.sandpolis.plugin.device/.git").exists()) {
	include("plugin:com.sandpolis.plugin.device")
	include("plugin:com.sandpolis.plugin.device:agent:kilo")
}
if (file("${rootDir}/plugin/com.sandpolis.plugin.filesystem/.git").exists()) {
	include("plugin:com.sandpolis.plugin.filesystem")
	include("plugin:com.sandpolis.plugin.filesystem:agent:kilo")
}
if (file("${rootDir}/plugin/com.sandpolis.plugin.shell/.git").exists()) {
	include("plugin:com.sandpolis.plugin.shell")
	include("plugin:com.sandpolis.plugin.shell:agent:kilo")
	include("plugin:com.sandpolis.plugin.shell:client:lifegem")
}
if (file("${rootDir}/plugin/com.sandpolis.plugin.osquery/.git").exists()) {
	include("plugin:com.sandpolis.plugin.osquery")
	include("plugin:com.sandpolis.plugin.osquery:agent:kilo")
	include("plugin:com.sandpolis.plugin.osquery:server:vanilla")
}
if (file("${rootDir}/plugin/com.sandpolis.plugin.upgrade/.git").exists()) {
	include("plugin:com.sandpolis.plugin.upgrade")
	include("plugin:com.sandpolis.plugin.upgrade:agent:kilo")
}
if (file("${rootDir}/plugin/com.sandpolis.plugin.snapshot/.git").exists()) {
	include("plugin:com.sandpolis.plugin.snapshot")
	include("plugin:com.sandpolis.plugin.snapshot:server:vanilla")
	include("plugin:com.sandpolis.plugin.snapshot:agent:kilo")
}
