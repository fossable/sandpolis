//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.filesystem.mount;

import static org.s7s.core.integration.fuse.fuse_lowlevel_h.fuse_session_new;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.s7s.core.integration.fuse.fuse_lowlevel_ops;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;

public class FuseDriver {

	public static void ll_open(MemoryAddress req, long ino, MemoryAddress fi) {

	}

	fuse_lowlevel_ops fuse_ops = new fuse_lowlevel_ops();

	public void test() throws Exception {

		MethodHandle comparHandle = MethodHandles.lookup().findStatic(FuseDriver.class, "ll_open",
				MethodType.methodType(void.class, MemoryAddress.class, long.class, MemoryAddress.class));

		CLinker.getInstance().upcallStub(comparHandle, FunctionDescriptor.ofVoid(C_POINTER, C_LONG, C_POINTER), null);

		var session = fuse_session_new(null, null, 0, null);
	}
}
