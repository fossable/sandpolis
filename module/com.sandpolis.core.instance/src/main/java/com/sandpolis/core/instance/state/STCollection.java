package com.sandpolis.core.instance.state;

import java.util.function.Function;

import com.sandpolis.core.instance.State.ProtoCollection;

/**
 * A {@link STCollection} is an unordered set of {@link STDocument}s. Every
 * document has a unique non-zero "tag" which is a function of the document's
 * identity.
 *
 * @since 5.1.1
 */
public interface STCollection extends STObject<ProtoCollection> {

	public <T extends VirtObject> STRelation<T> collectionList(Function<STDocument, T> constructor);
}
