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

import com.sandpolis.core.instance.state.st.STObject;

public class RelativeOid<T extends STObject<?>> extends OidBase {

	public RelativeOid(String path) {
		super(null, path);
	}

	public RelativeOid(String[] path) {
		super(null, path);
	}

	public RelativeOid(String namespace, String path) {
		super(namespace, path);
	}

	public RelativeOid(String namespace, String[] path) {
		super(namespace, path);
	}

	@Override
	public RelativeOid<T> resolve(String... components) {
		return resolve(RelativeOid::new, components);
	}

	@Override
	public RelativeOid parent() {
		return parent(RelativeOid::new);
	}

	@Override
	public RelativeOid<T> tail(int offset) {
		return tail(RelativeOid::new, offset);
	}

	@Override
	public RelativeOid<?> child(String component) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RelativeOid head(int length) {
		return head(RelativeOid::new, length);
	}

	@Override
	public RelativeOid<T> relativize(Oid oid) {
		return relativize(RelativeOid::new, oid);
	}
}
