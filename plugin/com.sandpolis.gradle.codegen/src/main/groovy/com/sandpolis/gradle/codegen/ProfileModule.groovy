/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.gradle.codegen

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

import javax.lang.model.element.Modifier

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import org.yaml.snakeyaml.Yaml

/**
 * Generator for the com.sandpolis.core.profile module.
 *
 * @author cilki
 */
class ProfileModule extends DefaultTask {

  @TaskAction
  void action() {

    // Load the file header
    def header = getClass().getResourceAsStream('/copyright.txt').text

    // Parse the index YAML
    new Yaml().load(getProject().file("attribute.yml").text).each {
      def name = firstKey(it).toUpperCase()
      def ak = TypeSpec.classBuilder("AK_" + name).addModifiers(Modifier.PUBLIC, Modifier.FINAL)

      processGroup(ak, it, null)

      // Write out
      JavaFile.builder("com.sandpolis.core.attribute.key", ak.build())
        .skipJavaLangImports(true).build().writeTo(getProject().file("src/main/java"));

      // Additional modifications to the generated source
      def out = getProject().file("src/main/java/com/sandpolis/core/attribute/key/AK_${name}.java")
      out.text = header + out.text
    }
  }

  /**
   * Convert the given YAML group into a field along with its children.
   */
  void processGroup(ak, group, parent) {
    def initializer = "\n\t\tnew AttributeNodeKey"
    if (parent == null)
      initializer += "(${group['tag']}, ${group.get('plurality', 0)})"
    else
      initializer += "($parent, ${group['tag']}, ${group.get('plurality', 0)})"

    def groupName = firstKey(group).toUpperCase()
    ak.addField(FieldSpec.builder(ClassName.bestGuess("com.sandpolis.core.attribute.AttributeNodeKey"),
        groupName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
      .addJavadoc('$L\n', firstValue(group)).initializer(initializer).build())

    group['children'].each {
      if (it['children'])
        processGroup(ak, it, groupName)
      else
        processAttribute(ak, it, groupName)
    }
  }

  /**
   * Convert the given YAML attribute into a field.
   */
  void processAttribute(ak, attribute, parent) {
    def initializer = "\n\t\tAttributeKey.newBuilder($parent, ${attribute['tag']})"
    if (attribute['static'])
      initializer += ".setStatic(${attribute['static']})"

    ak.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.bestGuess("com.sandpolis.core.attribute.AttributeKey"),
        ClassName.bestGuess(attribute['type'])), firstKey(attribute).toUpperCase(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
      .addJavadoc('$L\n', firstValue(attribute)).initializer(initializer + ".build()").build())
  }

  /**
   * Return the first key of a LinkedHashMap.
   */
  def firstKey(map) {
    return map.keySet().iterator().next()
  }

  /**
   * Return the first value of a LinkedHashMap.
   */
  def firstValue(map) {
    return map.values().iterator().next()
  }
}
