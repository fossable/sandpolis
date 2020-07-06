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
package com.sandpolis.core.instance.data;

/**
 * This class is a lightweight wrapper for {@link Document}s that provides
 * important context to consumers of this API.
 *
 * @author cilki
 * @since 6.2.0
 */
public abstract class StateObject {

	public Document document;

	protected StateObject(Document document) {
		this.document = document;
	}

	public Object test(String oid) {

		return null;
	}

	/**
	 * Compute an identifier representative of the identity of the document.
	 *
	 * @return The document tag
	 */
	public abstract int tag();
}
