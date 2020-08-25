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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An {@link Oid} corresponds to an item in a state tree.
 *
 * @param <T> The type of the object with which the OID corresponds
 */
public class Oid<T> implements Comparable<Oid<?>> {

	private final int[] value;

	private String stringValue;

	public Oid(String oid) {
		this(Arrays.stream(oid.split("\\.")).mapToInt(Integer::valueOf).toArray());
		this.stringValue = oid;
	}

	public Oid(int[] value) {
		if (value.length == 0)
			throw new IllegalArgumentException("Empty OID");

		this.value = value;
	}

	public boolean isChildOf(Oid<?> oid) {
		return Arrays.mismatch(this.value, oid.value) == this.value.length - 1;
	}

	public Oid<?> child(int tag) {
		int[] n = Arrays.copyOf(value, value.length + 1);
		n[n.length - 1] = tag;
		return new Oid<>(n);
	}

	/**
	 * Get the first component of the OID.
	 *
	 * @return The OID's first component
	 */
	public int head() {
		return value[0];
	}

	/**
	 * Get the last component of the OID.
	 *
	 * @return The OID's last component
	 */
	public int last() {
		return value[value.length - 1];
	}

	public Oid<?> head(int length) {
		if (value.length < length || length < 0)
			throw new IllegalArgumentException("Target length out of range");

		return new Oid<>(Arrays.copyOf(value, length));
	}

	/**
	 * Get the number of components in the OID.
	 *
	 * @return The OID's length
	 */
	public int size() {
		return value.length;
	}

	public Oid<?> tail() {
		if (value.length == 1)
			throw new IllegalStateException("Cannot get tail of single element OID");

		return new Oid<>(Arrays.copyOfRange(value, 1, value.length));
	}

	@Override
	public String toString() {
		if (stringValue != null)
			// Compute the dotted string form
			stringValue = Arrays.stream(value).boxed().map(String::valueOf).collect(Collectors.joining("."));

		return stringValue;
	}

	@Override
	public int compareTo(Oid<?> o) {
		return Arrays.compare(this.value, o.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Oid) {
			return Arrays.equals(value, ((Oid<?>) obj).value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * An {@link Oid} that corresponds to an {@link STAttribute}.
	 *
	 * @param <T> The type of the corresponding attribute's value
	 */
	public static class AttributeOid<T> extends Oid<T> {

		public AttributeOid(String oid) {
			super(oid);
		}

	}

	/**
	 * An {@link Oid} that corresponds to a {@link STCollection}.
	 *
	 * @param <T> The type of the corresponding collection
	 */
	public static class CollectionOid<T extends VirtObject> extends Oid<T> {

		public CollectionOid(String oid) {
			super(oid);
		}

	}

	/**
	 * An {@link Oid} that corresponds to a {@link STDocument}.
	 *
	 * @param <T> The type of the corresponding document
	 */
	public static class DocumentOid<T extends VirtObject> extends Oid<T> {

		public DocumentOid(String oid) {
			super(oid);
		}

	}
}
