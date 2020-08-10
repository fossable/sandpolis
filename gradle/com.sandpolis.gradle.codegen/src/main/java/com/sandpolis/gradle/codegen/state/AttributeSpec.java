package com.sandpolis.gradle.codegen.state;

import static com.sandpolis.gradle.codegen.state.STGenerator.DATA_PACKAGE;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public class AttributeSpec {

	/**
	 * The attribute's name.
	 */
	public String name;

	/**
	 * The attribute's fully-qualified Java type.
	 */
	public String type;

	/**
	 * A description of the attribute for code documentation.
	 */
	public String description;

	/**
	 * Whether the attribute can be used, potentially in addition to other
	 * attributes, to uniquely identify the parent document in a collection.
	 */
	public boolean identity;

	public TypeName getJavaFxPropertyType() {

		if (type.endsWith("[]")) {
			// TODO
			return ClassName.get("javafx.beans.property", type.replace("[]", "") + "Property");
		}

		return ClassName.get("javafx.beans.property", type + "Property");
	}

	public TypeName getJavaFxSimplePropertyType() {
		if (type.endsWith("[]")) {
			// TODO
			return ClassName.get("javafx.beans.property", "Simple" + type.replace("[]", "") + "Property");
		}

		return ClassName.get("javafx.beans.property", "Simple" + type + "Property");
	}

	public TypeName getAttributeType() {

		if (type.endsWith("[]")) {
			return ArrayTypeName.of(ClassName.bestGuess(type.replace("[]", "")).unbox());
		} else {
			var components = type.split("<|>");

			switch (components.length) {
			case 1:
				return ClassName.bestGuess(type);
			case 2:
				return ParameterizedTypeName.get(ClassName.bestGuess(components[0]),
						ClassName.bestGuess(components[1]));
			default:
				throw new RuntimeException("Invalid attribute type");
			}
		}
	}

	public TypeName getAttributeObjectType() {
		return ParameterizedTypeName.get(ClassName.get(DATA_PACKAGE, "Attribute"), getAttributeType());
	}

	public ClassName getImplementationType() {

		var components = type.split("<|>");
		if (components.length == 2) {
			// TODO
			return ClassName.get("com.sandpolis.core.instance.data",
					type.replaceAll(".*\\.", "").replace("[]", "Array") + "Attribute");
		} else {
			return ClassName.get("com.sandpolis.core.instance.data",
					type.replaceAll(".*\\.", "").replace("[]", "Array") + "Attribute");
		}
	}
}
