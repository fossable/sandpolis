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
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.util.Collection;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class CoreSTGenerator extends VSTGenerator {

	protected void processAttributeOid(TypeSpec.Builder oidClass, DocumentSpec document, AttributeSpec attribute) {

		{
			// Add the attribute's OID field
			var initializer = CodeBlock.of("new AbsoluteOid<>($LL, \"$L\")", namespace,
					document.name + "/" + attribute.name);

			// Add type data
			initializer = CodeBlock.of("$L.setData($T.TYPE, $T.class)", initializer,
					ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"), attribute.getAttributeType());

			// Add singularity data
			if (attribute.list) {
				initializer = CodeBlock.of("$L.setData($T.SINGULARITY, false)", initializer,
						ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"));
			}

			// Add identity data
			if (attribute.immutable) {
				initializer = CodeBlock.of("$L.setData($T.IMMUTABLE, true)", initializer,
						ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"));
			}

			// Add osquery data
			if (attribute.osquery != null) {
				initializer = CodeBlock.of("$L.setData($T.OSQUERY, \"$L\")", initializer,
						ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"), attribute.osquery);
			}

			var field = FieldSpec
					.builder(ParameterizedTypeName.get(ClassName.get(OID_PACKAGE, "AbsoluteOid"),
							ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"),
									attribute.getAttributeType())),
							attribute.name, PUBLIC, FINAL)
					.initializer(initializer);

			oidClass.addField(field.build());
		}
	}

	protected void processAttributeRelationList(TypeSpec.Builder parent, AttributeSpec attribute) {
		var type = ClassName.bestGuess(
				VST_PREFIX + LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, attribute.type.replaceAll(".*/", "")));
		{
			// Add the attribute's getter method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "get_" + attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(ParameterizedTypeName.get(ClassName.get(Collection.class), type)) //
					.addStatement("STAttribute<Oid> pointer = document.attribute(\"$L\")", attribute.name) //
					.addStatement("return null");
			parent.addMethod(method.build());
		}

		{
			// Add the attribute's setter method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "set_" + attribute.name)) //
					.addModifiers(PUBLIC) //
					.addParameter(type, "item") //
					.addStatement("document.attribute(\"$L\").set(item.oid())", attribute.name);
			parent.addMethod(method.build());
		}
	}

	protected void processAttributeRelation(TypeSpec.Builder parent, AttributeSpec attribute) {
		var type = ClassName.bestGuess(
				VST_PREFIX + LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, attribute.type.replaceAll(".*/", "")));
		{
			// Add the attribute's getter method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "get_" + attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(type) //
					.addStatement("STAttribute<Oid> pointer = document.attribute(\"$L\")", attribute.name) //
					.addStatement("return new $T($T.STStore.root().getDocument(pointer.get()))", type,
							ClassName.get("com.sandpolis.core.instance.state", "STStore"));
			parent.addMethod(method.build());
		}

		{
			// Add the attribute's setter method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "set_" + attribute.name)) //
					.addModifiers(PUBLIC) //
					.addParameter(type, "item") //
					.addStatement("document.attribute(\"$L\").set(item.oid())", attribute.name);
			parent.addMethod(method.build());
		}
	}

	protected void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute) {

		{
			// Add the attribute's getter method
			var method = MethodSpec
					.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL,
							(attribute.type.equals("java.lang.Boolean") ? "is_" : "get_") + attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(attribute.getAttributeType()) //
					.addStatement("return $L().get()", LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name));
			parent.addMethod(method.build());
		}

		if (attribute.id && !attribute.name.equals("id")) {
			// Add the attribute's id getter
			var method = MethodSpec.methodBuilder("getId") //
					.addModifiers(PUBLIC) //
					.returns(String.class);

			if (attribute.type.equals("java.lang.String")) {
				method.addStatement("return $L().get()", LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name));
			} else {
				method.addStatement("return String.valueOf($L().get())",
						LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name));
			}

			parent.addMethod(method.build());
		}

		{
			// Add the attribute's property method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(attribute.getAttributeObjectType()) //
					.addStatement("return document.attribute(\"$L\")", attribute.name);
			parent.addMethod(method.build());
		}

		if (attribute.id && !attribute.name.equals("id")) {
			// Add the attribute's id property method
			var method = MethodSpec.methodBuilder("id") //
					.addModifiers(PUBLIC) //
					.returns(attribute.getAttributeObjectType()) //
					.addStatement("return document.attribute(\"$L\")", attribute.name);
			parent.addMethod(method.build());
		}
	}

	protected void processDocumentOid(DocumentSpec document) {
		var parent = oidTypes.get(document.parentPath());

		var oidClass = TypeSpec.classBuilder("Oid").addModifiers(PUBLIC, STATIC).superclass(ParameterizedTypeName
				.get(ClassName.get(OID_PACKAGE, "AbsoluteOid"), ClassName.get(ST_PACKAGE, "STDocument")));

		{
			// Add constructor
			var constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC).addParameter(String.class, "path")
					.addStatement("super($LL, path)", namespace);

			oidClass.addMethod(constructor.build());
		}

		if (parent != null) {
			// Add OID field
			var field = FieldSpec
					.builder(ClassName.get(getProject().getName() + ".state", VST_PREFIX + document.className(), "Oid"),
							document.className().toLowerCase(), PUBLIC, FINAL) //
					.initializer("new $L(\"$L\")", VST_PREFIX + document.className() + ".Oid", document.name);

			parent.addField(field.build());
		}

		if (document.attributes != null) {
			for (var entry : document.attributes) {
				processAttributeOid(oidClass, document, entry);
			}
		}

		oidTypes.put(document.name.replaceAll("/+$", ""), oidClass);
	}

	@Override
	protected void processDocument(TypeSpec.Builder parent, DocumentSpec document) {

		var documentClass = TypeSpec.classBuilder(VST_PREFIX + document.className()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(VST_PACKAGE, "VirtDocument"));

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
				if (entry.type.contains("/")) {
					if (entry.list) {
						processAttributeRelationList(documentClass, entry);
					} else {
						processAttributeRelation(documentClass, entry);
					}
				} else {
					processAttribute(documentClass, entry);
				}
			}
		}

		if (parent != null) {
			if (document.name.endsWith("/")) {
				// Add collection field
				var field = FieldSpec.builder(
						ParameterizedTypeName.get(ClassName.get(VST_PACKAGE, "VirtCollection"),
								ClassName.bestGuess(VST_PREFIX + document.className())),
						document.className().toLowerCase(), PRIVATE);

				parent.addField(field.build());
			} else {
				// Add document field
				var field = FieldSpec.builder(ClassName.bestGuess(VST_PREFIX + document.className()),
						document.className().toLowerCase(), PRIVATE);

				parent.addField(field.build());
			}

			if (document.name.endsWith("/")) {
				// Add collection method
				var method = MethodSpec.methodBuilder(document.className().toLowerCase()) //
						.addModifiers(PUBLIC) //
						.returns(ParameterizedTypeName.get(ClassName.get(VST_PACKAGE, "VirtCollection"),
								ClassName.bestGuess(VST_PREFIX + document.className()))) //
						.addStatement("if ($L == null) $L = new VirtCollection<>(document.document(\"$L\"), $L::new)",
								document.className().toLowerCase(), document.className().toLowerCase(),
								document.basePath(), VST_PREFIX + document.className()) //
						.addStatement("return $L", document.className().toLowerCase());

				parent.addMethod(method.build());
			} else {
				// Add document method
				var method = MethodSpec.methodBuilder(document.className().toLowerCase()) //
						.addModifiers(PUBLIC) //
						.returns(ClassName.bestGuess(VST_PREFIX + document.className())) //
						.addStatement("if ($L == null) $L = new Virt$L(document.document(\"$L\"))",
								document.className().toLowerCase(), document.className().toLowerCase(),
								document.className(), document.basePath()) //
						.addStatement("return $L", document.className().toLowerCase());

				parent.addMethod(method.build());
			}
		}

		processDocumentOid(document);
		vstTypes.put(document.name.replaceAll("/+$", ""), documentClass);
	}
}
