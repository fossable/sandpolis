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

import com.sandpolis.core.instance.state.oid.RelativeOid.RelativeOidImpl;

public interface AbsoluteOid<T> extends Oid {

	public static class AbsoluteOidImpl<T> extends OidBase implements AbsoluteOid<T> {

		public AbsoluteOidImpl(int[] value) {
			super(value);
		}

		@Override
		public AbsoluteOid<?> parent() {
			return parent(AbsoluteOidImpl::new);
		}

		@Override
		public AbsoluteOid<T> resolve(int... tags) {
			return resolve(AbsoluteOidImpl::new, tags);
		}

		@Override
		public AbsoluteOid<?> head(int length) {
			return head(AbsoluteOidImpl::new, length);
		}

		@Override
		public RelativeOid<T> tail() {
			return tail(RelativeOidImpl::new);
		}

		@Override
		public AbsoluteOid<?> child(int tag) {
			return child(AbsoluteOidImpl::new, tag);
		}

	}
}
