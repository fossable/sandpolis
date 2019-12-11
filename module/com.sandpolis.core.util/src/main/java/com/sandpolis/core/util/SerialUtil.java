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
package com.sandpolis.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Object serialization utilities using default Java serialization.
 *
 * @author cilki
 * @since 1.0.0
 */
public final class SerialUtil {
	private SerialUtil() {
	}

	/**
	 * Serialize an object using the default serializer.
	 *
	 * @param object A {@link Serializable} object
	 * @return An array representing the object
	 * @throws IOException
	 */
	public static byte[] serialize(Object object) throws IOException {
		return serialize(object, false);
	}

	/**
	 * Serialize an object using the default serializer.
	 *
	 * @param object     A {@link Serializable} object
	 * @param compressed Indicates whether the serialized data should be compressed
	 * @return An array representing the object
	 * @throws IOException
	 */
	public static byte[] serialize(Object object, boolean compressed) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialize(object, out, compressed);

		return out.toByteArray();
	}

	/**
	 * Serialize an object using the default serializer.
	 *
	 * @param object     A {@link Serializable} object
	 * @param out
	 * @param compressed Indicates whether the serialized data should be compressed
	 * @throws IOException
	 */
	public static void serialize(Object object, OutputStream out, boolean compressed) throws IOException {
		if (object == null)
			throw new IllegalArgumentException();
		if (!(object instanceof Serializable))
			throw new IllegalArgumentException();
		if (out == null)
			throw new IllegalArgumentException();

		if (compressed) {
			out = new DeflaterOutputStream(out);
		}

		try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(object);
		}
	}

	/**
	 * Deserialize an object with the default deserializer.
	 *
	 * @param object A serialized object
	 * @return The restored object
	 * @throws IOException
	 * @throws ClassNotFoundException If the referenced class is not loaded
	 */
	public static Serializable deserialize(byte[] object) throws IOException, ClassNotFoundException {
		return deserialize(object, false);
	}

	/**
	 * Deserialize an object with the default deserializer.
	 *
	 * @param object
	 * @param compressed Indicates whether the serialized data is compressed
	 * @return The restored object
	 * @throws IOException
	 * @throws ClassNotFoundException If the referenced class is not loaded
	 */
	public static Serializable deserialize(byte[] object, boolean compressed)
			throws IOException, ClassNotFoundException {
		if (object == null)
			throw new IllegalArgumentException();

		return deserialize(new ByteArrayInputStream(object), compressed);
	}

	/**
	 * Deserialize an object with the default deserializer.
	 *
	 * @param in         The serialized data
	 * @param compressed Indicates whether the serialized data is compressed
	 * @return The restored object
	 * @throws IOException
	 * @throws ClassNotFoundException If the referenced class is not loaded
	 */
	public static Serializable deserialize(InputStream in, boolean compressed)
			throws IOException, ClassNotFoundException {
		if (in == null)
			throw new IllegalArgumentException();

		if (compressed)
			in = new InflaterInputStream(in);

		try (ObjectInputStream ois = new ObjectInputStream(in)) {
			return (Serializable) ois.readObject();
		}
	}

}
