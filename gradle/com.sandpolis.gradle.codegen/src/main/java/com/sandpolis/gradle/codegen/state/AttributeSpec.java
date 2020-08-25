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

import static com.sandpolis.gradle.codegen.state.STGenerator.ST_PACKAGE;

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

	public String simpleName() {
		return type.replaceAll(".*\\.", "");
	}

	public TypeName getAttributeObjectType() {
		return ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"), getAttributeType());
	}
}
