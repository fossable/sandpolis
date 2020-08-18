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

import javax.lang.model.element.Modifier;

import com.sandpolis.gradle.codegen.state.AttributeSpec;
import com.sandpolis.gradle.codegen.state.DocumentSpec;
import com.sandpolis.gradle.codegen.state.STGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

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

		{
			// Add the attribute's OID field
			var field = FieldSpec
					.builder(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "Oid", "AttributeOid"),
							attribute.getAttributeType()), attribute.name.toUpperCase(), PUBLIC, STATIC, FINAL)
					.initializer("new Oid.AttributeOid<>(\"$L\")", oid);

			parent.addField(field.build());
		}
	}

	@Override
	public void processCollection(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		var documentClass = TypeSpec.classBuilder(ST_PREFIX + document.name.replaceAll(".*\\.", "")) //
				.addModifiers(PUBLIC, STATIC) //
				.superclass(ClassName.get(ST_PACKAGE, "VirtObject"));

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "Document"), "document") //
					.addStatement("super(document)");
			documentClass.addMethod(method.build());
		}

		{
			// Add tag method
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
							identityString += ".putBytes(" + LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)
									+ "().get())";
							break;
						}
					}
				}
			}

			// If there were no explicit identity attributes, use the ID field
			if (identityString.isEmpty()) {
				identityString = ".putBytes(getId().getBytes())";
			}

			var method = MethodSpec.methodBuilder("tag") //
					.addAnnotation(Override.class) //
					.addModifiers(PUBLIC, FINAL) //
					.returns(int.class) //
					.addStatement("return $T.murmur3_32().newHasher()$L.hash().asInt()",
							ClassName.get("com.google.common.hash", "Hashing"), identityString);
			documentClass.addMethod(method.build());
		}

		{
			// Add ID getter
			var method = MethodSpec.methodBuilder("getId") //
					.addModifiers(PUBLIC) //
					.returns(String.class) //
					.addStatement("return document.getId()");
			documentClass.addMethod(method.build());
		}

		{
			// Add OID field
			var documentType = ClassName.bestGuess(documentClass.build().name);
			var field = FieldSpec
					.builder(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "Oid", "CollectionOid"), documentType),
							"COLLECTION", PUBLIC, STATIC, FINAL) //
					.initializer("new Oid.CollectionOid<>(\"$L\")", oid);

			documentClass.addField(field.build());
		}

		// Process subdocuments and attributes
		processChildren(documentClass, document, oid + ".0");

		parent.addType(documentClass.build());
	}

	@Override
	public void processDocument(TypeSpec.Builder parent, DocumentSpec document, String oid) {

		var documentName = ST_PREFIX + document.name.replaceAll(".*\\.", "");

		var documentClass = TypeSpec.classBuilder(documentName) //
				.addModifiers(PUBLIC, STATIC) //
				.superclass(ClassName.get(ST_PACKAGE, "VirtObject"));

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "Document"), "document") //
					.addStatement("super(document)");
			documentClass.addMethod(method.build());
		}

		{
			// Add ID getter
			var method = MethodSpec.methodBuilder("getId") //
					.addModifiers(PUBLIC) //
					.returns(String.class) //
					.addStatement("return document.getId()");
			documentClass.addMethod(method.build());
		}

		{
			// Add tag method
			var method = MethodSpec.methodBuilder("tag") //
					.addAnnotation(Override.class) //
					.addModifiers(PUBLIC, FINAL) //
					.returns(int.class) //
					.addStatement("return $L", oid.replaceAll(".*\\.", ""));
			documentClass.addMethod(method.build());
		}

		{
			// Add OID field
			var documentType = ClassName.bestGuess(documentClass.build().name);
			var field = FieldSpec
					.builder(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "Oid", "DocumentOid"), documentType),
							"DOCUMENT", PUBLIC, STATIC, FINAL) //
					.initializer("new Oid.DocumentOid<>(\"$L\")", oid);

			documentClass.addField(field.build());
		}

		// Process subdocuments and attributes
		processChildren(documentClass, document, oid);

		parent.addType(documentClass.build());

		{
			// Add document method
			var method = MethodSpec.methodBuilder(document.shortName().toLowerCase()) //
					.addModifiers(PUBLIC) //
					.returns(ClassName.bestGuess("Virt" + document.shortName())) //
					.addStatement("return new Virt$L(document.document($L))", document.shortName(),
							oid.replaceAll(".*\\.", ""));

			parent.addMethod(method.build());
		}
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