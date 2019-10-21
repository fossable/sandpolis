/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.gradle.codegen

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

import groovy.json.JsonSlurper

import java.util.ArrayList

import static javax.lang.model.element.Modifier.*

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Generator for attribute definition files.
 *
 * @author cilki
 */
class AttributeGenerator extends DefaultTask {

	@TaskAction
	void action () {

		// Parse the attribute file
		def root = new JsonSlurper().parse(getProject().file("attribute.json"), 'UTF-8')

		if (root.collections != null) {
			for (def collection : root.collections) {
				def ak = TypeSpec.classBuilder("AK_" + collection.name.toUpperCase()).addModifiers(PUBLIC, FINAL)

				processCollection(ak, collection, '')

				JavaFile.builder(project.name, ak.build())
					.addFileComment("This file was automatically generated. Do not edit!")
					.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
			}
		}
		if (root.documents != null) {
			for (def document : root.documents) {
				def ak = TypeSpec.classBuilder("AK_" + document.name.toUpperCase()).addModifiers(PUBLIC, FINAL)

				processDocument(ak, document, '')

				JavaFile.builder(project.name, ak.build())
					.addFileComment("This file was automatically generated. Do not edit!")
					.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
			}
		}
		if (root.attributes != null) {
			throw new RuntimeException("Top level attributes are not allowed")
		}
	}

	void processCollection(ak, collection, parent) {

		if (collection.collections != null) {
			for (def subcollection : collection.collections) {
				def nested = TypeSpec.classBuilder(subcollection.name.toUpperCase()).addModifiers(PUBLIC, FINAL, STATIC)
				processCollection(nested, subcollection, "$parent/${collection.name}/_")
				ak.addType(nested.build())
			}
		}
		if (collection.documents != null) {
			for (def document : collection.documents) {
				def nested = TypeSpec.classBuilder(document.name.toUpperCase()).addModifiers(PUBLIC, FINAL, STATIC)
				processDocument(nested, document, "$parent/${collection.name}/_")
				ak.addType(nested.build())
			}
		}
		if (collection.attributes != null) {
			for (def attribute : collection.attributes) {
				processAttribute(ak, attribute, "$parent/${collection.name}/_")
			}
		}
	}

	void processDocument(ak, document, parent) {

		if (document.collections != null) {
			for (def collection : document.collections) {
				processCollection(ak, collection, "$parent/${document.name}")
			}
		}
		if (document.documents != null) {
			for (def subdocument : document.documents) {
				processDocument(ak, subdocument, "$parent/${document.name}")
			}
		}
		if (document.attributes != null) {
			for (def attribute : document.attributes) {
				processAttribute(ak, attribute, "$parent/${document.name}")
			}
		}
	}

	void processAttribute(ak, attribute, parent) {

		def field = FieldSpec.builder(ParameterizedTypeName.get(
			ClassName.bestGuess("com.sandpolis.core.profile.attribute.key.AttributeKey"), ClassName.bestGuess(attribute.type)),
				attribute.name.toUpperCase(), PUBLIC, STATIC, FINAL)
			.initializer("new AttributeKey<>(\"${project.name}\", ${attribute.type}.class, \"$parent/${attribute.name}\")")
		
		if (attribute.description != null)
			field.addJavadoc('$L.\n', attribute.description)

		ak.addField(field.build())
	}
}
