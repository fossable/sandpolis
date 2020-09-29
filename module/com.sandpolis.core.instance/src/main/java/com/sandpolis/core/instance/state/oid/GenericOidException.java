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
