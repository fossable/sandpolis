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
package com.sandpolis.core.net.store.network;

import com.sandpolis.core.proto.net.MsgNetwork.LinkType;

/**
 * This class contains information about a link between two arbitrary instances.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class SockLink {

	/**
	 * The link type.
	 */
	private LinkType type;

	public SockLink(LinkType type) {
		this.type = type;
	}

	/**
	 * Get the {@link LinkType}.
	 *
	 * @return The link type
	 */
	public LinkType getType() {
		return type;
	}
}
