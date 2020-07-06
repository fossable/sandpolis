//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
open module com.sandpolis.core.instance {
	exports com.sandpolis.core.instance.data;
	exports com.sandpolis.core.instance.database;
	exports com.sandpolis.core.instance.net;
	exports com.sandpolis.core.instance.plugin;
	exports com.sandpolis.core.instance.pref;
	exports com.sandpolis.core.instance.profile;
	exports com.sandpolis.core.instance.profile.cmd;
	exports com.sandpolis.core.instance.store;
	exports com.sandpolis.core.instance.store.event;
	exports com.sandpolis.core.instance.thread;
	exports com.sandpolis.core.instance.util;
	exports com.sandpolis.core.instance;

	requires com.google.common;
	requires com.google.protobuf;
	requires java.persistence;
	requires org.slf4j;
	requires com.sandpolis.core.foundation;
	requires java.prefs;
	
	uses com.sandpolis.core.instance.plugin.SandpolisPlugin;
}