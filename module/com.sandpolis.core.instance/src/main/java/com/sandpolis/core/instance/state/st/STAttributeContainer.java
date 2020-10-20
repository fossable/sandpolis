package com.sandpolis.core.instance.state.st;

import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_ATTRIBUTE;
import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_COLLECTION;
import static com.sandpolis.core.foundation.util.OidUtil.OTYPE_DOCUMENT;

import java.util.Collection;
import java.util.function.Consumer;

import com.sandpolis.core.foundation.util.OidUtil;
import com.sandpolis.core.instance.state.oid.GenericOidException;
import com.sandpolis.core.instance.state.oid.RelativeOid;

public interface STAttributeContainer {

	/**
	 * Get an attribute by its tag. This method never returns {@code null}.
	 *
	 * @param <E> The type of the attribute's value
	 * @param tag The attribute tag
	 * @return The attribute associated with the tag
	 */
	public <E> STAttribute<E> attribute(long tag);

	/**
	 * Get all attributes in the document.
	 *
	 * @return A collection of all attributes
	 */
	public Collection<STAttribute<?>> attributes();

	/**
	 * Get an attribute by its tag. This method returns {@code null} if the
	 * attribute doesn't exist.
	 *
	 * @param <E> The type of the attribute's value
	 * @param tag The attribute tag
	 * @return The attribute associated with the tag or {@code null}
	 */
	public <E> STAttribute<E> getAttribute(long tag);

	/**
	 * Overwrite the attribute associated with the given tag.
	 *
	 * @param tag       The attribute tag
	 * @param attribute The attribute to associate with the tag or {@code null}
	 */
	public void setAttribute(long tag, STAttribute<?> attribute);

	public default <E> STAttribute<E> attribute(RelativeOid<E> oid) {
		if (!oid.isConcrete())
			throw new GenericOidException(oid);

		if (oid.size() == 1) {
			switch (OidUtil.getOidType(oid.first())) {
			case OTYPE_ATTRIBUTE:
				return attribute(oid.first());
			}

			throw new IllegalArgumentException("Unacceptable attribute tag: " + oid.first());
		} else {
			switch (OidUtil.getOidType(oid.first())) {
			case OTYPE_DOCUMENT:
				if (this instanceof STDocumentContainer) {
					return (STAttribute<E>) (((STDocumentContainer) this).document(oid.first()).attribute(oid.tail()));
				}
				break;
			case OTYPE_COLLECTION:
				if (this instanceof STCollectionContainer) {
					return (STAttribute<E>) ((STCollectionContainer) this).collection(oid.first())
							.document(oid.tail().first()).attribute(oid.tail(2));
				}
				break;
			}
			throw new IllegalArgumentException("OID: " + oid);
		}
	}

	public default <T> STAttribute<T> get(RelativeOid.STAttributeOid<T> oid) {
		return attribute(oid);
	}

	public STAttribute<?> newAttribute();

	public void remove(STAttribute<?> attribute);

	public void forEachAttribute(Consumer<STAttribute<?>> consumer);
}
