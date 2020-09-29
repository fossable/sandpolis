package com.sandpolis.core.instance.state.container;

import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_COLLECTION;
import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_DOCUMENT;

import java.util.Collection;
import java.util.function.Consumer;

import com.sandpolis.core.foundation.util.OidUtil;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.oid.GenericOidException;
import com.sandpolis.core.instance.state.oid.RelativeOid;

public interface CollectionContainer {

	/**
	 * Get a subcollection by its tag. This method never returns {@code null}.
	 *
	 * @param tag The subcollection tag
	 * @return The subcollection associated with the tag
	 */
	public STCollection collection(long tag);

	/**
	 * Get all subcollections.
	 *
	 * @return A collection of all subcollections
	 */
	public Collection<STCollection> collections();

	/**
	 * Get a subcollection by its tag. This method returns {@code null} if the
	 * subcollection doesn't exist.
	 *
	 * @param tag The subcollection tag
	 * @return The subcollection associated with the tag or {@code null}
	 */
	public STCollection getCollection(long tag);

	/**
	 * Overwrite the subcollection associated with the given tag.
	 *
	 * @param tag        The subcollection tag
	 * @param collection The subcollection to associate with the tag or {@code null}
	 */
	public void setCollection(long tag, STCollection collection);

	public default STCollection collection(RelativeOid<?> oid) {
		if (!oid.isConcrete())
			throw new GenericOidException(oid);

		if (oid.size() == 1) {
			switch (OidUtil.getOidType(oid.first())) {
			case OTYPE_COLLECTION:
				return collection(oid.first());
			}
			throw new IllegalArgumentException("Unacceptable collection tag: " + oid.first());
		} else {
			switch (OidUtil.getOidType(oid.first())) {
			case OTYPE_DOCUMENT:
				if (this instanceof DocumentContainer) {
					return ((DocumentContainer) this).document(oid.first()).collection(oid.tail());
				}
				break;
			case OTYPE_COLLECTION:
				return collection(oid.first()).document(oid.tail().first()).collection(oid.tail(2));
			}
			throw new IllegalArgumentException("Unexpected OID component: " + oid);
		}
	}

	public default STCollection get(RelativeOid.STCollectionOid<?> oid) {
		return collection(oid);
	}

	public STCollection newCollection();

	public void remove(STCollection collection);

	public void forEachCollection(Consumer<STCollection> consumer);
}
