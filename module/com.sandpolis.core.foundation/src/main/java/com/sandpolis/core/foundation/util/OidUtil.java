//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.foundation.util;

import com.google.common.hash.Hashing;

public final class OidUtil {

	public static final int LENGTH_OTYPE = 2;

	public static final int OTYPE_ATTRIBUTE = 2;
	public static final int OTYPE_COLLECTION = 1;
	public static final int OTYPE_DOCUMENT = 0;

	public static long computeAttributeTag(long raw) {
		return ((raw << LENGTH_OTYPE) | OTYPE_ATTRIBUTE) & Long.MAX_VALUE;
	}

	public static long computeCollectionTag(long raw) {
		return ((raw << LENGTH_OTYPE) | OTYPE_COLLECTION) & Long.MAX_VALUE;
	}

	public static long computeDocumentTag(long raw) {
		return ((raw << LENGTH_OTYPE) | OTYPE_DOCUMENT) & Long.MAX_VALUE;
	}

	public static int getOidType(long tag) {
		return (int) (tag & ((1L << LENGTH_OTYPE) - 1));
	}

	public static long computeNamespace(String id) {
		return computeDocumentTag(Hashing.murmur3_128().newHasher().putBytes(id.getBytes()).hash().asLong());
	}

	public static String[] splitPath(String path) {
		if (path.startsWith("//")) {
			throw new IllegalArgumentException();
		}

		// Strip up to one leading slash
		path = path.replaceAll("^/", "");

		String[] components = path.split("/");

		// Add an empty string for each trailing slash
		// TODO

		return components;
	}

	private OidUtil() {
	}
}
