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
package com.sandpolis.gradle.codegen.state;

import java.io.IOException;
import java.util.List;

import javax.lang.model.SourceVersion;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.sandpolis.gradle.codegen.ConfigExtension;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for virtual ST classes.
 */
public abstract class STGenerator extends DefaultTask {

	public static final String ST_PACKAGE = "com.sandpolis.core.instance.state";

	public static final String ST_PREFIX = "Virt";

	/**
	 * The elements of a VST tree flattened into a list.
	 */
	protected List<DocumentSpec> flatTree;

	@TaskAction
	public void action() throws Exception {

		// Load the schema
		flatTree = new ObjectMapper().readValue(
				((ConfigExtension) getProject().getExtensions().getByName("codegen")).stateTree,
				new TypeReference<List<DocumentSpec>>() {
				});

		// Check tree preconditions
		flatTree.forEach(this::validateDocument);

		// Calculate module namespace
		long namespace = OidUtil
				.computeDocumentTag(Hashing.murmur3_128().hashBytes(getProject().getName().getBytes()).asLong());

		// Find root document
		flatTree.stream().filter(document -> document.parent != null).findAny().ifPresent(document -> {
			if (document.parent.isEmpty()) {
				processRoot(document, String.valueOf(namespace));
			} else {
				processRoot(document, String.valueOf(namespace) + "." + document.parent);
			}
		});
	}

	/**
	 * Generate the given document from the specification.
	 */
	protected abstract void processRoot(DocumentSpec document, String oid);

	/**
	 * Assert the validity the given attribute.
	 * 
	 * @param attribute The attribute specification to validate
	 */
	protected void validateAttribute(AttributeSpec attribute) {

		// Name must be a valid Java identifier
		if (!SourceVersion.isIdentifier(attribute.name))
			throw new RuntimeException("Invalid attribute name: " + attribute.name);

		// Type must be present
		if (attribute.type == null || attribute.type.isEmpty())
			throw new RuntimeException("Missing type on attribute: " + attribute.name);
	}

	/**
	 * Assert the validity the given document.
	 * 
	 * @param document The document specification to validate
	 */
	protected void validateDocument(DocumentSpec document) {

		// Name must be a valid Java identifier
		for (var component : document.name.split("\\.")) {
			if (!SourceVersion.isIdentifier(component))
				throw new RuntimeException("Invalid document name: " + document.name);
		}

		// Validate sub-attributes
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on attribute: " + entry.getValue());

				validateAttribute(entry.getValue());
			}
		}

		// Validate sub-documents
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on document: " + entry.getValue());

				// Ensure sub-document exists
				if (flatTree.stream().filter(d -> entry.getValue().equals(d.name)).findAny().isEmpty())
					throw new RuntimeException("Failed to find document: " + entry.getValue());
			}
		}

		// Validate sub-collections
		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on collection: " + entry.getValue());
			}
		}

		// Validate sub-relations
		if (document.relations != null) {
			for (var entry : document.relations.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on relation: " + entry.getValue());
			}
		}
	}

	/**
	 * Write the given class to the appropriate generated sources directory.
	 * 
	 * @param spec The class specification
	 */
	protected void writeClass(TypeSpec spec) {
		try {
			JavaFile.builder(getProject().getName() + ".state", spec)
					.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
					.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
