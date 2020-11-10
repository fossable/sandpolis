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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for JavaFX document bindings.
 */
public class JavaFxSTGenerator extends VSTGenerator {

	protected void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute) {

		{
			// Add property getter
			var type = ParameterizedTypeName.get(ClassName.get("javafx.beans.value", "ObservableValue"),
					attribute.getAttributeType());
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name + "_property")) //
					.addModifiers(PUBLIC) //
					.returns(type) //
					.addStatement("return ($T) document.attribute(\"$L\")", type, attribute.name);
			parent.addMethod(method.build());
		}
	}

	@Override
	protected void processDocument(TypeSpec.Builder parent, DocumentSpec document) {
		var documentClass = TypeSpec.classBuilder("Fx" + document.className()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(VST_PACKAGE, VST_PREFIX + "Document"));

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
