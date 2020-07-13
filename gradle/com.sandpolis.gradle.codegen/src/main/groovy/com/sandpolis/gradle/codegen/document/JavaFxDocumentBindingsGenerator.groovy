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
 * Generator for JavaFX document bindings.
 */
class JavaFxDocumentBindingsGenerator extends DocumentBindingsGenerator {

	void processCollection(parent, collection) {
		processDocument(parent, collection)
	}

	void processAttribute(parentClass, attribute) {
		if (!attribute.type.startsWith("java.lang"))
			return

		def type
		if (attribute.type.endsWith("[]")) {
			type = ClassName.bestGuess(attribute.type.replace("[]", "")).unbox()
			return
		} else {
			def components = attribute.type.split("<|>")
			if (components.length == 2) {
				type = ParameterizedTypeName.get(ClassName.bestGuess(components[0]), ClassName.bestGuess(components[1]))
				return
			} else {
				type = ClassName.bestGuess(attribute.type)
			}
		}

		def propertyType = ClassName.bestGuess("javafx.beans.property.${type.simpleName()}Property")

		// Add property field
		def propertyField = FieldSpec.builder(propertyType, attribute.name, PRIVATE)
			.initializer("new \$T()", ClassName.bestGuess("javafx.beans.property.Simple${type.simpleName()}Property"))
		parentClass.addField(propertyField.build())

		// Add property getter
		def propertyGetter = MethodSpec.methodBuilder(camel(attribute.name + "_property"))
			.addModifiers(PUBLIC)
			.returns(propertyType)
			.addStatement("return ${attribute.name}")
		parentClass.addMethod(propertyGetter.build())
	}

	void processDocument(parent, document) {

		def documentClass = TypeSpec.classBuilder("Fx" + document.name)
			.addModifiers(PUBLIC, STATIC)

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
