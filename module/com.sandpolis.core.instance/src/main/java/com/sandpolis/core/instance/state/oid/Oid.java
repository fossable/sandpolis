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
package com.sandpolis.core.instance.state.oid;

import java.util.Arrays;

/**
 * An {@link Oid} corresponds to an item in a state tree.
 *
 * @param <T> The type of the object with which the OID corresponds
 */
public interface Oid extends Comparable<Oid> {

	/**
	 * Get the components of the OID.
	 *
	 * @return The OID components
	 */
	public int[] value();

	/**
	 * Get the first component of the OID.
	 *
	 * @return The OID's first component
	 */
	public default int first() {
		return value()[0];
	}

	/**
	 * Get the last component of the OID.
	 *
	 * @return The OID's last component
	 */
	public default int last() {
		return value()[size() - 1];
	}

	/**
	 * Get the number of components in the OID.
	 *
	 * @return The OID's length
	 */
	public default int size() {
		return value().length;
	}

	public default boolean isConcrete() {
		for (int i : value())
			if (i == 0)
				return false;
		return true;
	}

	@Override
	public default int compareTo(Oid oid) {
		return Arrays.compare(value(), oid.value());
	}

	public default boolean isChildOf(Oid oid) {
		return Arrays.mismatch(value(), oid.value()) == value().length - 1;
	}

	public Oid parent();

	public Oid resolve(int... tags);

	public Oid head(int length);

	public RelativeOid<?> tail();

	public Oid child(int tag);

	public RelativeOid<?> relativize(Oid oid);
}
