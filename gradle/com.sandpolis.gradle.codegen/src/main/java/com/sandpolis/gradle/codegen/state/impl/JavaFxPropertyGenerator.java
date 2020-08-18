package com.sandpolis.gradle.codegen.state.impl;

import static javax.lang.model.element.Modifier.PUBLIC;

import java.io.IOException;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class JavaFxPropertyGenerator extends DefaultTask {

	private static final List<ClassName> propertyTypes = List.of( //
			ClassName.get("com.sandpolis.core.foundation", "Platform", "OsType"), //
			ClassName.get("com.sandpolis.core.instance", "Metatypes", "InstanceType"), //
			ClassName.get("com.sandpolis.core.instance", "Metatypes", "InstanceFlavor"), //
			ClassName.get("java.security.cert", "X509Certificate") //
	);

	@TaskAction
	public void action() {
		propertyTypes.stream().forEach(type -> {
			try {
				generatePropertyClass(type);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void generatePropertyClass(ClassName type) throws IOException {
		var propertyClass = TypeSpec.classBuilder(type.simpleName() + "Property") //
				.superclass(
						ParameterizedTypeName.get(ClassName.get("javafx.beans.property", "SimpleObjectProperty"), type)) //
				.addModifiers(PUBLIC);

		{
			var method = MethodSpec.constructorBuilder() //
					.addModifiers(PUBLIC) //
					.addParameter(Object.class, "bean") //
					.addParameter(String.class, "name") //
					.addStatement("super(bean, name)");
			propertyClass.addMethod(method.build());
		}

		JavaFile.builder(getProject().getName(), propertyClass.build())
				.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
				.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
	}
}