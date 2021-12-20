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
include("core:org.s7s.core.agent")
include("core:org.s7s.core.client")
include("core:org.s7s.core.clientagent")
include("core:org.s7s.core.clientserver")
include("core:org.s7s.core.deployer")
include("core:org.s7s.core.foreign")
include("core:org.s7s.core.foundation")
include("core:org.s7s.core.instance")
include("core:org.s7s.core.server")
include("core:org.s7s.core.serveragent")

include("core:integration:org.s7s.core.integration.apt")
include("core:integration:org.s7s.core.integration.freedesktop")
include("core:integration:org.s7s.core.integration.fuse")
include("core:integration:org.s7s.core.integration.homebrew")
include("core:integration:org.s7s.core.integration.launchd")
include("core:integration:org.s7s.core.integration.linux")
include("core:integration:org.s7s.core.integration.osquery")
include("core:integration:org.s7s.core.integration.pacman")
include("core:integration:org.s7s.core.integration.qcow2")
include("core:integration:org.s7s.core.integration.systemd")
include("core:integration:org.s7s.core.integration.uefi")

// Instance modules
if (file("${rootDir}/instance/org.s7s.instance.agent.java/.git").exists())
	include("instance:org.s7s.instance.agent.java")
if (file("${rootDir}/instance/org.s7s.instance.client.terminal/.git").exists())
	include("instance:org.s7s.instance.client.terminal")
if (file("${rootDir}/instance/org.s7s.instance.client.web/.git").exists()) {
	include("instance:org.s7s.instance.client.web")
	include("instance:org.s7s.instance.client.web:api")
	include("instance:org.s7s.instance.client.web:www")
}
if (file("${rootDir}/instance/org.s7s.instance.client.desktop/.git").exists())
	include("instance:org.s7s.instance.client.desktop")
if (file("${rootDir}/instance/org.s7s.instance.server.java/.git").exists())
	include("instance:org.s7s.instance.server.java")
if (file("${rootDir}/instance/org.s7s.instance.client.ios/.git").exists())
	include("instance:org.s7s.instance.client.ios")
if (file("${rootDir}/instance/org.s7s.instance.agent.rust/.git").exists())
	include("instance:org.s7s.instance.agent.rust")
if (file("${rootDir}/instance/org.s7s.probe.cpp/.git").exists())
	include("instance:org.s7s.probe.cpp")
if (file("${rootDir}/instance/org.s7s.instance.agent.uefi/.git").exists())
	include("instance:org.s7s.instance.agent.uefi")
if (file("${rootDir}/instance/org.s7s.instance.deployer.rust/.git").exists())
	include("instance:org.s7s.instance.deployer.rust")
if (file("${rootDir}/instance/org.s7s.instance.deployer.java/.git").exists())
	include("instance:org.s7s.instance.deployer.java")
if (file("${rootDir}/instance/org.s7s.instance.installer.java/.git").exists())
	include("instance:org.s7s.instance.installer.java")

// Core plugins
if (file("${rootDir}/plugin/org.s7s.plugin.desktop/.git").exists()) {
	include("plugin:org.s7s.plugin.desktop")
	include("plugin:org.s7s.plugin.desktop:agent:java")
	include("plugin:org.s7s.plugin.desktop:client:desktop")
}
if (file("${rootDir}/plugin/org.s7s.plugin.device/.git").exists()) {
	include("plugin:org.s7s.plugin.device")
	include("plugin:org.s7s.plugin.device:agent:java")
}
if (file("${rootDir}/plugin/org.s7s.plugin.filesystem/.git").exists()) {
	include("plugin:org.s7s.plugin.filesystem")
	include("plugin:org.s7s.plugin.filesystem:agent:java")
}
if (file("${rootDir}/plugin/org.s7s.plugin.shell/.git").exists()) {
	include("plugin:org.s7s.plugin.shell")
	include("plugin:org.s7s.plugin.shell:agent:java")
	include("plugin:org.s7s.plugin.shell:client:desktop")
}
if (file("${rootDir}/plugin/org.s7s.plugin.upgrade/.git").exists()) {
	include("plugin:org.s7s.plugin.upgrade")
	include("plugin:org.s7s.plugin.upgrade:agent:java")
}
if (file("${rootDir}/plugin/org.s7s.plugin.snapshot/.git").exists()) {
	include("plugin:org.s7s.plugin.snapshot")
	include("plugin:org.s7s.plugin.snapshot:server:java")
	include("plugin:org.s7s.plugin.snapshot:agent:java")
}
