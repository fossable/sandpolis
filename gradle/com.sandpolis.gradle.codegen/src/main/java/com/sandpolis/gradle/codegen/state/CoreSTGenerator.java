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

import com.sandpolis.core.foundation.util.OidUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class CoreSTGenerator extends VSTGenerator {

	private void processChildren(TypeSpec.Builder documentClass, TypeSpec.Builder oidClass, DocumentSpec document,
			String oid) {
		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processCollection(documentClass, oidClass, subdocument,
						oid + "." + OidUtil.computeCollectionTag(entry.getKey()));
			}
		}
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				var subdocument = flatTree.stream().filter(spec -> spec.name.equals(entry.getValue())).findAny().get();
				processDocument(documentClass, oidClass, subdocument,
						oid + "." + OidUtil.computeDocumentTag(entry.getKey()));
			}
		}
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				processAttribute(documentClass, oidClass, entry.getValue(),
						oid + "." + OidUtil.computeAttributeTag(entry.getKey()));
			}
		}
		if (document.relations != null) {
			for (var entry : document.relations.entrySet()) {
				processRelation(documentClass, entry.getValue(),
						oid + "." + OidUtil.computeAttributeTag(entry.getKey()));
			}
		}
	}

	protected void processAttribute(TypeSpec.Builder parent, TypeSpec.Builder parentOidClass, AttributeSpec attribute,
			String oid) {

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

		{
			// Add the attribute's property method
			var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, attribute.name)) //
					.addModifiers(PUBLIC) //
					.returns(attribute.getAttributeObjectType()) //
					.addStatement("return document.attribute($LL)", oid.replaceAll(".*\\.", ""));
			parent.addMethod(method.build());
		}

		{
			// Add the attribute's OID field
			var initializer = CodeBlock.of("new AbsoluteOid.STAttributeOid<>(\"$L\")", oid);

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

			var field = FieldSpec
					.builder(ParameterizedTypeName.get(ClassName.get(OID_PACKAGE, "AbsoluteOid", "STAttributeOid"),
							attribute.getAttributeType()), attribute.name, PUBLIC, FINAL)
					.initializer(initializer);

			parentOidClass.addField(field.build());
		}
	}

	protected void processCollection(TypeSpec.Builder parent, TypeSpec.Builder parentOidClass, DocumentSpec document,
			String oid) {
		var documentClass = TypeSpec.classBuilder(VST_PREFIX + document.shortName()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(VST_PACKAGE, "VirtDocument"));

		var oidClass = TypeSpec.classBuilder("Oid").addModifiers(PUBLIC, STATIC)
				.superclass(ClassName.get(OID_PACKAGE, "AbsoluteOid", "STCollectionOid"));

		{
			// Add constructor
			var constructor = MethodSpec.constructorBuilder().addParameter(String.class, "oid")
					.addStatement("super(oid)");

			oidClass.addMethod(constructor.build());
		}

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "STDocument"), "document") //
					.addStatement("super(document)");
			documentClass.addMethod(method.build());
		}

		{
			// Add ID getter
			var method = MethodSpec.methodBuilder("getId") //
					.addModifiers(PUBLIC) //
					.returns(String.class) //
					.addStatement("return document.getId()");
			documentClass.addMethod(method.build());
		}

		{
			// Add OID field
			var field = FieldSpec
					.builder(ClassName.get(getProject().getName() + ".state", VST_PREFIX + document.shortName(), "Oid"),
							document.shortName().toLowerCase(), PUBLIC, FINAL) //
					.initializer("new $L(\"$L\")", VST_PREFIX + document.shortName() + ".Oid", oid);

			if (parentOidClass != null) {
				parentOidClass.addField(field.build());
			} else if (parent != null) {
				parent.addField(field.addModifiers(STATIC).build());
			}
		}

		// Process subdocuments and attributes
		processChildren(documentClass, oidClass, document, oid + ".0");

		if (parent != null) {
			{
				// Add collection field
				var field = FieldSpec.builder(
						ParameterizedTypeName.get(ClassName.get(VST_PACKAGE, "VirtCollection"),
								ClassName.bestGuess(VST_PREFIX + document.shortName())),
						document.shortName().toLowerCase(), PRIVATE);

				parent.addField(field.build());
			}

			{
				// Add collection method
				var method = MethodSpec.methodBuilder(document.shortName().toLowerCase()) //
						.addModifiers(PUBLIC) //
						.returns(ParameterizedTypeName.get(ClassName.get(VST_PACKAGE, "VirtCollection"),
								ClassName.bestGuess(VST_PREFIX + document.shortName()))) //
						.addStatement("if ($L == null) $L = new VirtCollection<>(document.collection($LL))",
								document.shortName().toLowerCase(), document.shortName().toLowerCase(),
								oid.replaceAll(".*\\.", "")) //
						.addStatement("return $L", document.shortName().toLowerCase());

				parent.addMethod(method.build());
			}
		}

		documentClass.addType(oidClass.build());
		writeClass(documentClass.build());
	}

	protected void processDocument(TypeSpec.Builder parent, TypeSpec.Builder parentOidClass, DocumentSpec document,
			String oid) {

		var documentClass = TypeSpec.classBuilder(VST_PREFIX + document.shortName()) //
				.addModifiers(PUBLIC) //
				.superclass(ClassName.get(VST_PACKAGE, "VirtDocument"));

		var oidClass = TypeSpec.classBuilder("Oid").addModifiers(PUBLIC, STATIC)
				.superclass(ClassName.get(OID_PACKAGE, "AbsoluteOid", "STDocumentOid"));

		{
			// Add constructor
			var constructor = MethodSpec.constructorBuilder().addParameter(String.class, "oid")
					.addStatement("super(oid)");

			oidClass.addMethod(constructor.build());
		}

		{
			// Add constructor
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(ClassName.get(ST_PACKAGE, "STDocument"), "document") //
					.addStatement("super(document)");
			documentClass.addMethod(method.build());
		}

		{
			// Add ID getter
			var method = MethodSpec.methodBuilder("getId") //
					.addModifiers(PUBLIC) //
					.returns(String.class) //
					.addStatement("return document.getId()");
			documentClass.addMethod(method.build());
		}

		{
			// Add OID field
			var field = FieldSpec
					.builder(ClassName.get(getProject().getName() + ".state", VST_PREFIX + document.shortName(), "Oid"),
							document.shortName().toLowerCase(), PUBLIC, FINAL) //
					.initializer("new $L(\"$L\")", VST_PREFIX + document.shortName() + ".Oid", oid);

			parentOidClass.addField(field.build());
		}

		// Process subdocuments and attributes
		processChildren(documentClass, oidClass, document, oid);

		if (parent != null) {
			{
				// Add document field
				var field = FieldSpec.builder(ClassName.bestGuess(VST_PREFIX + document.shortName()),
						document.shortName().toLowerCase(), PRIVATE);

				parent.addField(field.build());
			}

			{
				// Add document method
				var method = MethodSpec.methodBuilder(document.shortName().toLowerCase()) //
						.addModifiers(PUBLIC) //
						.returns(ClassName.bestGuess(VST_PREFIX + document.shortName())) //
						.addStatement("if ($L == null) $L = new Virt$L(document.document($LL))",
								document.shortName().toLowerCase(), document.shortName().toLowerCase(),
								document.shortName(), oid.replaceAll(".*\\.", "")) //
						.addStatement("return $L", document.shortName().toLowerCase());

				parent.addMethod(method.build());
			}
		}

		documentClass.addType(oidClass.build());
		writeClass(documentClass.build());
	}

	protected void processRelation(TypeSpec.Builder parent, RelationSpec relation, String oid) {
		if (relation.list) {
			{
				// Add the getter method
				var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "get_" + relation.name)) //
						.addModifiers(PUBLIC) //
						.returns(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STRelation"),
								ClassName.bestGuess(VSTGenerator.VST_PREFIX + relation.simpleName()))) //
						.addStatement("return document.collection($LL).collectionList($L::new)",
								oid.replaceAll(".*\\.", ""), VSTGenerator.VST_PREFIX + relation.simpleName());
				parent.addMethod(method.build());
			}

		} else {
			{
				// Add the getter method
				var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "get_" + relation.name)) //
						.addModifiers(PUBLIC) //
						.returns(ClassName.bestGuess(VSTGenerator.VST_PREFIX + relation.simpleName())) //
						.addStatement("return new $L(document.document($LL))",
								VSTGenerator.VST_PREFIX + relation.simpleName(), oid.replaceAll(".*\\.", ""));
				parent.addMethod(method.build());
			}

			{
				// Add the setter method
				var method = MethodSpec.methodBuilder(LOWER_UNDERSCORE.to(LOWER_CAMEL, "set_" + relation.name)) //
						.addModifiers(PUBLIC) //
						.addParameter(ClassName.bestGuess(VSTGenerator.VST_PREFIX + relation.simpleName()), "v"); //
//						.addStatement("document.setDocument($LL, v.document)", oid.replaceAll(".*\\.", ""));
				parent.addMethod(method.build());
			}
		}
	}

	@Override
	protected void processRoot(DocumentSpec document, String oid) {
		var oidClass = TypeSpec.classBuilder(document.shortName()) //
				.addModifiers(PUBLIC, FINAL);

		{
			// Add private constructor
			var constructor = MethodSpec.constructorBuilder().addModifiers(PRIVATE);
			oidClass.addMethod(constructor.build());
		}

		{
			// Add root field
			var field = FieldSpec.builder(ClassName.get(getProject().getName() + ".state", document.shortName()),
					"root", PUBLIC, FINAL, STATIC).initializer("new $L()", document.shortName());

			oidClass.addField(field.build());
		}

		{
			// Add root getter
			var method = MethodSpec.methodBuilder(document.shortName()) //
					.addModifiers(PUBLIC, STATIC) //
					.returns(ClassName.get(getProject().getName() + ".state", document.shortName())) //
					.addStatement("return root");
			oidClass.addMethod(method.build());
		}

		processChildren(null, oidClass, document, oid);
		writeClass(oidClass.build());
	}
}
