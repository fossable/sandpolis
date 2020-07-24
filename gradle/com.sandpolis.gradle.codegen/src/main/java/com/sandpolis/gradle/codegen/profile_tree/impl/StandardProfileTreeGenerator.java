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
package com.sandpolis.gradle.codegen.profile_tree.impl;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.util.Comparator;

import com.sandpolis.gradle.codegen.profile_tree.AttributeSpec;
import com.sandpolis.gradle.codegen.profile_tree.CollectionSpec;
import com.sandpolis.gradle.codegen.profile_tree.DocumentSpec;
import com.sandpolis.gradle.codegen.profile_tree.ProfileTreeGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for basic document bindings.
 */
public class StandardProfileTreeGenerator extends ProfileTreeGenerator {

	public void processCollection(TypeSpec.Builder parent, CollectionSpec collection) {
		var collectionClass = TypeSpec.classBuilder(collection.name) //
				.addModifiers(PUBLIC, STATIC) //
				.superclass(ClassName.get("com.sandpolis.core.instance.data", "StateObject"));

		// Add constructor
		var constructor = MethodSpec.constructorBuilder() //
				.addModifiers(PUBLIC) //
				.addParameter(DOCUMENT_TYPE, "document") //
				.addStatement("super(document)");
		collectionClass.addMethod(constructor.build());

		if (collection.collections != null) {
			for (var subcollection : collection.collections) {
				processCollection(collectionClass, subcollection);
			}
		}
		if (collection.documents != null) {
			for (var subdocument : collection.documents) {
				processDocument(collectionClass, subdocument);
			}
		}
		if (collection.attributes != null) {
			for (var subattribute : collection.attributes) {
				processAttribute(collectionClass, subattribute);
			}
		}

		// Add tag method
		var identity = "";
		if (collection.attributes != null) {
			collection.attributes.sort(Comparator.comparingInt(AttributeSpec::tag));
			for (var subattribute : collection.attributes) {
				if (subattribute.identity) {
					switch (subattribute.type) {
					case "java.lang.String":
						identity += ".putBytes(${camel(subattribute.name)}().get().getBytes())";
						break;
					case "java.lang.Byte[]":
						identity += ".putBytes(${camel(subattribute.name)}().get())";
						break;
					}
				}
			}
		}

		// If there are no explicit identity attributes, use the database ID
		if (identity.isEmpty()) {
			identity = ".putBytes(getId().getBytes())";
		}

		var tag = MethodSpec.methodBuilder("tag") //
				.addAnnotation(Override.class) //
				.addModifiers(PUBLIC, FINAL) //
				.returns(int.class) //
				.addStatement("return $T.murmur3_32().newHasher()$L.hash().asInt()",
						ClassName.get("com.google.common.hash", "Hashing"), identity);
		collectionClass.addMethod(tag.build());

		// Add ID getter
		var id = MethodSpec.methodBuilder("getId") //
				.addModifiers(PUBLIC) //
				.returns(String.class) //
				.addStatement("return document.getId()");
		collectionClass.addMethod(id.build());

		parent.addType(collectionClass.build());
	}

	public void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute) {

		TypeName type = Utils.toType(attribute.type);
		ClassName attrImplType = Utils.toAttributeType(attribute.type);

		var attributeType = ParameterizedTypeName.get(ClassName.get("com.sandpolis.core.instance.data", "Attribute"),
				type);

		// Add the attribute's getter method
		var getter = MethodSpec
				.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL,
						(attribute.type.equals("java.lang.Boolean") ? "is_" : "get_") + attribute.name)) //
				.addModifiers(PUBLIC) //
				.returns(type) //
				.addStatement("return $L().get()", LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name));
		parent.addMethod(getter.build());

		// Add the attribute's property method
		var property = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)) //
				.addModifiers(PUBLIC) //
				.returns(attributeType) //
				.addStatement("return document.attribute($L, $T::new)", attribute.tag, attrImplType);
		parent.addMethod(property.build());

		// Add the attribute's static tag field
		var tagField = FieldSpec.builder(int.class, attribute.name.toUpperCase(), PUBLIC, STATIC, FINAL)
				.initializer("$L", attribute.tag);
		parent.addField(tagField.build());
	}

	public void processDocument(TypeSpec.Builder parent, DocumentSpec document) {

		var documentClass = TypeSpec.classBuilder(document.name) //
				.addModifiers(PUBLIC, STATIC) //
				.superclass(ClassName.get("com.sandpolis.core.instance.data", "StateObject"));

		// Add constructor
		var constructor = MethodSpec.constructorBuilder() //
				.addModifiers(PUBLIC) //
				.addParameter(DOCUMENT_TYPE, "document") //
				.addStatement("super(document)");
		documentClass.addMethod(constructor.build());

		// Add ID getter
		var id = MethodSpec.methodBuilder("getId") //
				.addModifiers(PUBLIC) //
				.returns(String.class) //
				.addStatement("return document.getId()");
		documentClass.addMethod(id.build());

		// Add tag method
		var tag = MethodSpec.methodBuilder("tag") //
				.addAnnotation(Override.class) //
				.addModifiers(PUBLIC, FINAL) //
				.returns(int.class) //
				.addStatement("return $L", document.tag);
		documentClass.addMethod(tag.build());

		if (document.collections != null) {
			for (var subcollection : document.collections) {
				processCollection(documentClass, subcollection);
			}
		}
		if (document.documents != null) {
			for (var subdocument : document.documents) {
				processDocument(documentClass, subdocument);
			}
		}
		if (document.attributes != null) {
			for (var subattribute : document.attributes) {
				processAttribute(documentClass, subattribute);
			}
		}

		parent.addType(documentClass.build());

		// Add document method
		var documentMethod = MethodSpec.methodBuilder(document.name.toLowerCase()) //
				.addModifiers(PUBLIC) //
				.returns(ClassName.bestGuess(document.name)) //
				.addStatement("return new $L(document.document($L))", document.name, document.tag);
		parent.addMethod(documentMethod.build());
	}
}
