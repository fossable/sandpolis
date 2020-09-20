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
 * An {@link Oid} corresponds to one or more objects in a virtual state tree.
 * OIDs have a sequence of 32-bit integers that can uniquely locate objects in a
 * tree structure.
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
public interface Oid extends Comparable<Oid>, Iterable<Integer> {

	public static final int ID_SPACE = 2;

	public static final int TYPE_ATTRIBUTE = 0;
	public static final int TYPE_DOCUMENT = 1;
	public static final int TYPE_COLLECTION = 2;
	public static final int TYPE_RELATION = 3;

	/**
	 * Extend the OID by adding the given component to the end.
	 * 
	 * @param component The new component
	 * @return A new OID
	 */
	public Oid child(int component);

	@Override
	public default int compareTo(Oid oid) {
		return Arrays.compare(value(), oid.value());
	}

	/**
	 * Get the first (leftmost) component of the OID.
	 *
	 * @return The OID's first component
	 */
	public default int first() {
		return value()[0];
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
		for (int i : value())
			if (i == 0)
				return false;
		return true;
	}

	@Override
	public default Iterator<Integer> iterator() {
		return Arrays.stream(value()).iterator();
	}

	/**
	 * Get the last (rightmost) component of the OID.
	 *
	 * @return The OID's last component
	 */
	public default int last() {
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
	public RelativeOid<?> relativize(Oid oid);

	/**
	 * Return a new OID with its generic components replaced (from left to right)
	 * with the given components. The resulting OID will be concrete if the number
	 * of supplied components equals the number of generic components in the OID
	 * (and none of the supplied components are 0).
	 * 
	 * @param components The new components
	 * @return A new OID
	 */
	public Oid resolve(int... components);

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
	public default RelativeOid<?> tail() {
		return tail(1);
	}

	public RelativeOid<?> tail(int offset);

	/**
	 * Get the components of the OID.
	 *
	 * @return The OID components
	 */
	public int[] value();

	public static int type(int component) {
		if (component == 0)
			throw new IllegalArgumentException("Oid component was 0");

		return component & ((1 << ID_SPACE) - 1);
	}
}
