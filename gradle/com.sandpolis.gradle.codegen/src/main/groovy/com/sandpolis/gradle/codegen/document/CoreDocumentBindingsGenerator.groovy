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

		// Add document field
		def field = FieldSpec.builder(DOCUMENT_TYPE, "document", PRIVATE)
		collectionClass.addField(field.build())

		// Add constructor
		def constructor = MethodSpec.constructorBuilder()
			.addModifiers(PUBLIC)
			.addParameter(DOCUMENT_TYPE, "document")
			.addStatement("this.document = document")
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

		def constants = ""
		if (collection.attributes != null) {
			for (def subattribute : collection.attributes) {
				if (subattribute.constant == true) {
					if (subattribute.type == "String") {
						constants += ".putBytes(${camel(subattribute.name)}().get().getBytes())"
					}
				}
			}
		}

		// Add tag method
		def tag = MethodSpec.methodBuilder("tag")
			.addModifiers(PUBLIC)
			.returns(int.class)
			.addStatement("return \$T.murmur3_32().newHasher()\$L.hash().asInt()", ClassName.bestGuess("com.google.common.hash.Hashing"), constants)
		collectionClass.addMethod(tag.build())

		parent.addType(collectionClass.build())
	}

	void processAttribute(parent, attribute) {

		def type
		if (attribute.type.endsWith("[]")) {
			type = ArrayTypeName.of(ClassName.bestGuess(attribute.type.replace("[]", "")).unbox())
		} else {
			type = ClassName.bestGuess(attribute.type)
		}

		def attributeType = ParameterizedTypeName.get(ClassName.bestGuess("com.sandpolis.core.instance.attribute2.Attribute"), type)

		// Add the attribute's getter method
		def getter = MethodSpec.methodBuilder(camel((attribute.type != "Boolean" ? "get_" : "is_") + attribute.name))
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

	}

	void processDocument(parent, document) {

		def documentClass = TypeSpec.classBuilder(document.name)
			.addModifiers(PUBLIC, STATIC)

		// Add document field
		def field = FieldSpec.builder(DOCUMENT_TYPE, "document", PRIVATE)
		documentClass.addField(field.build())

		// Add constructor
		def constructor = MethodSpec.constructorBuilder()
			.addModifiers(PUBLIC)
			.addParameter(DOCUMENT_TYPE, "document")
			.addStatement("this.document = document")
		documentClass.addMethod(constructor.build())

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
	}
}
