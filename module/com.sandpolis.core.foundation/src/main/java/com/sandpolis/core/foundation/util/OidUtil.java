package com.sandpolis.core.foundation.util;

import com.google.common.hash.Hashing;

public final class OidUtil {

	public static final int OFFSET_OTYPE = 0;
	public static final int LENGTH_OTYPE = 2;

	public static final int OTYPE_ATTRIBUTE = 2;
	public static final int OTYPE_COLLECTION = 1;
	public static final int OTYPE_DOCUMENT = 0;

	public static final int OFFSET_ATYPE = OFFSET_OTYPE + LENGTH_OTYPE;
	public static final int LENGTH_ATYPE = 5;

	public static final int ATYPE_BOOLEAN = 0;
	public static final int ATYPE_BYTES = 1;
	public static final int ATYPE_DOUBLE = 2;
	public static final int ATYPE_INSTANCEFLAVOR = 3;
	public static final int ATYPE_INSTANCETYPE = 4;
	public static final int ATYPE_INTEGER = 5;
	public static final int ATYPE_LONG = 6;
	public static final int ATYPE_OID = 7;
	public static final int ATYPE_OSTYPE = 8;
	public static final int ATYPE_STRING = 9;
	public static final int ATYPE_X509CERTIFICATE = 10;

	public static final int OFFSET_SINGULARITY = OFFSET_ATYPE + LENGTH_ATYPE;
	public static final int LENGTH_SINGULARITY = 1;

	public static long computeAttributeTag(long raw, String type, boolean singular) {
		switch (type) {
		case "java.lang.Boolean":
			return computeAttributeTag(raw, ATYPE_BOOLEAN, singular);
		case "java.lang.Byte[]":
			return computeAttributeTag(raw, ATYPE_BYTES, singular);
		case "java.lang.Double":
			return computeAttributeTag(raw, ATYPE_DOUBLE, singular);
		case "java.lang.Integer":
			return computeAttributeTag(raw, ATYPE_INTEGER, singular);
		case "java.lang.Long":
			return computeAttributeTag(raw, ATYPE_LONG, singular);
		case "java.lang.String":
			return computeAttributeTag(raw, ATYPE_STRING, singular);
		case "com.sandpolis.core.instance.Metatypes.InstanceType":
			return computeAttributeTag(raw, ATYPE_INSTANCETYPE, singular);
		case "com.sandpolis.core.instance.Metatypes.InstanceFlavor":
			return computeAttributeTag(raw, ATYPE_INSTANCEFLAVOR, singular);
		case "com.sandpolis.core.foundation.Platform.OsType":
			return computeAttributeTag(raw, ATYPE_OSTYPE, singular);
		case "java.security.cert.X509Certificate":
			return computeAttributeTag(raw, ATYPE_X509CERTIFICATE, singular);
		}

		throw new IllegalArgumentException("Unknown type: " + type);
	}

	public static long computeAttributeTag(long raw, int type, boolean singular) {

		// Set OID type
		raw = encode(raw, OFFSET_OTYPE, LENGTH_OTYPE, OTYPE_ATTRIBUTE);

		// Set attribute type
		raw = encode(raw, OFFSET_ATYPE, LENGTH_ATYPE, type);

		// Set singularity
		raw = encode(raw, OFFSET_SINGULARITY, LENGTH_SINGULARITY, singular ? 1 : 0);

		return raw;
	}

	public static long computeCollectionTag(long raw) {
		return encode(raw, OFFSET_OTYPE, LENGTH_OTYPE, OTYPE_COLLECTION);
	}

	public static long computeDocumentTag(long raw) {
		return encode(raw, OFFSET_OTYPE, LENGTH_OTYPE, OTYPE_DOCUMENT);
	}

	public static int getAttributeType(long tag) {
		if (getOidType(tag) != OTYPE_ATTRIBUTE)
			throw new IllegalArgumentException("Only attributes can have value types (tag: " + tag + ")");

		return decode(tag, OFFSET_ATYPE, LENGTH_ATYPE);
	}

	public static int getOidType(long tag) {
		return decode(tag, OFFSET_OTYPE, LENGTH_OTYPE);
	}

	public static boolean isSingular(long tag) {
		if (getOidType(tag) != OTYPE_ATTRIBUTE)
			throw new IllegalArgumentException("Only attributes can contain lists");

		return decode(tag, OFFSET_SINGULARITY, LENGTH_SINGULARITY) == 1;
	}

	public static long uuidToTag(String uuid) {
		return computeDocumentTag(Hashing.murmur3_128().newHasher().putBytes(uuid.getBytes()).hash().asLong());
	}

	private static long encode(long base, int offset, int length, long value) {

		// Clear a space for the value
		base &= ~(((1L << length) - 1) << (Long.SIZE - (offset + length)));

		// Position the value
		value <<= Long.SIZE - (offset + length);

		// Write value
		return base | value;
	}

	private static int decode(long base, int offset, int length) {
		return (int) ((base >> (Long.SIZE - (offset + length))) & ((1L << length) - 1));
	}

	private OidUtil() {
	}
}
