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
 * Indicates that a concrete OID is required, but a generic OID was supplied
 * instead.
 */
public class GenericOidException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GenericOidException(Oid oid) {
		super("A concrete OID was required, but not supplied: " + oid);
	}
}
