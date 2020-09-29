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
package com.sandpolis.core.foundation.util;

/**
 * This utility simplifies the handling of many types of IDs.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class IDUtil {

	/**
	 * Get a request ID for use in a message that requires a response.
	 *
	 * @return A new request ID
	 */
	public static int rq() {
		return RandUtil.nextInt() << 0x01;
	}

	/**
	 * Generate a new stream ID.
	 *
	 * @return The stream ID
	 */
	public static int stream() {
		return RandUtil.nextInt() | 0x01;
	}

	/**
	 * Generate a new filesystem handle ID.
	 *
	 * @return The FsHandle ID
	 */
	public static int fm() {
		return RandUtil.nextInt();
	}

	/**
	 * Generate a new listener ID.
	 *
	 * @return The listener ID
	 */
	public static int listener() {
		return RandUtil.nextInt();
	}

	/**
	 * Generate a new group ID.
	 *
	 * @return The group ID
	 */
	public static long group() {
		return RandUtil.nextLong();
	}

	private IDUtil() {
	}
}
