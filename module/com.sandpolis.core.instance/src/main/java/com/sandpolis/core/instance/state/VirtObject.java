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
package com.sandpolis.core.instance.state;

/**
 * A {@link VirtObject} is a member of the virtual state tree.
 *
 * @since 6.2.0
 */
public abstract class VirtObject {

	public final Document document;

	protected VirtObject(Document document) {
		this.document = document;
	}

	/**
	 * Compute an identifier representative of the identity of the document.
	 *
	 * @return The document tag
	 */
	public abstract int tag();

	/**
	 * Determine whether the object has a valid identity.
	 * 
	 * @return Whether all "identity" attributes are defined
	 */
	public boolean checkIdentity() {
		return true;
	}
}
