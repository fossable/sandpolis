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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import javax.lang.model.SourceVersion;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandpolis.core.foundation.util.OidUtil;
import com.sandpolis.gradle.codegen.ConfigExtension;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for virtual state tree (VST) classes.
 */
public abstract class VSTGenerator extends DefaultTask {

	public static class AttributeSpec {

		/**
		 * A description of the attribute for code documentation.
		 */
		public String description;

		/**
		 * Whether the attribute cannot be modified after initially set.
		 */
		public boolean immutable;

		/**
		 * Whether the attribute is a list type.
		 */
		public boolean list;

		/**
		 * The attribute's name.
		 */
		public String name;

		/**
		 * The attribute's fully-qualified Java type.
		 */
		public String type;

		public TypeName getAttributeObjectType() {
			return ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"), getAttributeType());
		}

		public TypeName getAttributeType() {

			if (type.endsWith("[]")) {
				return ArrayTypeName.of(ClassName.bestGuess(type.replace("[]", "")).unbox());
			} else {
				return ClassName.bestGuess(type);
			}
		}

		public String simpleName() {
			return type.replaceAll(".*\\.", "");
		}
	}

	public static class DocumentSpec {

		/**
		 * The document's attributes sorted by tag.
		 */
		public TreeMap<Integer, AttributeSpec> attributes;

		/**
		 * The document's sub-collections sorted by tag.
		 */
		public TreeMap<Integer, String> collections;

		/**
		 * The document's sub-documents sorted by tag.
		 */
		public TreeMap<Integer, String> documents;

		/**
		 * The fully qualified document name.
		 */
		public String name;

		/**
		 * The OID of the parent document.
		 */
		public String parent;

		/**
		 * The document's relations sorted by tag.
		 */
		public TreeMap<Integer, RelationSpec> relations;

		public String shortName() {
			return name.replaceAll(".*\\.", "");
		}
	}

	public static class RelationSpec {

		public boolean list;

		public String name;

		public String type;

		public String simpleName() {
			return type.replaceAll(".*\\.", "");
		}
	}

	public static final String OID_PACKAGE = "com.sandpolis.core.instance.state.oid";
	public static final String ST_PACKAGE = "com.sandpolis.core.instance.state.st";

	public static final String VST_PACKAGE = "com.sandpolis.core.instance.state.vst";

	/**
	 * The naming prefix for generated classes.
	 */
	public static final String VST_PREFIX = "Virt";

	/**
	 * The elements of a VST tree flattened into a list.
	 */
	protected List<DocumentSpec> flatTree;

	protected File stateTree;

	@TaskAction
	public void action() throws Exception {

		// Load the schema
		stateTree = ((ConfigExtension) getProject().getExtensions().getByName("codegen")).stateTree;
		flatTree = new ObjectMapper().readValue(stateTree, new TypeReference<List<DocumentSpec>>() {
		});

		// Check tree preconditions
		flatTree.forEach(this::validateDocument);

		// Determine module namespace
		long namespace = OidUtil.computeNamespace(getProject().getName());

		// Generate classes
		flatTree.forEach(document -> {
			if (document.parent != null) {
				processRoot(document,
						String.valueOf(namespace) + (document.parent.isEmpty() ? "" : "." + document.parent));
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

		// Name must be specified
		if (document.name == null) {
			throw new RuntimeException("Missing document name (" + stateTree.getAbsolutePath() + ")");
		}

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
