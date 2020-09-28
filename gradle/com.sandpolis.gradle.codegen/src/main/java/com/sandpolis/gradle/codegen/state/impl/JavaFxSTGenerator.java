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
import static javax.lang.model.element.Modifier.PUBLIC;

import com.sandpolis.gradle.codegen.state.AttributeSpec;
import com.sandpolis.gradle.codegen.state.DocumentSpec;
import com.sandpolis.gradle.codegen.state.OidUtil;
import com.sandpolis.gradle.codegen.state.RelationSpec;
import com.sandpolis.gradle.codegen.state.STGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

/**
 * Generator for JavaFX document bindings.
 */
public class JavaFxSTGenerator extends STGenerator {

	protected void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute, String oid) {

		{
			// Add property getter
			var type = ParameterizedTypeName.get(ClassName.get("javafx.beans.value", "ObservableValue"),
					attribute.getAttributeType());
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name + "_property")) //
					.addModifiers(PUBLIC) //
					.returns(type) //
					.addStatement("return ($T) document.attribute($L)", type, oid.replaceAll(".*\\.", ""));
			parent.addMethod(method.build());
		}
	}

	protected void processCollection(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		processDocument(parent, document, oid + ".0");
	}

	protected void processDocument(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		var documentClass = TypeSpec.classBuilder("Fx" + document.shortName()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(ST_PACKAGE, "Virt" + document.shortName()));

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "STDocument"), "document") //
					.addStatement("super(document)");
			documentClass.addMethod(method.build());
		}

		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processCollection(documentClass, subdocument, oid + "." + OidUtil.computeCollectionTag(entry.getKey()));
			}
		}
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processDocument(documentClass, subdocument, oid + "." + OidUtil.computeDocumentTag(entry.getKey()));
			}
		}
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				processAttribute(documentClass, entry.getValue(), oid + "."
						+ OidUtil.computeAttributeTag(entry.getKey(), entry.getValue().type, !entry.getValue().list));
			}
		}

		writeClass(documentClass.build());
	}

	protected void processRelation(Builder parent, RelationSpec relation, String oid) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processRoot(DocumentSpec document, String oid) {
		processDocument(null, document, oid);
	}
}
