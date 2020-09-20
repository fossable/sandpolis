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
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class OidBase implements Oid {

	/**
	 * The dotted representation of the oid which is computed and cached when
	 * necessary.
	 */
	private String dotted;

	/**
	 * The components of the oid.
	 */
	protected final int[] value;

	public OidBase(int[] value) {
		if (value.length == 0)
			throw new IllegalArgumentException("Empty OID");

		this.value = value;
	}

	public OidBase(String oid) {
		this(Arrays.stream(oid.split("\\.")).mapToInt(Integer::valueOf).toArray());
		this.dotted = oid;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OidBase) {
			return Arrays.equals(value, ((OidBase) obj).value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		if (dotted == null)
			// Compute the dotted string form
			dotted = Arrays.stream(value).boxed().map(String::valueOf).collect(Collectors.joining("."));

		return dotted;
	}

	@Override
	public int[] value() {
		return value;
	}

	protected <E extends Oid> E child(Function<int[], E> cons, int component) {
		int[] n = Arrays.copyOf(value, value.length + 1);
		n[n.length - 1] = component;
		return cons.apply(n);
	}

	protected <E extends Oid> E head(Function<int[], E> cons, int length) {
		if (value.length < length || length <= 0)
			throw new IllegalArgumentException("Target length out of range");

		return cons.apply(Arrays.copyOf(value, length));
	}

	protected <E extends Oid> E parent(Function<int[], E> cons) {
		if (size() == 1)
			return null;

		return (E) head(size() - 1);
	}

	protected <E extends RelativeOid<?>> E relativize(Function<int[], E> cons, Oid oid) {
		if (oid == null)
			return cons.apply(value.clone());

		if (!isChildOf(oid))
			throw new IllegalArgumentException("Target: " + this + " must be a child of: " + oid);

		return cons.apply(Arrays.copyOfRange(value, oid.size(), value.length));
	}

	protected <E extends Oid> E resolve(Function<int[], E> cons, int... tags) {
		if (isConcrete())
			throw new IllegalStateException("Cannot resolve a concrete OID");

		int[] components = value.clone();

		int i = 0;
		for (int tag : tags) {
			for (; i < components.length; i++) {
				if (components[i] == 0) {
					components[i] = tag;
					break;
				}
			}
		}

		return cons.apply(components);
	}

	protected <E extends Oid> E tail(Function<int[], E> cons, int offset) {
		if (value.length < offset || offset < 1)
			throw new IllegalStateException("Invalid tail offset: " + offset);

		return cons.apply(Arrays.copyOfRange(value, offset, value.length));
	}
}
