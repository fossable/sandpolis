package com.sandpolis.core.foundation.util;

import com.google.common.hash.Hashing;

public final class OidUtil {

	public static final int LENGTH_OTYPE = 2;

	public static final int OTYPE_ATTRIBUTE = 2;
	public static final int OTYPE_COLLECTION = 1;
	public static final int OTYPE_DOCUMENT = 0;

	public static long computeAttributeTag(long raw) {
		return (raw << LENGTH_OTYPE) | OTYPE_ATTRIBUTE;
	}

	public static long computeCollectionTag(long raw) {
		return (raw << LENGTH_OTYPE) | OTYPE_COLLECTION;
	}

	public static long computeDocumentTag(long raw) {
		return (raw << LENGTH_OTYPE) | OTYPE_DOCUMENT;
	}

	public static int getOidType(long tag) {
		return (int) (tag & ((1L << LENGTH_OTYPE) - 1));
	}

	public static long uuidToTag(String uuid) {
		return computeDocumentTag(Hashing.murmur3_128().newHasher().putBytes(uuid.getBytes()).hash().asLong());
	}

	private OidUtil() {
	}
}
