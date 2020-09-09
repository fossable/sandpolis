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

import com.sandpolis.core.instance.state.oid.RelativeOid.RelativeOidImpl;

public abstract class OidBase implements Oid {

	public static final int SUFFIX_ATTRIBUTE = 1;
	public static final int SUFFIX_DOCUMENT = 2;
	public static final int SUFFIX_COLLECTION = 3;
	public static final int SUFFIX_RELATION = 4;

	/**
	 * The components of the oid.
	 */
	protected final int[] value;

	/**
	 * The dotted representation of the oid which is computed and cached when
	 * necessary.
	 */
	private String dotted;

	public OidBase(String oid) {
		this(Arrays.stream(oid.split("\\.")).mapToInt(Integer::valueOf).toArray());
		this.dotted = oid;
	}

	public OidBase(int[] value) {
		if (value.length == 0)
			throw new IllegalArgumentException("Empty OID");

		this.value = value;
	}

	@Override
	public int[] value() {
		return value;
	}

	@Override
	public String toString() {
		if (dotted == null)
			// Compute the dotted string form
			dotted = Arrays.stream(value).boxed().map(String::valueOf).collect(Collectors.joining("."));

		return dotted;
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
	public RelativeOid<?> relativize(Oid oid) {
		if (!oid.isChildOf(this))
			throw new IllegalArgumentException();

		return new RelativeOidImpl<>(Arrays.copyOfRange(value, oid.size(), value.length));
	}

	protected <E extends OidBase> E resolve(Function<int[], E> cons, int... tags) {
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

	protected <E extends OidBase> E parent(Function<int[], E> cons) {
		if (size() == 1)
			throw new RuntimeException();

		return (E) head(size() - 1);
	}

	protected <E extends OidBase> E head(Function<int[], E> cons, int length) {
		if (value.length < length || length <= 0)
			throw new IllegalArgumentException("Target length out of range");

		return cons.apply(Arrays.copyOf(value, length));
	}

	protected <E extends OidBase> E tail(Function<int[], E> cons) {
		if (value.length == 1)
			throw new IllegalStateException("Cannot get tail of single element OID");

		return cons.apply(Arrays.copyOfRange(value, 1, value.length));
	}

	protected <E extends OidBase> E child(Function<int[], E> cons, int tag) {
		int[] n = Arrays.copyOf(value, value.length + 1);
		n[n.length - 1] = tag;
		return cons.apply(n);
	}

}
