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
import static com.sandpolis.gradle.codegen.profile_tree.impl.Utils.toJavaFxProperty;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

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
 * Generator for JavaFX document bindings.
 */
public class JavaFxProfileTreeGenerator extends ProfileTreeGenerator {

	public void processCollection(TypeSpec.Builder parent, CollectionSpec collection) {
//		processDocument(parent, collection);
	}

	public void processAttribute(TypeSpec.Builder parentClass, AttributeSpec attribute) {
		if (!attribute.type.startsWith("java.lang"))
			return;

		TypeName type = Utils.toType(attribute.type);

		var propertyType = ClassName.get("javafx.beans.property", toJavaFxProperty(attribute.type));

		// Add property field
		var propertyField = FieldSpec.builder(propertyType, attribute.name, PRIVATE).initializer("new $T()",
				ClassName.get("javafx.beans.property", "Simple" + toJavaFxProperty(attribute.type)));
		parentClass.addField(propertyField.build());

		// Add property getter
		var propertyGetter = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name + "_property")) //
				.addModifiers(PUBLIC) //
				.returns(propertyType) //
				.addStatement("return $L", attribute.name);
		parentClass.addMethod(propertyGetter.build());
	}

	public void processDocument(TypeSpec.Builder parent, DocumentSpec document) {

		var documentClass = TypeSpec.classBuilder("Fx" + document.name) //
				.addModifiers(PUBLIC, STATIC);

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
	}
}
