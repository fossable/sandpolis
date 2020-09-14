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
package com.sandpolis.core.instance.state.oid;

public interface RelativeOid<T> extends Oid {

	public static class RelativeOidImpl<T> extends OidBase implements RelativeOid<T> {

		public RelativeOidImpl(int... value) {
			super(value);
		}

		@Override
		public RelativeOid<?> parent() {
			return parent(RelativeOidImpl::new);
		}

		@Override
		public RelativeOid<T> resolve(int... tags) {
			return resolve(RelativeOidImpl::new, tags);
		}

		@Override
		public RelativeOid<T> tail() {
			return tail(RelativeOidImpl::new);
		}
	}
}
