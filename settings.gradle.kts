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
include("module:com.sandpolis.core.agent")
include("module:com.sandpolis.core.client")
include("module:com.sandpolis.core.clientagent")
include("module:com.sandpolis.core.clientserver")
include("module:com.sandpolis.core.foreign")
include("module:com.sandpolis.core.foundation")
include("module:com.sandpolis.core.instance")
include("module:com.sandpolis.core.net")
include("module:com.sandpolis.core.server")
include("module:com.sandpolis.core.serveragent")

// Instance modules
if (file("${rootDir}/com.sandpolis.agent.kilo/.git").exists())
	include("com.sandpolis.agent.kilo")
if (file("${rootDir}/com.sandpolis.client.ascetic/.git").exists())
	include("com.sandpolis.client.ascetic")
if (file("${rootDir}/com.sandpolis.client.lifegem/.git").exists())
	include("com.sandpolis.client.lifegem")
if (file("${rootDir}/com.sandpolis.server.vanilla/.git").exists())
	include("com.sandpolis.server.vanilla")
if (file("${rootDir}/com.sandpolis.client.lockstone/.git").exists())
	include("com.sandpolis.client.lockstone")
if (file("${rootDir}/com.sandpolis.agent.micro/.git").exists())
	include("com.sandpolis.agent.micro")
if (file("${rootDir}/com.sandpolis.agent.nano/.git").exists())
	include("com.sandpolis.agent.nano")
if (file("${rootDir}/com.sandpolis.distagent.rust/.git").exists())
	include("com.sandpolis.distagent.rust")

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
