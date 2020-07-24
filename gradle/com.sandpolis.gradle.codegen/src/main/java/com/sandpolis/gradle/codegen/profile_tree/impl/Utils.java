package com.sandpolis.gradle.codegen.profile_tree.impl;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

class Utils {

	public static String toJavaFxProperty(String attributeType) {
		if (attributeType.endsWith("[]")) {
			return ClassName.bestGuess(attributeType.replace("[]", "")).simpleName() + "Property";
		}

		return ClassName.bestGuess(attributeType).simpleName() + "Property";
	}

	public static TypeName toType(String attributeType) {
		if (attributeType.endsWith("[]")) {
			return ArrayTypeName.of(ClassName.bestGuess(attributeType.replace("[]", "")).unbox());
		} else {
			var components = attributeType.split("<|>");
			if (components.length == 2) {
				return ParameterizedTypeName.get(ClassName.bestGuess(components[0]),
						ClassName.bestGuess(components[1]));

			} else {
				return ClassName.bestGuess(attributeType);
			}
		}
	}

	public static ClassName toAttributeType(String attributeType) {
		if (attributeType.endsWith("[]")) {
			// TODO
			return ClassName.bestGuess("com.sandpolis.core.instance.data.ByteArrayAttribute");
		} else {
			var components = attributeType.split("<|>");
			if (components.length == 2) {
				// TODO
				return ClassName.bestGuess("com.sandpolis.core.instance.data.ByteArrayAttribute");
			} else {
				return ClassName.get("com.sandpolis.core.instance.data",
						ClassName.bestGuess(attributeType).simpleName() + "Attribute");
			}
		}
	}

	private Utils() {
	}
}
