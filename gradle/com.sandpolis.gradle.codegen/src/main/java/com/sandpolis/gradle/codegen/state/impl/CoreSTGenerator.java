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

import com.sandpolis.core.foundation.util.OidUtil;
import com.sandpolis.gradle.codegen.state.AttributeSpec;
import com.sandpolis.gradle.codegen.state.DocumentSpec;
import com.sandpolis.gradle.codegen.state.RelationSpec;
import com.sandpolis.gradle.codegen.state.STGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class CoreSTGenerator extends STGenerator {

	private void processChildren(TypeSpec.Builder documentClass, TypeSpec.Builder oidClass, DocumentSpec document,
			String oid) {
		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processCollection(documentClass, oidClass, subdocument,
						oid + "." + OidUtil.computeCollectionTag(entry.getKey()));
			}
		}
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processDocument(documentClass, oidClass, subdocument,
						oid + "." + OidUtil.computeDocumentTag(entry.getKey()));
			}
		}
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				processAttribute(documentClass, oidClass, entry.getValue(), oid + "."
						+ OidUtil.computeAttributeTag(entry.getKey(), entry.getValue().type, !entry.getValue().list));
			}
		}
		if (document.relations != null) {
			for (var entry : document.relations.entrySet()) {
				processRelation(documentClass, entry.getValue(), oid + "."
						+ OidUtil.computeAttributeTag(entry.getKey(), OidUtil.ATYPE_OID, entry.getValue().list));
			}
		}
	}

	protected void processAttribute(TypeSpec.Builder parent, TypeSpec.Builder parentOidClass, AttributeSpec attribute,
			String oid) {

		{
			// Add the attribute's getter method
			var method = MethodSpec
					.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL,
							(attribute.type.equals("java.lang.Boolean") ? "is_" : "get_") + attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(attribute.getAttributeType()) //
					.addStatement("return $L().get()", LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name));
			parent.addMethod(method.build());
		}

		{
			// Add the attribute's property method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(attribute.getAttributeObjectType()) //
					.addStatement("return document.attribute($LL)", oid.replaceAll(".*\\.", ""));
			parent.addMethod(method.build());
		}

		{
			// Add the attribute's OID field
			var field = FieldSpec
					.builder(ParameterizedTypeName.get(
							ClassName.get(ST_PACKAGE + ".oid", "AbsoluteOid", "STAttributeOid"),
							attribute.getAttributeType()), attribute.name, PUBLIC, FINAL)
					.initializer("new AbsoluteOid.STAttributeOid<>(\"$L\")", oid);

			parentOidClass.addField(field.build());
		}
	}

	protected void processCollection(TypeSpec.Builder parent, TypeSpec.Builder parentOidClass, DocumentSpec document,
			String oid) {
		var documentClass = TypeSpec.classBuilder(ST_PREFIX + document.shortName()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(ST_PACKAGE, "VirtObject"));

		var oidClass = TypeSpec.classBuilder("Oid").addModifiers(PUBLIC, STATIC).superclass(
				ParameterizedTypeName.get(ClassName.get(ST_PACKAGE + ".oid", "AbsoluteOid", "STCollectionOid"),
						ClassName.bestGuess(ST_PREFIX + document.shortName())));

		{
			// Add constructor
			var constructor = MethodSpec.constructorBuilder().addParameter(String.class, "oid")
					.addStatement("super(oid)");

			oidClass.addMethod(constructor.build());
		}

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "STDocument"), "document") //
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
					.returns(long.class) //
					.addStatement("return $T.computeDocumentTag($T.murmur3_128().newHasher()$L.hash().asLong())",
							OidUtil.class, ClassName.get("com.google.common.hash", "Hashing"), identityString);
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
			var field = FieldSpec
					.builder(ClassName.get(ST_PREFIX + document.shortName(), "Oid"), document.shortName().toLowerCase(),
							PUBLIC, FINAL) //
					.initializer("new $L(\"$L\")", ST_PREFIX + document.shortName() + ".Oid", oid);

			if (parentOidClass != null) {
				parentOidClass.addField(field.build());
			} else if (parent != null) {
				parent.addField(field.addModifiers(STATIC).build());
			}
		}

		// Process subdocuments and attributes
		processChildren(documentClass, oidClass, document, oid + ".0");

		documentClass.addType(oidClass.build());
		writeClass(documentClass.build());
	}

	protected void processDocument(TypeSpec.Builder parent, TypeSpec.Builder parentOidClass, DocumentSpec document,
			String oid) {

		var documentClass = TypeSpec.classBuilder(ST_PREFIX + document.shortName()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(ST_PACKAGE, "VirtObject"));

		var oidClass = TypeSpec.classBuilder("Oid").addModifiers(PUBLIC, STATIC).superclass(
				ParameterizedTypeName.get(ClassName.get(ST_PACKAGE + ".oid", "AbsoluteOid", "STDocumentOid"),
						ClassName.bestGuess(ST_PREFIX + document.shortName())));

		{
			// Add constructor
			var constructor = MethodSpec.constructorBuilder().addParameter(String.class, "oid")
					.addStatement("super(oid)");

			oidClass.addMethod(constructor.build());
		}

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "STDocument"), "document") //
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
					.returns(long.class) //
					.addStatement("return $LL", oid.replaceAll(".*\\.", ""));
			documentClass.addMethod(method.build());
		}

		{
			// Add OID field
			var field = FieldSpec
					.builder(ClassName.get(ST_PREFIX + document.shortName(), "Oid"), document.shortName().toLowerCase(),
							PUBLIC, FINAL) //
					.initializer("new $L(\"$L\")", ST_PREFIX + document.shortName() + ".Oid", oid);

			if (parentOidClass != null) {
				parentOidClass.addField(field.build());
			} else if (parent != null) {
				parent.addField(field.addModifiers(STATIC).build());
			}
		}

		// Process subdocuments and attributes
		processChildren(documentClass, parent == null ? null : oidClass, document, oid);

		{
			// Add document method
			var method = MethodSpec.methodBuilder(document.shortName().toLowerCase()) //
					.addModifiers(PUBLIC) //
					.returns(ClassName.bestGuess("Virt" + document.shortName())) //
					.addStatement("return new Virt$L(document.document($LL))", document.shortName(),
							oid.replaceAll(".*\\.", ""));

			if (parent != null)
				parent.addMethod(method.build());
		}

		documentClass.addType(oidClass.build());
		writeClass(documentClass.build());
	}

	protected void processRelation(TypeSpec.Builder parent, RelationSpec relation, String oid) {
		if (relation.list) {
			{
				// Add the getter method
				var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "get_" + relation.name)) //
						.addModifiers(PUBLIC) //
						.returns(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STRelation"),
								ClassName.bestGuess(STGenerator.ST_PREFIX + relation.simpleName()))) //
						.addStatement("return document.collection($LL).collectionList($L::new)",
								oid.replaceAll(".*\\.", ""), STGenerator.ST_PREFIX + relation.simpleName());
				parent.addMethod(method.build());
			}

		} else {
			{
				// Add the getter method
				var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "get_" + relation.name)) //
						.addModifiers(PUBLIC) //
						.returns(ClassName.bestGuess(STGenerator.ST_PREFIX + relation.simpleName())) //
						.addStatement("return new $L(document.document($LL))",
								STGenerator.ST_PREFIX + relation.simpleName(), oid.replaceAll(".*\\.", ""));
				parent.addMethod(method.build());
			}

			{
				// Add the setter method
				var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "set_" + relation.name)) //
						.addModifiers(PUBLIC) //
						.addParameter(ClassName.bestGuess(STGenerator.ST_PREFIX + relation.simpleName()), "v") //
						.addStatement("document.setDocument($LL, v.document)", oid.replaceAll(".*\\.", ""));
				parent.addMethod(method.build());
			}
		}
	}

	@Override
	protected void processRoot(DocumentSpec document, String oid) {
		processDocument(null, null, document, oid);
	}
}
