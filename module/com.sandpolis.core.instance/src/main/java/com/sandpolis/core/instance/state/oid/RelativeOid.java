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

import com.sandpolis.core.foundation.util.OidUtil;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

public interface RelativeOid<T> extends Oid {

	public static RelativeOid<?> newOid(long... value) {
		switch (OidUtil.getOidType(value[value.length - 1])) {
		case OTYPE_ATTRIBUTE:
			return new STAttributeOid<>(value);
		case OTYPE_DOCUMENT:
			return new STDocumentOid<>(value);
		case OTYPE_COLLECTION:
			return new STCollectionOid<>(value);
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * A relative {@link Oid} that corresponds to an {@link STAttribute}.
	 *
	 * @param <T> The type of the corresponding attribute's value
	 */
	public static class STAttributeOid<T> extends OidBase implements RelativeOid<T> {

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
		public RelativeOid.STAttributeOid<T> resolve(long... tags) {
			return resolve(RelativeOid.STAttributeOid::new, tags);
		}

		public RelativeOid.STAttributeOid<T> resolveLocal() {
			return resolve(OidUtil.uuidToTag(Core.UUID));
		}

		public RelativeOid.STAttributeOid<T> resolveUuid(String uuid) {
			return resolve(OidUtil.uuidToTag(uuid));
		}

		@Override
		public RelativeOid.STDocumentOid<?> parent() {
			return parent(RelativeOid.STDocumentOid::new);
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
		public RelativeOid<?> head(int length) {
			return head(RelativeOid::newOid, length);
		}

		@Override
		public RelativeOid.STAttributeOid<T> relativize(Oid oid) {
			return relativize(RelativeOid.STAttributeOid::new, oid);
		}
	}

	/**
	 * A relative {@link Oid} that corresponds to a {@link STCollection}.
	 *
	 * @param <T> The type of the corresponding collection
	 */
	public static class STCollectionOid<T extends VirtObject> extends OidBase implements RelativeOid<T> {

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
		public RelativeOid.STCollectionOid<?> resolve(long... tags) {
			return resolve(RelativeOid.STCollectionOid::new, tags);
		}

		public RelativeOid.STCollectionOid<?> resolveLocal() {
			return resolve(OidUtil.uuidToTag(Core.UUID));
		}

		public RelativeOid.STCollectionOid<?> resolveUuid(String uuid) {
			return resolve(OidUtil.uuidToTag(uuid));
		}

		@Override
		public RelativeOid.STCollectionOid<T> tail(int offset) {
			return tail(RelativeOid.STCollectionOid::new, offset);
		}

		@Override
		public RelativeOid.STDocumentOid<?> parent() {
			return parent(RelativeOid.STDocumentOid::new);
		}

		@Override
		public RelativeOid.STDocumentOid<?> child(long component) {
			return child(RelativeOid.STDocumentOid::new, component);
		}

		@Override
		public RelativeOid<?> head(int length) {
			return head(RelativeOid::newOid, length);
		}

		@Override
		public RelativeOid.STCollectionOid<T> relativize(Oid oid) {
			return relativize(RelativeOid.STCollectionOid::new, oid);
		}
	}

	/**
	 * A relative {@link Oid} that corresponds to a {@link STDocument}.
	 *
	 * @param <T> The type of the corresponding document
	 */
	public static class STDocumentOid<T extends VirtObject> extends OidBase implements RelativeOid<T> {

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
		public RelativeOid.STDocumentOid<?> resolve(long... tags) {
			return resolve(RelativeOid.STDocumentOid::new, tags);
		}

		public RelativeOid.STDocumentOid<?> resolveLocal() {
			return resolve(OidUtil.uuidToTag(Core.UUID));
		}

		public RelativeOid.STDocumentOid<?> resolveUuid(String uuid) {
			return resolve(OidUtil.uuidToTag(uuid));
		}

		@Override
		public RelativeOid<?> parent() {
			return parent(RelativeOid::newOid);
		}

		@Override
		public RelativeOid.STDocumentOid<T> tail(int offset) {
			return tail(RelativeOid.STDocumentOid::new, offset);
		}

		@Override
		public RelativeOid<?> child(long component) {
			return child(RelativeOid::newOid, component);
		}

		@Override
		public RelativeOid<?> head(int length) {
			return head(RelativeOid::newOid, length);
		}

		@Override
		public RelativeOid.STDocumentOid<T> relativize(Oid oid) {
			return relativize(RelativeOid.STDocumentOid::new, oid);
		}
	}
}
