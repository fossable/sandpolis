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
package com.sandpolis.core.net.util;

import java.lang.reflect.Method;

import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLite.Builder;
import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.foundation.util.IDUtil;
import com.sandpolis.core.net.Message.MSG;

/**
 * Utilities for simplifying common operations related to protocol buffers.
 * Using a static import is a convenient way to use these methods.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class MsgUtil {

	public static MSG.Builder msg() {
		return MSG.newBuilder();
	}

	public static MSG.Builder msg(MessageLiteOrBuilder payload) {
		return pack(msg(), payload);
	}

	public static MSG.Builder pack(MSG.Builder msg, MessageLiteOrBuilder payload) {
		if (payload instanceof Builder) {
			return msg.setPayload(((Builder) payload).build().toByteString()).setPayloadType(getPayloadType(payload));
		} else if (payload instanceof MessageLite) {
			return msg.setPayload(((MessageLite) payload).toByteString()).setPayloadType(getPayloadType(payload));
		} else {
			throw new AssertionError();
		}
	}

	public static <T extends MessageLite> T unpack(MSG msg, Class<T> payloadType) {
		try {
			var parseFrom = payloadType.getMethod("parseFrom", ByteString.class);
			return (T) parseFrom.invoke(null, msg.getPayload());
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Create a new empty request message.
	 *
	 * @return A new request
	 */
	public static MSG.Builder rq() {
		return MSG.newBuilder().setId(IDUtil.rq());
	}

	/**
	 * Create a new empty response message.
	 *
	 * @param id The sequence ID
	 * @return A new response
	 */
	public static MSG.Builder rs(int id) {
		return MSG.newBuilder().setId(id);
	}

	/**
	 * Create a new response message.
	 *
	 * @param rq The original request message
	 * @return A new response
	 */
	public static MSG.Builder rs(MSG rq) {
		var rs = rs(rq.getId());
		if (rq.getFrom() != 0)
			rs.setTo(rq.getFrom());
		if (rq.getTo() != 0)
			rs.setFrom(rq.getTo());
		return rs;
	}

	public static MSG.Builder ev(int id) {
		return MSG.newBuilder().setId(id);
	}

	/**
	 * Create a new request message.
	 *
	 * @param payload The request payload
	 * @return A new request
	 */
	public static MSG.Builder rq(MessageLiteOrBuilder payload) {
		return pack(rq(), payload);
	}

	/**
	 * Create a new response message.
	 *
	 * @param msg     The original request message
	 * @param payload The response payload
	 * @return A new response
	 */
	public static MSG.Builder rs(MSG msg, MessageLiteOrBuilder payload) {
		return pack(rs(msg), payload);
	}

	public static MSG.Builder ev(int id, MessageLiteOrBuilder payload) {
		return pack(ev(id), payload);
	}

	public static int getPayloadType(MessageLiteOrBuilder payload) {
		return getPayloadType(payload.getClass());
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

	private MsgUtil() {
	}
}
