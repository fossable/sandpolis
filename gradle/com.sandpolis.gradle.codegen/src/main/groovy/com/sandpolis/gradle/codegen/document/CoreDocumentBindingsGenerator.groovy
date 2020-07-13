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
package com.sandpolis.gradle.codegen.document

import static javax.lang.model.element.Modifier.*

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec

/**
 * Generator for basic document bindings.
 */
class CoreDocumentBindingsGenerator extends DocumentBindingsGenerator {

	void processCollection(parent, collection) {
		def collectionClass = TypeSpec.classBuilder(collection.name)
			.addModifiers(PUBLIC, STATIC)
			.superclass(ClassName.bestGuess("com.sandpolis.core.instance.data.StateObject"))

		// Add constructor
		def constructor = MethodSpec.constructorBuilder()
			.addModifiers(PUBLIC)
			.addParameter(DOCUMENT_TYPE, "document")
			.addStatement("super(document)")
		collectionClass.addMethod(constructor.build())

		if (collection.collections != null) {
			for (def subcollection : collection.collections) {
				processCollection(collectionClass, subcollection)
			}
		}
		if (collection.documents != null) {
			for (def subdocument : collection.documents) {
				processDocument(collectionClass, subdocument)
			}
		}
		if (collection.attributes != null) {
			for (def subattribute : collection.attributes) {
				processAttribute(collectionClass, subattribute)
			}
		}

		// Add tag method
		def identity = ""
		if (collection.attributes != null) {
			for (def subattribute : collection.attributes.sort{it.tag}) {
				if (subattribute.identity == true) {
					switch (subattribute.type) {
					case "java.lang.String":
						identity += ".putBytes(${camel(subattribute.name)}().get().getBytes())"
						break
					case "java.lang.Byte[]":
						identity += ".putBytes(${camel(subattribute.name)}().get())"
						break
					}
				}
			}
		}

		// If there are no explicit identity attributes, use the database ID
		if (identity.isEmpty()) {
			identity = ".putBytes(getId().getBytes())"
		}

		def tag = MethodSpec.methodBuilder("tag")
			.addAnnotation(Override.class)
			.addModifiers(PUBLIC, FINAL)
			.returns(int.class)
			.addStatement("return \$T.murmur3_32().newHasher()\$L.hash().asInt()", ClassName.bestGuess("com.google.common.hash.Hashing"), identity)
		collectionClass.addMethod(tag.build())

		// Add ID getter
		def id = MethodSpec.methodBuilder("getId")
			.addModifiers(PUBLIC)
			.returns(String.class)
			.addStatement("return document.getId()")
		collectionClass.addMethod(id.build())

		parent.addType(collectionClass.build())
	}

	void processAttribute(parent, attribute) {

		def type
		if (attribute.type.endsWith("[]")) {
			type = ArrayTypeName.of(ClassName.bestGuess(attribute.type.replace("[]", "")).unbox())
		} else {
			def components = attribute.type.split("<|>")
			if (components.length == 2) {
				type = ParameterizedTypeName.get(ClassName.bestGuess(components[0]), ClassName.bestGuess(components[1]))
			} else {
				type = ClassName.bestGuess(attribute.type)
			}
		}

		def attributeType = ParameterizedTypeName.get(ClassName.bestGuess("com.sandpolis.core.instance.data.Attribute"), type)

		// Add the attribute's getter method
		def getter = MethodSpec.methodBuilder(camel((attribute.type != "java.lang.Boolean" ? "get_" : "is_") + attribute.name))
			.addModifiers(PUBLIC)
			.returns(type)
			.addStatement("return ${camel(attribute.name)}().get()")
		parent.addMethod(getter.build())

		// Add the attribute's property method
		def property = MethodSpec.methodBuilder(camel(attribute.name))
			.addModifiers(PUBLIC)
			.returns(attributeType)
			.addStatement("return document.attribute(${attribute.tag})")
		parent.addMethod(property.build())

		// Add the attribute's static tag field
		def tagField = FieldSpec.builder(int.class, "${attribute.name.toUpperCase()}", PUBLIC, STATIC, FINAL)
			.initializer("${attribute.tag}")
		parent.addField(tagField.build())
	}

	void processDocument(parent, document) {

		def documentClass = TypeSpec.classBuilder(document.name)
			.addModifiers(PUBLIC, STATIC)
			.superclass(ClassName.bestGuess("com.sandpolis.core.instance.data.StateObject"))

		// Add constructor
		def constructor = MethodSpec.constructorBuilder()
			.addModifiers(PUBLIC)
			.addParameter(DOCUMENT_TYPE, "document")
			.addStatement("super(document)")
		documentClass.addMethod(constructor.build())

		// Add ID getter
		def id = MethodSpec.methodBuilder("getId")
			.addModifiers(PUBLIC)
			.returns(String.class)
			.addStatement("return document.getId()")
		documentClass.addMethod(id.build())

		// Add tag method
		def tag = MethodSpec.methodBuilder("tag")
			.addAnnotation(Override.class)
			.addModifiers(PUBLIC, FINAL)
			.returns(int.class)
			.addStatement("return ${document.tag}")
		documentClass.addMethod(tag.build())

		if (document.collections != null) {
			for (def subcollection : document.collections) {
				processCollection(documentClass, subcollection)
			}
		}
		if (document.documents != null) {
			for (def subdocument : document.documents) {
				processDocument(documentClass, subdocument)
			}
		}
		if (document.attributes != null) {
			for (def subattribute : document.attributes) {
				processAttribute(documentClass, subattribute)
			}
		}

		parent.addType(documentClass.build())

		// Add document method
		def documentMethod = MethodSpec.methodBuilder(document.name.toLowerCase())
			.addModifiers(PUBLIC)
			.returns(ClassName.bestGuess(document.name))
			.addStatement("return new ${document.name}(document.document(${document.tag}))")
		parent.addMethod(documentMethod.build())
	}
}
