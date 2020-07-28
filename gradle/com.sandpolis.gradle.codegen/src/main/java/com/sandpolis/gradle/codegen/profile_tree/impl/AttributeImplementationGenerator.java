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

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

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
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for attribute implementations.
 *
 * @author cilki
 */
public class AttributeImplementationGenerator extends DefaultTask {

	private static final List<String> entityTypes = List.of( //
			"java.lang.String", //
			"java.lang.Boolean", //
			"java.lang.Integer", //
			"java.lang.Long", //
			"java.lang.Double", //
			"java.util.Date" //
	);
	private static final List<String> arrayTypes = List.of( //
			"java.lang.Byte[]" //
	);

	private static final List<String> convertableTypes = List.of( //
			"com.sandpolis.core.foundation.Platform.OsType", //
			"com.sandpolis.core.instance.Metatypes.InstanceType", //
			"com.sandpolis.core.instance.Metatypes.InstanceFlavor", //
			"java.security.cert.X509Certificate" //
	);

	@TaskAction
	public void action() {

		entityTypes.stream().forEach(type -> {
			try {
				generateAttribute(type);
				generateListAttribute(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		arrayTypes.stream().forEach(type -> {
			try {
				generateAttribute(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		convertableTypes.stream().forEach(type -> {
			try {
				generateConverter(type);
				generateAttribute(type);
				generateListAttribute(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void generateAttribute(String attributeType) throws IOException {
		var type = Utils.toType(attributeType);

		var attributeClass = TypeSpec
				.classBuilder(ClassName.bestGuess(attributeType).simpleName().replace("[]", "Array") + "Attribute")
				.addModifiers(PUBLIC).addAnnotation(ClassName.get("javax.persistence", "Entity"))
				.superclass(ParameterizedTypeName.get(ClassName.get("com.sandpolis.core.instance.data", "Attribute"),
						type));

		// Add value field
		var valueField = FieldSpec.builder(type.isBoxedPrimitive() ? type.unbox() : type, "value", PRIVATE);

		if (convertableTypes.contains(attributeType)) {
			valueField
					.addAnnotation(
							AnnotationSpec.builder(ClassName.get("javax.persistence", "Convert"))
									.addMember("converter", "$T.class",
											ClassName.get("com.sandpolis.core.instance.data",
													ClassName.bestGuess(attributeType).simpleName() + "Converter"))
									.build());
		}
		if (arrayTypes.contains(attributeType)) {
			valueField.addAnnotation(ClassName.get("javax.persistence", "Lob"));
		}
		attributeClass.addField(valueField.build());

		// Add get method
		var getMethod = MethodSpec.methodBuilder("get") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(type) //
				.addCode("if (supplier != null) return supplier.get();\nreturn value;");
		attributeClass.addMethod(getMethod.build());

		// Add set method
		var setMethod = MethodSpec.methodBuilder("set") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.addParameter(type, "value") //
				.addStatement("this.value = value");
		attributeClass.addMethod(setMethod.build());

		// Add timestamp method
		var timestampMethod = MethodSpec.methodBuilder("timestamp") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(java.util.Date.class) //
				.addStatement("return null");
		attributeClass.addMethod(timestampMethod.build());

		// Add serialize method
		var serializeMethod = MethodSpec.methodBuilder("serialize") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(ClassName.get("com.sandpolis.core.instance", "Attribute", "ProtoAttribute")) //
				.addStatement("return $T.newBuilder().build()",
						ClassName.get("com.sandpolis.core.instance", "Attribute", "ProtoAttribute"));
		attributeClass.addMethod(serializeMethod.build());

		// Add merge method
		var mergeMethod = MethodSpec.methodBuilder("merge") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.addException(Exception.class) //
				.addParameter(ClassName.get("com.sandpolis.core.instance", "Attribute", "ProtoAttribute"), "delta") //
				.addStatement("set(null)");
		attributeClass.addMethod(mergeMethod.build());

		// Output the class
		JavaFile.builder(getProject().getName() + ".data", attributeClass.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}

	public void generateListAttribute(String attributeType) throws IOException {
		var type = Utils.toType(attributeType);

		// Alter the type to be a list
		var listType = ParameterizedTypeName.get(ClassName.get("java.util", "List"), type);

		var attributeListClass = TypeSpec
				.classBuilder(ClassName.bestGuess(attributeType).simpleName() + "ListAttribute").addModifiers(PUBLIC)
				.addAnnotation(ClassName.get("javax.persistence", "Entity")).superclass(ParameterizedTypeName
						.get(ClassName.get("com.sandpolis.core.instance.data", "Attribute"), listType));

		// Add value field
		var valueField = FieldSpec.builder(listType, "value", PRIVATE);
		valueField.addAnnotation(ClassName.get("javax.persistence", "ElementCollection"));

		if (convertableTypes.contains(attributeType)) {
			valueField
					.addAnnotation(
							AnnotationSpec.builder(ClassName.get("javax.persistence", "Convert"))
									.addMember("converter", "$T.class",
											ClassName.get("com.sandpolis.core.instance.data",
													ClassName.bestGuess(attributeType).simpleName() + "Converter"))
									.build());
		}
		attributeListClass.addField(valueField.build());

		// Add get method
		var getMethod = MethodSpec.methodBuilder("get") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(listType) //
				.addStatement("return value");
		attributeListClass.addMethod(getMethod.build());

		// Add set method
		var setMethod = MethodSpec.methodBuilder("set") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.addParameter(listType, "value") //
				.addStatement("this.value = value");
		attributeListClass.addMethod(setMethod.build());

		// Add timestamp method
		var timestampMethod = MethodSpec.methodBuilder("timestamp") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(java.util.Date.class) //
				.addStatement("return null");
		attributeListClass.addMethod(timestampMethod.build());

		// Add serialize method
		var serializeMethod = MethodSpec.methodBuilder("serialize") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.returns(ClassName.get("com.sandpolis.core.instance", "Attribute", "ProtoAttribute")) //
				.addStatement("return $T.newBuilder().build()",
						ClassName.get("com.sandpolis.core.instance", "Attribute", "ProtoAttribute"));
		attributeListClass.addMethod(serializeMethod.build());

		// Add merge method
		var mergeMethod = MethodSpec.methodBuilder("merge") //
				.addModifiers(PUBLIC) //
				.addAnnotation(Override.class) //
				.addException(Exception.class) //
				.addParameter(ClassName.get("com.sandpolis.core.instance", "Attribute", "ProtoAttribute"), "delta") //
				.addStatement("set(null)");
		attributeListClass.addMethod(mergeMethod.build());

		// Output the class
		JavaFile.builder(getProject().getName() + ".data", attributeListClass.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}

	public void generateConverter(String attributeType) throws IOException {
		var type = Utils.toType(attributeType);

		var converterClass = TypeSpec.classBuilder(ClassName.bestGuess(attributeType).simpleName() + "Converter")
				.addModifiers(PUBLIC).addAnnotation(ClassName.get("javax.persistence", "Converter"));

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
		JavaFile.builder(getProject().getName() + ".data", converterClass.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}
}
