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

import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_ATTRIBUTE;
import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_COLLECTION;
import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_DOCUMENT;

import java.util.Arrays;

import com.sandpolis.core.foundation.util.OidUtil;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;

public interface AbsoluteOid extends Oid {

	public static AbsoluteOid newOid(String value) {
		return newOid(Arrays.stream(value.split("\\.")).mapToLong(Long::valueOf).toArray());
	}

	public static AbsoluteOid newOid(long... value) {
		switch (OidUtil.getOidType(value[value.length - 1])) {
		case OTYPE_ATTRIBUTE:
			return new STAttributeOid<>(value);
		case OTYPE_DOCUMENT:
			return new STDocumentOid(value);
		case OTYPE_COLLECTION:
			return new STCollectionOid(value);
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * An absolute {@link Oid} that corresponds to an {@link STAttribute}.
	 *
	 * @param <T> The type of the corresponding attribute's value
	 */
	public static class STAttributeOid<T> extends OidBase implements AbsoluteOid {

		public STAttributeOid(String oid) {
			super(oid);

			if (OidUtil.getOidType(last()) != OTYPE_ATTRIBUTE)
				throw new IllegalArgumentException();
		}

		public STAttributeOid(long[] oid) {
			super(oid);

			if (OidUtil.getOidType(last()) != OTYPE_ATTRIBUTE)
				throw new IllegalArgumentException();
		}

		@Override
		public AbsoluteOid.STAttributeOid<T> resolve(long... tags) {
			return resolve(AbsoluteOid.STAttributeOid::new, tags);
		}

		public AbsoluteOid.STAttributeOid<T> resolveLocal() {
			return resolve(OidUtil.computeNamespace(Core.UUID));
		}

		public AbsoluteOid.STAttributeOid<T> resolveUuid(String uuid) {
			return resolve(OidUtil.computeNamespace(uuid));
		}

		@Override
		public AbsoluteOid.STDocumentOid parent() {
			return parent(AbsoluteOid.STDocumentOid::new);
		}

		@Override
		public RelativeOid.STAttributeOid<T> tail(int offset) {
			return tail(RelativeOid.STAttributeOid::new, offset);
		}

		@Override
		public Oid child(long component) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AbsoluteOid head(int length) {
			return head(AbsoluteOid::newOid, length);
		}

		@Override
		public RelativeOid.STAttributeOid<T> relativize(Oid oid) {
			return relativize(RelativeOid.STAttributeOid::new, oid);
		}
	}

	/**
	 * An absolute {@link Oid} that corresponds to a {@link STCollection}.
	 *
	 * @param <T> The type of the corresponding collection
	 */
	public static class STCollectionOid extends OidBase implements AbsoluteOid {

		public STCollectionOid(String oid) {
			super(oid);

			if (OidUtil.getOidType(last()) != OTYPE_COLLECTION)
				throw new IllegalArgumentException();
		}

		public STCollectionOid(long[] oid) {
			super(oid);

			if (OidUtil.getOidType(last()) != OTYPE_COLLECTION)
				throw new IllegalArgumentException();
		}

		@Override
		public AbsoluteOid.STCollectionOid resolve(long... tags) {
			return resolve(AbsoluteOid.STCollectionOid::new, tags);
		}

		public AbsoluteOid.STCollectionOid resolveLocal() {
			return resolve(OidUtil.computeNamespace(Core.UUID));
		}

		public AbsoluteOid.STCollectionOid resolveUuid(String uuid) {
			return resolve(OidUtil.computeNamespace(uuid));
		}

		@Override
		public RelativeOid.STCollectionOid tail(int offset) {
			return tail(RelativeOid.STCollectionOid::new, offset);
		}

		@Override
		public AbsoluteOid.STDocumentOid parent() {
			return parent(AbsoluteOid.STDocumentOid::new);
		}

		@Override
		public AbsoluteOid.STDocumentOid child(long component) {
			return child(AbsoluteOid.STDocumentOid::new, component);
		}

		@Override
		public AbsoluteOid head(int length) {
			return head(AbsoluteOid::newOid, length);
		}

		@Override
		public RelativeOid.STCollectionOid relativize(Oid oid) {
			return relativize(RelativeOid.STCollectionOid::new, oid);
		}
	}

	/**
	 * An absolute {@link Oid} that corresponds to a {@link STDocument}.
	 *
	 * @param <T> The type of the corresponding document
	 */
	public static class STDocumentOid extends OidBase implements AbsoluteOid {

		public STDocumentOid(String oid) {
			super(oid);

			if (OidUtil.getOidType(last()) != OTYPE_DOCUMENT)
				throw new IllegalArgumentException();
		}

		public STDocumentOid(long[] oid) {
			super(oid);

			if (OidUtil.getOidType(last()) != OTYPE_DOCUMENT)
				throw new IllegalArgumentException("Unacceptable document tag: " + last());
		}

		@Override
		public AbsoluteOid.STDocumentOid resolve(long... tags) {
			return resolve(AbsoluteOid.STDocumentOid::new, tags);
		}

		public AbsoluteOid.STDocumentOid resolveLocal() {
			return resolve(OidUtil.computeNamespace(Core.UUID));
		}

		public AbsoluteOid.STDocumentOid resolveUuid(String uuid) {
			return resolve(OidUtil.computeNamespace(uuid));
		}

		@Override
		public AbsoluteOid parent() {
			return parent(AbsoluteOid::newOid);
		}

		@Override
		public RelativeOid.STDocumentOid tail(int offset) {
			return tail(RelativeOid.STDocumentOid::new, offset);
		}

		@Override
		public AbsoluteOid child(long component) {
			return child(AbsoluteOid::newOid, component);
		}

		@Override
		public AbsoluteOid head(int length) {
			return head(AbsoluteOid::newOid, length);
		}

		@Override
		public RelativeOid.STDocumentOid relativize(Oid oid) {
			return relativize(RelativeOid.STDocumentOid::new, oid);
		}
	}
}
