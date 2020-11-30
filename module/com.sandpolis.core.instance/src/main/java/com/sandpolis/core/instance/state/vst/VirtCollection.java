package com.sandpolis.core.instance.state.vst;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument;

public class VirtCollection<V extends VirtDocument> implements VirtObject {

	private Map<String, V> documents;

	private final STDocument collection;

	private final Function<STDocument, V> constructor;

	public VirtCollection(STDocument collection, Function<STDocument, V> constructor) {
		this.collection = collection;
		this.constructor = constructor;
		this.documents = new HashMap<>();

		collection.forEachDocument(document -> {
			constructor.apply(document);
		});
	}

	public Collection<V> values() {
		return documents.values();
	}

	public <T extends V> V add(Consumer<V> configurator, Function<STDocument, T> vconstructor) {

		// Create a temporary document and apply the configuration to it
		var ephemeralDocument = new EphemeralDocument();
		var ephemeralElement = constructor.apply(ephemeralDocument);
		configurator.accept(ephemeralElement);

		// Generate ID if needed
		if (!ephemeralElement.id().isPresent()) {
			ephemeralElement.id().set(UUID.randomUUID().toString());
		}

		// Copy temporary document to the ST tree
		var document = collection.document(ephemeralElement.getId());
		document.copyFrom(ephemeralDocument);

		// Create element in the VST tree
		var element = vconstructor.apply(document);
		documents.put(ephemeralElement.getId(), element);
		return element;
	}

	public long count() {
		return documents.size();
	}

	public Optional<V> get(String path) {
		return Optional.ofNullable(documents.get(path));
	}
}
