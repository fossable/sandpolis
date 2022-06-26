//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLite.Builder;
import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.foundation.S7SRandom;
import org.s7s.core.protocol.Message.MSG;

public record S7SMsg(MessageLiteOrBuilder msg) {

	private static final HashMap<Class<?>, Function<MSG, ?>> unpackCache = new HashMap<>();

	public static S7SMsg of(MSG msg) {
		return new S7SMsg(msg);
	}

	public MSG.Builder asBuilder() {
		if (msg instanceof MSG.Builder m) {
			return m;
		}

		throw new IllegalStateException();
	}

	public MSG asMsg() {
		if (msg instanceof MSG m) {
			return m;
		}

		throw new IllegalStateException();
	}

	public MSG.Builder pack(MessageLiteOrBuilder payload) {
		if (payload instanceof Builder p) {
			return pack(p);
		}
		if (payload instanceof MessageLite p) {
			return pack(p);
		}

		throw new AssertionError();
	}

	public MSG.Builder pack(Builder payload) {
		return asBuilder().setPayload(payload.build().toByteString())
				.setPayloadType(getPayloadType(payload.getClass()));
	}

	public MSG.Builder pack(MessageLite payload) {
		return asBuilder().setPayload(payload.toByteString()).setPayloadType(getPayloadType(payload.getClass()));
	}

	public MSG.Builder pack(Enum payload) {
		if (payload.ordinal() > 64) {
			throw new IllegalArgumentException("Payload ordinal overflow");
		}
		return asBuilder().setPayload(ByteString.copyFrom(ByteBuffer.allocate(1).put((byte) payload.ordinal())))
				.setPayloadType(getPayloadType(payload.getClass()));
	}

	@SuppressWarnings("unchecked")
	public <T> T unpack(Class<T> payloadType) {

		var unpacker = unpackCache.get(payloadType);
		if (unpacker == null) {

			// Enum payload
			if (Enum.class.isAssignableFrom(payloadType)) {
				try {
					var values = (T[]) payloadType.getMethod("values").invoke(null);

					unpacker = msg -> {
						return values[msg.getPayload().asReadOnlyByteBuffer().get()];
					};
					unpackCache.put(payloadType, unpacker);
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					throw new IllegalArgumentException(e);
				}
			}

			// Regular payload
			else {

				MethodHandle parseFrom;
				try {
					parseFrom = MethodHandles.publicLookup()
							.unreflect(payloadType.getMethod("parseFrom", ByteString.class));
				} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException(e);
				}

				unpacker = msg -> {
					try {
						return parseFrom.invokeExact(msg.getPayload());
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				};
				unpackCache.put(payloadType, unpacker);
			}
		}

		return (T) unpacker.apply(asMsg());
	}

	public static S7SMsg rq() {
		return new S7SMsg(MSG.newBuilder().setId(S7SRandom.nextNonzeroInt()));
	}

	/**
	 * Create a new empty response message.
	 *
	 * @param id The sequence ID
	 * @return A new response
	 */
	public static S7SMsg rs(int id) {
		return new S7SMsg(MSG.newBuilder().setId(id));
	}

	/**
	 * Create a new response message.
	 *
	 * @param rq The original request message
	 * @return A new response
	 */
	public static S7SMsg rs(MSG rq) {
		return new S7SMsg(MSG.newBuilder().setId(rq.getId()).setTo(rq.getFrom()).setFrom(rq.getTo()));
	}

	public static S7SMsg ev(int id) {
		return new S7SMsg(MSG.newBuilder().setId(id));
	}

	public static int getPayloadType(Class<?> messageType) {
		return Hashing.murmur3_32().hashUnencodedChars(messageType.getName().replaceAll("\\$Builder$", "")).asInt();
	}

	public static int getPayloadType(Method method) {
		for (var param : method.getParameterTypes()) {
			if (MessageLite.class.isAssignableFrom(param)) {
				return getPayloadType(param);
			}
		}

		throw new IllegalArgumentException();
	}

}
