//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.shell.client.lifegem {
	exports org.s7s.plugin.shell.client.lifegem;

	requires javafx.base;
	requires javafx.graphics;
	requires javafx.web;
	requires jdk.jsobject;
	requires org.s7s.instance.client.desktop;
	requires org.s7s.core.instance;
	requires tornadofx;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;

	provides org.s7s.core.instance.plugin.SandpolisPlugin with org.s7s.plugin.shell.client.lifegem.ShellPlugin;
}
