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
package com.sandpolis.gradle.codegen.state.impl;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.sandpolis.gradle.codegen.state.AttributeSpec;
import com.sandpolis.gradle.codegen.state.DocumentSpec;
import com.sandpolis.gradle.codegen.state.STGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for basic document bindings.
 */
public class CoreSTGenerator extends STGenerator {

	@Override
	public void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute, String oid) {

		// Add the attribute's getter method
		var getter = MethodSpec
				.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL,
						(attribute.type.equals("java.lang.Boolean") ? "is_" : "get_") + attribute.name)) //
				.addModifiers(PUBLIC) //
				.returns(attribute.getAttributeType()) //
				.addStatement("return $L().get()", LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name));
		parent.addMethod(getter.build());

		// Add the attribute's property method
		var property = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)) //
				.addModifiers(PUBLIC) //
				.returns(attribute.getAttributeObjectType()) //
				.addStatement("return document.attribute($L)", oid.replaceAll(".*\\.", ""));
		parent.addMethod(property.build());

		// Add the attribute's OID field
		parent.addField(newAttributeOidField(attribute, oid).build());
	}

	@Override
	public void processCollection(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		var documentClass = TypeSpec.classBuilder(DOCUMENT_PREFIX + document.name.replaceAll(".*\\.", "")) //
				.addModifiers(PUBLIC, STATIC) //
				.superclass(ClassName.get(DATA_PACKAGE, "StateObject"));

		// Add constructor
		documentClass.addMethod(newDocumentConstructor().build());

		// Add tag method
		documentClass.addMethod(newCollectionTagMethod(document).build());

		// Add ID getter
		documentClass.addMethod(newDocumentIdMethod().build());

		// Add OID field
		documentClass.addField(newCollectionOidField(ClassName.bestGuess(documentClass.build().name), oid).build());

		// Process subdocuments and attributes
		processChildren(documentClass, document, oid + ".0");

		parent.addType(documentClass.build());
	}

	@Override
	public void processDocument(TypeSpec.Builder parent, DocumentSpec document, String oid) {

		var documentName = DOCUMENT_PREFIX + document.name.replaceAll(".*\\.", "");

		var documentClass = TypeSpec.classBuilder(documentName) //
				.addModifiers(PUBLIC, STATIC) //
				.superclass(ClassName.get(DATA_PACKAGE, "StateObject"));

		// Add constructor
		documentClass.addMethod(newDocumentConstructor().build());

		// Add ID getter
		documentClass.addMethod(newDocumentIdMethod().build());

		// Add tag method
		documentClass.addMethod(newDocumentTagMethod(oid).build());

		// Add OID field
		documentClass.addField(newDocumentOidField(ClassName.bestGuess(documentClass.build().name), oid).build());

		// Process subdocuments and attributes
		processChildren(documentClass, document, oid);

		parent.addType(documentClass.build());

		// Add document method
		if (!document.name.equals("Profile")) {
			parent.addMethod(newDocumentMethod(document, oid).build());
		}
	}

	private FieldSpec.Builder newAttributeOidField(AttributeSpec attribute, String oid) {
		return FieldSpec
				.builder(ParameterizedTypeName.get(ClassName.get(DATA_PACKAGE + ".oid", "AttributeOid"),
						attribute.getAttributeType()), attribute.name.toUpperCase(), PUBLIC, STATIC, FINAL)
				.initializer("new AttributeOid<>(\"$L\")", oid);
	}

	private FieldSpec.Builder newDocumentOidField(TypeName documentType, String oid) {
		return FieldSpec
				.builder(ParameterizedTypeName.get(ClassName.get(DATA_PACKAGE + ".oid", "DocumentOid"), documentType),
						"DOCUMENT", PUBLIC, STATIC, FINAL)
				.initializer("new DocumentOid<>(\"$L\")", oid);
	}

	private FieldSpec.Builder newCollectionOidField(TypeName documentType, String oid) {
		return FieldSpec
				.builder(ParameterizedTypeName.get(ClassName.get(DATA_PACKAGE + ".oid", "CollectionOid"), documentType),
						"COLLECTION", PUBLIC, STATIC, FINAL)
				.initializer("new CollectionOid<>(\"$L\")", oid);
	}

	private MethodSpec.Builder newCollectionTagMethod(DocumentSpec document) {
		String identityString = "";
		if (document.attributes != null) {
			for (var attribute : document.attributes.values()) {
				if (attribute.identity) {
					switch (attribute.type) {
					case "java.lang.String":
						identityString += ".putBytes(" + LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)
								+ "().get().getBytes())";
						break;
					case "java.lang.Byte[]":
						identityString += ".putBytes(" + LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name) + "().get())";
						break;
					}
				}
			}
		}

		// If there were no explicit identity attributes, use the ID field
		if (identityString.isEmpty()) {
			identityString = ".putBytes(getId().getBytes())";
		}

		return MethodSpec.methodBuilder("tag") //
				.addAnnotation(Override.class) //
				.addModifiers(PUBLIC, FINAL) //
				.returns(int.class) //
				.addStatement("return $T.murmur3_32().newHasher()$L.hash().asInt()",
						ClassName.get("com.google.common.hash", "Hashing"), identityString);
	}

	/**
	 * Generate a constructor of the form:
	 * 
	 * <pre>
	 * <code>
	 * public Example(Document document) {
	 *     super(document);
	 * }
	 * </code>
	 * </pre>
	 * 
	 * @return A generated method
	 */
	private MethodSpec.Builder newDocumentConstructor() {
		return MethodSpec.constructorBuilder() //
				.addModifiers(PUBLIC) //
				.addParameter(DOCUMENT_TYPE, "document") //
				.addStatement("super(document)");
	}

	/**
	 * Generate a method of the form:
	 * 
	 * <pre>
	 * <code>
	 * public Example example() {
	 *     return new Example(document.document(4));
	 * }
	 * </code>
	 * </pre>
	 * 
	 * @return A generated method
	 */
	private MethodSpec.Builder newDocumentMethod(DocumentSpec document, String oid) {
		String shortName = document.name.replaceAll(".*\\.", "");

		return MethodSpec.methodBuilder(shortName.toLowerCase()) //
				.addModifiers(PUBLIC) //
				.returns(ClassName.bestGuess("Virt" + shortName)) //
				.addStatement("return new Virt$L(document.document($L))", shortName, oid.replaceAll(".*\\.", ""));
	}

	/**
	 * Generate a method called {@code tag} of the form:
	 * 
	 * <pre>
	 * <code>
	 * public final int tag() {
	 *     return 4;
	 * }
	 * </code>
	 * </pre>
	 * 
	 * @return A generated method
	 */
	private MethodSpec.Builder newDocumentTagMethod(String oid) {
		return MethodSpec.methodBuilder("tag") //
				.addAnnotation(Override.class) //
				.addModifiers(PUBLIC, FINAL) //
				.returns(int.class) //
				.addStatement("return $L", oid.replaceAll(".*\\.", ""));
	}

	/**
	 * Generate a method called {@code getId} of the form:
	 * 
	 * <pre>
	 * <code>
	 * public String getId() {
	 *     return document.getId();
	 * }
	 * </code>
	 * </pre>
	 * 
	 * @return A generated method
	 */
	private MethodSpec.Builder newDocumentIdMethod() {
		return MethodSpec.methodBuilder("getId") //
				.addModifiers(PUBLIC) //
				.returns(String.class) //
				.addStatement("return document.getId()");
	}

	private void processChildren(TypeSpec.Builder documentClass, DocumentSpec document, String oid) {
		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processCollection(documentClass, subdocument, oid + "." + entry.getKey());
			}
		}
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processDocument(documentClass, subdocument, oid + "." + entry.getKey());

			}
		}
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				processAttribute(documentClass, entry.getValue(), oid + "." + entry.getKey());
			}
		}
	}
}
