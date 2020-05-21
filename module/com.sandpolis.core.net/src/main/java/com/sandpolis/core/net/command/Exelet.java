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
package com.sandpolis.core.net.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sandpolis.core.net.connection.Connection;

/**
 * An {@link Exelet} handles incoming messages from a {@link Connection}.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class Exelet {

	/**
	 * Defines the message type that the target {@link Exelet} method handles.
	 *
	 * @author cilki
	 * @since 5.1.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Handler {

		/**
		 * The message tag of the message that is handled.
		 *
		 * @return The message tag
		 */
		public int tag();
	}

	/**
	 * When applied to an {@link Exelet} method, the method will be executable on
	 * authenticated connections only.
	 *
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Auth {
	}

	/**
	 * When applied to an {@link Exelet} method, the method will be executable on
	 * unauthenticated connections only.
	 *
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Unauth {
	}

	/**
	 * When applied to an {@link Exelet} method, the method will be executable on
	 * connections which have the necessary permission.
	 *
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Permission {
		short permission();
	}
}
