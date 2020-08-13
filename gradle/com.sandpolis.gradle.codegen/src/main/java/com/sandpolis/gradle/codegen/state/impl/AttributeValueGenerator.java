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

import static com.sandpolis.gradle.codegen.state.STGenerator.ST_PACKAGE;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.io.IOException;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for attribute value implementations.
 */
public class AttributeValueGenerator extends DefaultTask {

	/**
	 * Types that are integral Hibernate entities.
	 */
	private static final List<ClassName> entityTypes = List.of( //
			ClassName.get(String.class), //
			ClassName.get(Boolean.class), //
			ClassName.get(Integer.class), //
			ClassName.get(Long.class), //
			ClassName.get(Double.class) //
	);

	/**
	 * Types that are arrays.
	 */
	private static final List<ArrayTypeName> arrayTypes = List.of( //
			ArrayTypeName.of(byte.class) //
	);

	/**
	 * Types that require a converter class.
	 */
	private static final List<ClassName> convertableTypes = List.of( //
			ClassName.get("com.sandpolis.core.foundation", "Platform", "OsType"), //
			ClassName.get("com.sandpolis.core.instance", "Metatypes", "InstanceType"), //
			ClassName.get("com.sandpolis.core.instance", "Metatypes", "InstanceFlavor"), //
			ClassName.get("java.security.cert", "X509Certificate") //
	);

	@TaskAction
	public void action() {

		entityTypes.stream().forEach(type -> {
			try {
				generateAttributeValue(type);
				generateListAttributeValue(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		arrayTypes.stream().forEach(type -> {
			try {
				generateArrayAttributeValue(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		convertableTypes.stream().forEach(type -> {
			try {
				generateConverter(type);
				generateAttributeValue(type);
				generateListAttributeValue(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Generate an attribute value implementation for an array type.
	 * 
	 * @param type The array type
	 * @throws IOException
	 */
	private void generateArrayAttributeValue(ArrayTypeName type) throws IOException {
		var av = TypeSpec //
				.classBuilder(type.componentType.box().toString().replaceAll(".*\\.", "") + "ArrayAttributeValue") //
				.addModifiers(PUBLIC) //
				.addAnnotation(ClassName.get("javax.persistence", "Embeddable")) //
				.superclass(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "AttributeValue"), type));

		{
			// Add value field
			var field = FieldSpec.builder(type, "value", PRIVATE) //
					.addAnnotation(ClassName.get("javax.persistence", "Lob"));
			av.addField(field.build());
		}

		{
			// Add get method
			var method = MethodSpec.methodBuilder("get") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(type) //
					.addStatement("return value");
			av.addMethod(method.build());
		}

		{
			// Add set method
			var method = MethodSpec.methodBuilder("set") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.addParameter(type, "value") //
					.addStatement("this.value = value");
			av.addMethod(method.build());
		}

		{
			// Add clone method
			var method = MethodSpec.methodBuilder("clone") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "AttributeValue"), type)) //
					.addStatement("return new $L()",
							type.componentType.box().toString().replaceAll(".*\\.", "") + "ArrayAttributeValue");
			av.addMethod(method.build());
		}

		{
			// Add getProto method
			var method = MethodSpec.methodBuilder("getProto") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(ClassName.get("com.sandpolis.core.instance", "State", "ProtoAttributeValue", "Builder")) //
					.addStatement("if (value == null) return null") //
					.addStatement("return State.ProtoAttributeValue.newBuilder().addBytes($T.copyFrom(value))",
							ClassName.get("com.google.protobuf", "ByteString"));

			av.addMethod(method.build());
		}

		{
			// Add setProto method
			var method = MethodSpec.methodBuilder("setProto") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.addParameter(ClassName.get("com.sandpolis.core.instance", "State", "ProtoAttributeValue"), "v") //
					.addStatement("value = v.getBytes(0).toByteArray()");

			av.addMethod(method.build());
		}

		// Output the class
		JavaFile.builder(getProject().getName() + ".state", av.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}

	/**
	 * Generate an attribute value implementation for a scalar type.
	 * 
	 * @param type
	 * @throws IOException
	 */
	private void generateAttributeValue(ClassName type) throws IOException {

		var av = TypeSpec //
				.classBuilder(type.simpleName() + "AttributeValue") //
				.addModifiers(PUBLIC) //
				.addAnnotation(ClassName.get("javax.persistence", "Embeddable")) //
				.superclass(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "AttributeValue"), type));

		// Add value field
		av.addField(newValueField(type).build());

		{
			// Add get method
			var method = MethodSpec.methodBuilder("get") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(type) //
					.addStatement("return value");
			av.addMethod(method.build());

		}

		{
			// Add set method
			var method = MethodSpec.methodBuilder("set") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.addParameter(type, "value") //
					.addStatement("this.value = value");
			av.addMethod(method.build());
		}

		{
			// Add clone method
			var method = MethodSpec.methodBuilder("clone") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "AttributeValue"), type)) //
					.addStatement("return new $L()", type.simpleName() + "AttributeValue");
			av.addMethod(method.build());
		}

		{
			// Add getProto method
			var method = MethodSpec.methodBuilder("getProto") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(ClassName.get("com.sandpolis.core.instance", "State", "ProtoAttributeValue", "Builder")) //
					.addStatement("if (value == null) return null");

			switch (type.toString()) {
			case "java.security.cert.X509Certificate":
				method.addStatement(
						"return State.ProtoAttributeValue.newBuilder().addBytes($T.copyFrom($L.INSTANCE.convertToDatabaseColumn(value)))",
						ClassName.get("com.google.protobuf", "ByteString"), //
						type.simpleName() + "Converter");
				break;
			default:
				method.addStatement("return State.ProtoAttributeValue.newBuilder().add$L(value)", type.simpleName());
			}

			av.addMethod(method.build());
		}

		{
			// Add setProto method
			var method = MethodSpec.methodBuilder("setProto") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.addParameter(ClassName.get("com.sandpolis.core.instance", "State", "ProtoAttributeValue"), "v");

			switch (type.toString()) {
			case "java.security.cert.X509Certificate":
				method.addStatement("value = $L.INSTANCE.convertToEntityAttribute(v.getBytes(0).toByteArray())",
						type.simpleName() + "Converter");
				break;
			default:
				method.addStatement("value = v.get$L(0)", type.simpleName());
			}

			av.addMethod(method.build());
		}

		// Output the class
		JavaFile.builder(getProject().getName() + ".state", av.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}

	private void generateListAttributeValue(ClassName type) throws IOException {

		// Alter the type to be a list
		var listType = ParameterizedTypeName.get(ClassName.get("java.util", "List"), type);

		var av = TypeSpec //
				.classBuilder(type.simpleName() + "ListAttributeValue") //
				.addModifiers(PUBLIC).addAnnotation(ClassName.get("javax.persistence", "Embeddable")) //
				.superclass(ParameterizedTypeName
						.get(ClassName.get("com.sandpolis.core.instance.state", "AttributeValue"), listType));

		// Add value field
		av.addField(newValueField(listType).build());

		{
			// Add get method
			var method = MethodSpec.methodBuilder("get") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(listType) //
					.addStatement("return value");
			av.addMethod(method.build());

		}

		{
			// Add set method
			var method = MethodSpec.methodBuilder("set") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.addParameter(listType, "value") //
					.addStatement("this.value = value");
			av.addMethod(method.build());
		}

		{
			// Add clone method
			var method = MethodSpec.methodBuilder("clone") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "AttributeValue"), listType)) //
					.addStatement("return new $L()", type.simpleName() + "ListAttributeValue");
			av.addMethod(method.build());
		}

		{
			// Add getProto method
			var method = MethodSpec.methodBuilder("getProto") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.returns(ClassName.get("com.sandpolis.core.instance", "State", "ProtoAttributeValue", "Builder")) //
					.addStatement("if (value == null) return null");

			switch (type.toString()) {
			case "com.sandpolis.core.foundation.Platform.OsType":
			case "com.sandpolis.core.instance.Metatypes.InstanceType":
			case "com.sandpolis.core.instance.Metatypes.InstanceFlavor":
				method.addStatement("return null");
				break;
			default:
				method.addStatement("return State.ProtoAttributeValue.newBuilder().addAll$L(value)", type.simpleName());

			}

			av.addMethod(method.build());
		}

		{
			// Add setProto method
			var method = MethodSpec.methodBuilder("setProto") //
					.addModifiers(PUBLIC) //
					.addAnnotation(Override.class) //
					.addParameter(ClassName.get("com.sandpolis.core.instance", "State", "ProtoAttributeValue"), "v") //
					.addStatement("value = v.get$LList()", type.simpleName());
			av.addMethod(method.build());
		}

		// Output the class
		JavaFile.builder(getProject().getName() + ".state", av.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}

	private FieldSpec.Builder newValueField(TypeName type) {

		var valueField = FieldSpec.builder(type, "value", PRIVATE);

		// Add converter annotation if applicable
		if (convertableTypes.contains(type)) {
			valueField.addAnnotation( //
					AnnotationSpec.builder(ClassName.get("javax.persistence", "Convert"))
							.addMember("converter", "$T.class", ClassName.get(ST_PACKAGE,
									type.toString().replaceAll(".*\\.", "").replace("[]", "Array") + "Converter"))
							.build());
		}

		// Add JPA annotation for list attributes
		if (type instanceof ParameterizedTypeName) {
			valueField.addAnnotation(ClassName.get("javax.persistence", "ElementCollection"));
		}

		return valueField;
	}

	public void generateConverter(ClassName type) throws IOException {

		var converterClass = TypeSpec //
				.classBuilder(type.simpleName() + "Converter") //
				.addModifiers(PUBLIC) //
				.addAnnotation(ClassName.get("javax.persistence", "Converter"));

		{
			// Add instance field
			var field = FieldSpec
					.builder(ClassName.get(ST_PACKAGE, type.simpleName() + "Converter"), "INSTANCE", PUBLIC, STATIC,
							FINAL) //
					.initializer("new $L()", type.simpleName() + "Converter");
			converterClass.addField(field.build());
		}

		// Add convertToDatabaseColumn
		var convertToDatabaseColumn = MethodSpec.methodBuilder("convertToDatabaseColumn") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.addParameter(type, "value");

		// Add convertToEntityAttribute
		var convertToEntityAttribute = MethodSpec.methodBuilder("convertToEntityAttribute") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(type);

		switch (type.toString()) {
		case "com.sandpolis.core.foundation.Platform.OsType":
		case "com.sandpolis.core.instance.Metatypes.InstanceType":
		case "com.sandpolis.core.instance.Metatypes.InstanceFlavor":
			converterClass.addSuperinterface(ParameterizedTypeName
					.get(ClassName.get("javax.persistence", "AttributeConverter"), type, ClassName.get(Integer.class)));
			convertToDatabaseColumn.addStatement("return value.getNumber()").returns(Integer.class);
			convertToEntityAttribute.addStatement("return $T.forNumber(value)", type).addParameter(Integer.class,
					"value");
			break;
		case "java.security.cert.X509Certificate":
			converterClass.addSuperinterface(ParameterizedTypeName
					.get(ClassName.get("javax.persistence", "AttributeConverter"), type, ArrayTypeName.of(byte.class)));
			convertToDatabaseColumn
					.addCode(
							"try { return value.getEncoded(); } catch (Exception e) { throw new RuntimeException(e); }")
					.returns(byte[].class);
			convertToEntityAttribute.addParameter(byte[].class, "value").addCode(
					"try { return $T.parseCert(value); } catch (Exception e) { throw new RuntimeException(e); }",
					ClassName.get("com.sandpolis.core.foundation.util", "CertUtil"));
			break;
		}

		converterClass.addMethod(convertToDatabaseColumn.build());
		converterClass.addMethod(convertToEntityAttribute.build());

		// Output the class
		JavaFile.builder(getProject().getName() + ".state", converterClass.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}
}
