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
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.sandpolis.gradle.codegen.state.AttributeSpec;
import com.sandpolis.gradle.codegen.state.DocumentSpec;
import com.sandpolis.gradle.codegen.state.STGenerator;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for JavaFX document bindings.
 */
public class JavaFxSTGenerator extends STGenerator {

	@Override
	public void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute, String oid) {

		{
			// Add property field
			var field = FieldSpec.builder(attribute.getJavaFxPropertyType(), attribute.name, PRIVATE, FINAL)
					.initializer("new $T(this, \"$L\")", attribute.getJavaFxSimplePropertyType(),
							attribute.name.toLowerCase());
			parent.addField(field.build());
		}

		{
			// Add property getter
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name + "_property")) //
					.addModifiers(PUBLIC) //
					.returns(attribute.getJavaFxPropertyType()) //
					.addStatement("return $L", attribute.name);
			parent.addMethod(method.build());
		}
	}

	@Override
	public void processCollection(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		processDocument(parent, document, oid + ".0");
	}

	@Override
	public void processDocument(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		var documentClass = TypeSpec.classBuilder("Fx" + document.name.replaceAll(".*\\.", "")) //
				.addModifiers(PUBLIC, STATIC);

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

		parent.addType(documentClass.build());
	}
}
