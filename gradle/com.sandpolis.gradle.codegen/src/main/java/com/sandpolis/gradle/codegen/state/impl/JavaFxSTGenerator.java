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

import com.sandpolis.gradle.codegen.state.AttributeSpec;
import com.sandpolis.gradle.codegen.state.DocumentSpec;
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

	@Override
	public void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute, String oid) {

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

	@Override
	public void processCollection(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		oid = oid + ".0";

		var documentClass = TypeSpec.classBuilder("Fx" + document.name.replaceAll(".*\\.", "")) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(ST_PACKAGE, "VirtObject"));

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
									+ "Property().getValue().getBytes())";
							break;
						case "java.lang.Byte[]":
							identityString += ".putBytes(" + LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)
									+ "Property().getValue())";
							break;
						}
					}
				}
			}

			// If there were no explicit identity attributes, use the ID field
			if (identityString.isEmpty()) {
				identityString = ".putBytes(\"\".getBytes())";
			}

			var method = MethodSpec.methodBuilder("tag") //
					.addAnnotation(Override.class) //
					.addModifiers(PUBLIC, FINAL) //
					.returns(int.class) //
					.addStatement("return ($T.murmur3_32().newHasher()$L.hash().asInt() << 2) | 1",
							ClassName.get("com.google.common.hash", "Hashing"), identityString);
			documentClass.addMethod(method.build());
		}

		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processCollection(documentClass, subdocument,
						oid + "." + CoreSTGenerator.addTagType(entry.getKey(), 2));
			}
		}
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processDocument(documentClass, subdocument, oid + "." + CoreSTGenerator.addTagType(entry.getKey(), 1));
			}
		}
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				processAttribute(documentClass, entry.getValue(),
						oid + "." + CoreSTGenerator.addTagType(entry.getKey(), 1));
			}
		}

		writeClass(documentClass.build());
	}

	@Override
	public void processDocument(TypeSpec.Builder parent, DocumentSpec document, String oid) {
		var documentClass = TypeSpec.classBuilder("Fx" + document.name.replaceAll(".*\\.", "")) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(ST_PACKAGE, "VirtObject"));

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
			var method = MethodSpec.methodBuilder("tag") //
					.addAnnotation(Override.class) //
					.addModifiers(PUBLIC, FINAL) //
					.returns(int.class) //
					.addStatement("return $L", oid.replaceAll(".*\\.", ""));
			documentClass.addMethod(method.build());
		}

		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processCollection(documentClass, subdocument,
						oid + "." + CoreSTGenerator.addTagType(entry.getKey(), 2));
			}
		}
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processDocument(documentClass, subdocument, oid + "." + CoreSTGenerator.addTagType(entry.getKey(), 1));
			}
		}
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				processAttribute(documentClass, entry.getValue(),
						oid + "." + CoreSTGenerator.addTagType(entry.getKey(), 1));
			}
		}

		writeClass(documentClass.build());
	}

	@Override
	public void processRelation(Builder parent, RelationSpec relation, String oid) {
		// TODO Auto-generated method stub

	}
}
