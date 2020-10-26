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
package com.sandpolis.core.net.exelet;

import static com.sandpolis.core.instance.Metatypes.InstanceType.AGENT;
import static com.sandpolis.core.instance.Metatypes.InstanceType.SERVER;
import static com.sandpolis.core.instance.Metatypes.InstanceType.CLIENT;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sandpolis.core.instance.Metatypes.InstanceType;
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
		 * Whether the handler will be available to authenticated connections only.
		 *
		 * @return The handler auth level
		 */
		public boolean auth();

		public InstanceType[] instances() default { AGENT, CLIENT, SERVER };
	}
}
