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

import java.util.Objects;

import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.instance.state.Oid.AttributeOid;

/**
 * A {@link VirtObject} is a member of the virtual state tree.
 *
 * @since 6.2.0
 */
public abstract class VirtObject {

	public final STDocument document;

	protected VirtObject(STDocument document) {
		this.document = document;
	}

	/**
	 * Compute an identifier representative of the identity of the document.
	 *
	 * @return The document tag
	 */
	public abstract int tag();

	public <T> STAttribute<T> get(AttributeOid<T> oid) {
		if (Objects.requireNonNull(oid).isChildOf(document.getOid()))
			throw new IllegalArgumentException();

		return document.attribute(oid.last());
	}

	/**
	 * A {@link VirtObject} is valid if all present attributes pass value
	 * restrictions.
	 *
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public ErrorCode valid() {
		return ErrorCode.OK;
	}

	/**
	 * A {@link VirtObject} is complete if all required fields are present.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public ErrorCode complete() {
		return ErrorCode.OK;
	}
}
