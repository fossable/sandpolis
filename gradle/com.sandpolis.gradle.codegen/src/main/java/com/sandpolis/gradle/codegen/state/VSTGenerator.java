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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.SourceVersion;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.common.hash.Hashing;
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
		 * Whether the attribute can be used as a unique identifier.
		 */
		public boolean id;

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
		 * The corresponding osquery identifier.
		 */
		public String osquery;

		/**
		 * The attribute's fully-qualified Java type or OID reference type.
		 */
		public String type;

		public TypeName getAttributeObjectType() {
			return ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"), getAttributeType());
		}

		public TypeName getAttributeType() {

			if (type.contains("/")) {
				// The attribute is a relation
				return ClassName.get(OID_PACKAGE, "Oid");
			} else if (type.endsWith("[]")) {
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
		 * The document's attributes.
		 */
		public List<AttributeSpec> attributes;

		/**
		 * The fully qualified document name.
		 */
		public String name;

		public String basePath() {
			var components = name.split("/");
			return components[components.length - 1];
		}

		public String className() {
			var components = name.split("/");
			return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, components[components.length - 1]);
		}

		public String parentPath() {
			return name.replaceAll("/+[^/]+/*$", "");
		}
	}

	public static final String OID_PACKAGE = "com.sandpolis.core.instance.state.oid";
	public static final String ST_PACKAGE = "com.sandpolis.core.instance.state.st";

	public static final String VST_PACKAGE = "com.sandpolis.core.instance.state.vst";

	/**
	 * The naming prefix for generated classes.
	 */
	public static final String VST_PREFIX = "Virt";

	// OidUtil duplicate!
	private static long computeDocumentTag(long raw) {
		return ((raw << 2) | 0) & Long.MAX_VALUE;
	}

	// OidUtil duplicate!
	private static long computeNamespace(String id) {
		return computeDocumentTag(Hashing.murmur3_128().newHasher().putBytes(id.getBytes()).hash().asLong());
	}

	/**
	 * The elements of a VST tree flattened into a list.
	 */
	protected List<DocumentSpec> flatTree;

	// Determine module namespace
	protected final long namespace = computeNamespace(getProject().getName());

	protected Map<String, TypeSpec.Builder> oidTypes = new HashMap<>();

	protected File stateTree;

	protected Map<String, TypeSpec.Builder> vstTypes = new HashMap<>();

	@TaskAction
	public void action() throws Exception {

		// Load the schema
		stateTree = ((ConfigExtension) getProject().getExtensions().getByName("codegen")).stateTree;
		flatTree = new ObjectMapper().readValue(stateTree, new TypeReference<List<DocumentSpec>>() {
		});

		// Check tree preconditions
		flatTree.forEach(this::validateDocument);

		// Generate classes
		flatTree.forEach(document -> {
			var vstParent = vstTypes.get(document.parentPath());

			processDocument(vstParent, document);
		});

		vstTypes.forEach((path, type) -> {
			var oidClass = oidTypes.get(path);
			if (oidClass != null) {
				type.addType(oidClass.build());
			}
			writeClass(type.build());
		});
	}

	protected abstract void processDocument(TypeSpec.Builder parent, DocumentSpec document);

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

		// Name must be a valid Java identifier or OID path
		if (document.name.contains("/")) {
			// TODO
		} else {
			for (var component : document.name.split("\\.")) {
				if (!SourceVersion.isIdentifier(component))
					throw new RuntimeException("Invalid document name: " + document.name);
			}
		}

		// Validate sub-attributes
		if (document.attributes != null) {
			document.attributes.forEach(this::validateAttribute);

			long idCount = document.attributes.stream().map(spec -> spec.id).filter(id -> id).count();
			if (idCount > 1) {
				throw new RuntimeException("More than one attribute marked with 'id'");
			} else if (idCount == 0) {
				// Add id field
				var id = new AttributeSpec();
				id.name = "id";
				id.type = "java.lang.String";
				id.id = true;
				document.attributes.add(id);
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
