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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
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

	public static MSG.Builder msg(MessageOrBuilder payload) {
		return setPayload(msg(), payload);
	}

	private static MSG.Builder setPayload(MSG.Builder msg, MessageOrBuilder payload) {
		if (payload instanceof Builder) {
			return msg.setPayload(Any.pack(((Builder) payload).build(), getModuleId(payload)));
		} else if (payload instanceof Message) {
			return msg.setPayload(Any.pack((Message) payload, getModuleId(payload)));
		} else {
			throw new AssertionError();
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
	public static MSG.Builder rq(MessageOrBuilder payload) {
		return setPayload(rq(), payload);
	}

	/**
	 * Create a new response message.
	 *
	 * @param msg     The original request message
	 * @param payload The response payload
	 * @return A new response
	 */
	public static MSG.Builder rs(MSG msg, MessageOrBuilder payload) {
		return setPayload(rs(msg), payload);
	}

	public static MSG.Builder ev(int id, MessageOrBuilder payload) {
		return setPayload(ev(id), payload);
	}

	public static String getModuleId(MessageOrBuilder message) {
		return getModuleId(message.getClass());
	}

	public static String getModuleId(Class<?> messageType) {
		return messageType.getPackageName().replaceAll("\\.msg$", "");
	}

	public static String getModuleId(Method method) {
		for (var param : method.getParameterTypes()) {
			if (Message.class.isAssignableFrom(param)) {
				return getModuleId(param);
			}
		}

		throw new RuntimeException();
	}

	public static String getTypeUrl(Class<?> messageType) {
		var components = messageType.getPackageName().split("\\.");
		if (components.length < 3)
			throw new IllegalArgumentException();

		return String.format("%s/%s.%s.%s", getModuleId(messageType), components[components.length - 3],
				components[components.length - 2], components[components.length - 1]);
	}

	public static String getTypeUrl(Method method) {
		for (var param : method.getParameterTypes()) {
			if (Message.class.isAssignableFrom(param)) {
				return getTypeUrl(param);
			}
		}

		throw new RuntimeException();
	}

	private MsgUtil() {
	}
}
