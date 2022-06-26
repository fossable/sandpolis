//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.exelet;

import static org.s7s.core.foundation.Instance.InstanceType.AGENT;
import static org.s7s.core.foundation.Instance.InstanceType.SERVER;
import static org.s7s.core.foundation.Instance.InstanceType.CLIENT;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.connection.Connection;

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
