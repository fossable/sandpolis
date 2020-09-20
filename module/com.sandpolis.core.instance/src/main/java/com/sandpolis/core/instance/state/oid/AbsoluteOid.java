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

import java.util.Arrays;

import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

public interface AbsoluteOid<T> extends Oid {

	public static AbsoluteOid<?> newOid(String value) {
		return newOid(Arrays.stream(value.split("\\.")).mapToInt(Integer::valueOf).toArray());
	}

	public static AbsoluteOid<?> newOid(int... value) {
		switch (Oid.type(value[value.length - 1])) {
		case TYPE_ATTRIBUTE:
			return new STAttributeOid<>(value);
		case TYPE_DOCUMENT:
			return new STDocumentOid<>(value);
		case TYPE_COLLECTION:
			return new STCollectionOid<>(value);
		case TYPE_RELATION:
//			return  new AbsoluteOidImpl<>(value);// TODO
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * An absolute {@link Oid} that corresponds to an {@link STAttribute}.
	 *
	 * @param <T> The type of the corresponding attribute's value
	 */
	public static class STAttributeOid<T> extends OidBase implements AbsoluteOid<T> {

		public STAttributeOid(String oid) {
			super(oid);

			if (Oid.type(last()) != Oid.TYPE_ATTRIBUTE)
				throw new IllegalArgumentException();
		}

		public STAttributeOid(int[] oid) {
			super(oid);

			if (Oid.type(last()) != Oid.TYPE_ATTRIBUTE)
				throw new IllegalArgumentException();
		}

		@Override
		public AbsoluteOid.STAttributeOid<T> resolve(int... tags) {
			return resolve(AbsoluteOid.STAttributeOid::new, tags);
		}

		public AbsoluteOid.STAttributeOid<T> resolveLocal() {
			return resolve(9);
		}

		@Override
		public AbsoluteOid.STDocumentOid<?> parent() {
			return parent(AbsoluteOid.STDocumentOid::new);
		}

		@Override
		public RelativeOid.STAttributeOid<T> tail(int offset) {
			return tail(RelativeOid.STAttributeOid::new, offset);
		}

		@Override
		public Oid child(int component) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AbsoluteOid<?> head(int length) {
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
	public static class STCollectionOid<T extends VirtObject> extends OidBase implements AbsoluteOid<T> {

		public STCollectionOid(String oid) {
			super(oid);

			if (Oid.type(last()) != Oid.TYPE_COLLECTION)
				throw new IllegalArgumentException();
		}

		public STCollectionOid(int[] oid) {
			super(oid);

			if (Oid.type(last()) != Oid.TYPE_COLLECTION)
				throw new IllegalArgumentException();
		}

		@Override
		public AbsoluteOid.STCollectionOid<?> resolve(int... tags) {
			return resolve(AbsoluteOid.STCollectionOid::new, tags);
		}

		public AbsoluteOid.STCollectionOid<?> resolveLocal() {
			return resolve(9);
		}

		@Override
		public RelativeOid.STCollectionOid<T> tail(int offset) {
			return tail(RelativeOid.STCollectionOid::new, offset);
		}

		@Override
		public AbsoluteOid.STDocumentOid<?> parent() {
			return parent(AbsoluteOid.STDocumentOid::new);
		}

		@Override
		public AbsoluteOid.STDocumentOid<?> child(int component) {
			return child(AbsoluteOid.STDocumentOid::new, component);
		}

		@Override
		public AbsoluteOid<?> head(int length) {
			return head(AbsoluteOid::newOid, length);
		}

		@Override
		public RelativeOid.STCollectionOid<T> relativize(Oid oid) {
			return relativize(RelativeOid.STCollectionOid::new, oid);
		}
	}

	/**
	 * An absolute {@link Oid} that corresponds to a {@link STDocument}.
	 *
	 * @param <T> The type of the corresponding document
	 */
	public static class STDocumentOid<T extends VirtObject> extends OidBase implements AbsoluteOid<T> {

		public STDocumentOid(String oid) {
			super(oid);

			if (Oid.type(last()) != Oid.TYPE_DOCUMENT)
				throw new IllegalArgumentException();
		}

		public STDocumentOid(int[] oid) {
			super(oid);

			if (Oid.type(last()) != Oid.TYPE_DOCUMENT)
				throw new IllegalArgumentException("Unacceptable document tag: " + last());
		}

		@Override
		public AbsoluteOid.STDocumentOid<?> resolve(int... tags) {
			return resolve(AbsoluteOid.STDocumentOid::new, tags);
		}

		public AbsoluteOid.STDocumentOid<?> resolveLocal() {
			return resolve(9);
		}

		@Override
		public AbsoluteOid<?> parent() {
			return parent(AbsoluteOid::newOid);
		}

		@Override
		public RelativeOid.STDocumentOid<T> tail(int offset) {
			return tail(RelativeOid.STDocumentOid::new, offset);
		}

		@Override
		public AbsoluteOid<?> child(int component) {
			return child(AbsoluteOid::newOid, component);
		}

		@Override
		public AbsoluteOid<?> head(int length) {
			return head(AbsoluteOid::newOid, length);
		}

		@Override
		public RelativeOid.STDocumentOid<T> relativize(Oid oid) {
			return relativize(RelativeOid.STDocumentOid::new, oid);
		}
	}
}
