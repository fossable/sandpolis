//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.gradle.codegen.state;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static javax.lang.model.element.Modifier.*;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for JavaFX document bindings.
 */
public class JavaFxSTGenerator extends VSTGenerator {

	protected void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute) {

		var type = ParameterizedTypeName.get(ClassName.get("javafx.beans.value", "ObservableValue"),
				attribute.getAttributeType());

		{
			// Add property field

			var field = FieldSpec
					.builder(type, LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name + "_property"), PUBLIC, FINAL) //
					.initializer("($T) document.attribute(\"$L\")", type, attribute.name);
			parent.addField(field.build());
		}
	}

	@Override
	protected void processDocument(TypeSpec.Builder parent, DocumentSpec document) {
		var documentClass = TypeSpec.classBuilder("Fx" + document.className()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get("com.sandpolis.core.instance.state", VST_PREFIX + document.className()));

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "STDocument"), "document") //
					.addStatement("super(document)");
			documentClass.addMethod(method.build());
		}

		if (document.attributes != null) {
			for (var entry : document.attributes) {
				processAttribute(documentClass, entry);
			}
		}

		vstTypes.put(document.name, documentClass);
	}

}
