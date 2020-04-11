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
package com.sandpolis.core.profile;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.profile.AttributeStore.AttributeStoreConfig;
import com.sandpolis.core.profile.AttributeStore.DocumentKey;
import com.sandpolis.core.profile.attribute.key.AttributeKey;

public class AttributeStore extends MapStore<String, DocumentKey, AttributeStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(AttributeStore.class);

	public AttributeStore() {
		super(log);
	}

	public void register(Class<?> cls) throws Exception {
		checkArgument(cls.getSimpleName().startsWith("AK_"));

		DocumentKey document = get(cls.getPackageName()).orElse(null);
		if (document == null) {
			document = new DocumentKey();
			add(document);
		}

		if (cls.isAnnotationPresent(Document.class)) {
			registerDocument(document, cls);
		} else if (cls.isAnnotationPresent(Collection.class)) {
			registerCollection(document, cls);
		} else {
			throw new RuntimeException();
		}
	}

	private void registerDocument(DocumentKey parent, Class<?> cls) throws Exception {

		DocumentKey d = new DocumentKey();
		parent.documents.put(cls.getSimpleName().toLowerCase(), d);

		for (var field : cls.getDeclaredFields()) {
			d.attributes.put(field.getName().toLowerCase(), (AttributeKey<?>) field.get(null));
		}

		for (var nested : cls.getClasses()) {
			if (nested.isAnnotationPresent(Document.class)) {
				registerDocument(d, nested);
			} else if (nested.isAnnotationPresent(Collection.class)) {
				registerCollection(d, nested);
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void registerCollection(DocumentKey parent, Class<?> cls) throws Exception {

		CollectionKey c = new CollectionKey();
		parent.collections.put(cls.getSimpleName().toLowerCase(), c);
		DocumentKey d = new DocumentKey();
		c.documents.put("_", d);

		for (var field : cls.getDeclaredFields()) {
			d.attributes.put(field.getName().toLowerCase(), (AttributeKey<?>) field.get(null));
		}

		for (var nested : cls.getClasses()) {
			if (nested.isAnnotationPresent(Document.class)) {
				registerDocument(d, nested);
			} else if (nested.isAnnotationPresent(Collection.class)) {
				registerCollection(d, nested);
			} else {
				throw new RuntimeException();
			}
		}
	}

	static class DocumentKey {
		Map<String, CollectionKey> collections = new HashMap<>();
		Map<String, DocumentKey> documents = new HashMap<>();
		Map<String, AttributeKey<?>> attributes = new HashMap<>();
	}

	static class CollectionKey {
		Map<String, DocumentKey> documents = new HashMap<>();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface Document {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface Collection {

	}

	@Override
	public AttributeStore init(Consumer<AttributeStoreConfig> configurator) {
		var config = new AttributeStoreConfig();
		configurator.accept(config);

		return (AttributeStore) super.init(null);
	}

	public final class AttributeStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(DocumentKey.class, DocumentKey::toString);
		}
	}

	public static final AttributeStore AttributeStore = new AttributeStore();
}
