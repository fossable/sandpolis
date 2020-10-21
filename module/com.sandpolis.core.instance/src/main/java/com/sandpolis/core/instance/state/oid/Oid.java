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
import java.util.Iterator;

/**
 * <p>
 * An {@link Oid} corresponds to one or more objects in a real or virtual state
 * tree. OIDs locate objects in the state tree with an immutable sequence of
 * 64-bit integers called tags.
 * 
 * <p>
 * OIDs also contain mutable data which are accessible via {@link OidData}.
 * 
 * <p>
 * When represented as a String, OID components are joined with "." which is
 * known as dotted-notation.
 * 
 * <h3>Concrete/Generic</h3>
 * <p>
 * An OID is either "concrete", meaning that it corresponds to exactly one
 * virtual object, or "generic" which means the OID corresponds to multiple
 * objects of the same type.
 * 
 * <h3>Absolute/Relative</h3>
 * <p>
 * An OID is either "absolute", meaning that its head element is the root of the
 * state tree, or "relative" which means the OID is a proper subset of an
 * absolute OID.
 */
public interface Oid extends Comparable<Oid>, Iterable<Long> {

	/**
	 * Extend the OID by adding the given component to the end.
	 * 
	 * @param component The new component
	 * @return A new OID
	 */
	public Oid child(long component);

	@Override
	public default int compareTo(Oid oid) {
		return Arrays.compare(value(), oid.value());
	}

	/**
	 * Get the first (leftmost) component of the OID.
	 *
	 * @return The OID's first component
	 */
	public default long first() {
		return value()[0];
	}

	public <T> T getData(OidData<T> dataType);

	public default <T> T getData(OidData<T> dataType, T defaultItem) {
		T item = getData(dataType);
		return item == null ? defaultItem : item;
	}

	/**
	 * Truncate the OID by removing components from the end.
	 * 
	 * @param length The length of the new OID
	 * @return A new OID
	 */
	public Oid head(int length);

	/**
	 * Determine whether this OID is a descendant of the given OID.
	 * 
	 * @param oid The ancestor OID
	 * @return Whether this OID is a descendant
	 */
	public default boolean isChildOf(Oid oid) {
		if (oid == null)
			return true;

		return Arrays.mismatch(value(), oid.value()) == Math.min(size(), oid.size());
	}

	/**
	 * Determine whether the OID corresponds to exactly one entity (concrete) or
	 * multiple entities (generic). The OID is generic if it contains at least one
	 * component that equals 0.
	 * 
	 * @return Whether the OID is concrete
	 */
	public default boolean isConcrete() {
		for (long i : value())
			if (i == 0)
				return false;
		return true;
	}

	@Override
	public default Iterator<Long> iterator() {
		return Arrays.stream(value()).iterator();
	}

	/**
	 * Get the last (rightmost) component of the OID.
	 *
	 * @return The OID's last component
	 */
	public default long last() {
		return value()[size() - 1];
	}

	/**
	 * Get the parent OID.
	 * 
	 * @return A new OID or {@code null} if this OID is the root
	 */
	public Oid parent();

	/**
	 * Return an OID that's the result of removing the given OID from the left side
	 * of {@code this}.
	 * 
	 * @param oid The reference OID which must be an ancestor of {@code this}
	 * @return A new OID
	 */
	public RelativeOid relativize(Oid oid);

	/**
	 * Return a new OID with its generic components replaced (from left to right)
	 * with the given components. The resulting OID will be concrete if the number
	 * of supplied components equals the number of generic components in the OID
	 * (and none of the supplied components are 0).
	 * 
	 * @param components The new components
	 * @return A new OID
	 */
	public Oid resolve(long... components);

	public <T, O extends Oid> O setData(OidData<T> dataType, T data);

	/**
	 * Get the number of components in the OID.
	 *
	 * @return The OID's length
	 */
	public default int size() {
		return value().length;
	}

	/**
	 * Return an OID without its first component.
	 * 
	 * @return A new OID
	 */
	public default RelativeOid tail() {
		return tail(1);
	}

	public RelativeOid tail(int offset);

	/**
	 * Get the components of the OID.
	 *
	 * @return The OID components
	 */
	public long[] value();

	public long namespace();
}
