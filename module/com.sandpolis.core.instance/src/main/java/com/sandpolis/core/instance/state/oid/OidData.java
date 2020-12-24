//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.state.oid;

/**
 * {@link OidData} allows auxiliary data to be associated with an {@link Oid}.
 *
 * @since 7.0.0
 *
 * @param <T> The data type
 */
public final class OidData<T> {

	/**
	 * The class of the corresponding attribute's value.
	 */
	public static final OidData<Class<?>> TYPE = new OidData<>();

	/**
	 * Whether the corresponding attribute is singular.
	 */
	public static final OidData<Boolean> SINGULARITY = new OidData<>();

	/**
	 * Whether the corresponding attribute is immutable.
	 */
	public static final OidData<Boolean> IMMUTABLE = new OidData<>();

	/**
	 * The corresponding osquery type if applicable.
	 */
	public static final OidData<String> OSQUERY = new OidData<>();
}
