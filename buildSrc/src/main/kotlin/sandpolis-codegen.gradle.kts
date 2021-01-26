//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import com.google.common.base.CaseFormat
import com.squareup.javapoet.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import com.beust.klaxon.*

val OID_PACKAGE = "com.sandpolis.core.instance.state.oid"
val ST_PACKAGE = "com.sandpolis.core.instance.state.st"

fun processAttribute(oidClass: TypeSpec.Builder, document: JsonObject, attribute: JsonObject) {

    // Validate name
    if (!SourceVersion.isIdentifier(attribute.string("name"))) {
        throw RuntimeException("Invalid name: " + attribute.string("name"))
    }

    // Determine type
    val type = if (attribute.string("type")!!.contains("/"))
        ClassName.get(Object::class.java)
    else if (attribute.string("type")!!.equals("java.lang.Byte[]"))
        ArrayTypeName.of(TypeName.BYTE)
    else
        ClassName.bestGuess(attribute.string("type"))

    // Add the attribute's OID field
    var initializer = CodeBlock.of("new AbsoluteOid<>(\"\$L\", \"\$L\")", project.name,
            document.string("name") + "/" + attribute.string("name"))

    // Add type data
    initializer = CodeBlock.of("\$L.setData(\$T.TYPE, \$T.class)", initializer,
            ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"), type)

    // Add singularity data
    if (attribute.boolean("list") == true) {
        initializer = CodeBlock.of("\$L.setData(\$T.SINGULARITY, false)", initializer,
                ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"))
    }

    // Add identity data
    if (attribute.boolean("immutable") == true) {
        initializer = CodeBlock.of("\$L.setData(\$T.IMMUTABLE, true)", initializer,
                ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"))
    }

    // Add osquery data
    if (attribute.string("osquery") != null) {
        initializer = CodeBlock.of("\$L.setData(\$T.OSQUERY, \"\$L\")", initializer,
                ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"), attribute.string("osquery"))
    }

    // Create fields
    val field = FieldSpec
            .builder(ParameterizedTypeName.get(ClassName.get(OID_PACKAGE, "AbsoluteOid"),
                    ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"),
                            type)),
                    attribute.string("name"), Modifier.PUBLIC, Modifier.FINAL)
            .initializer(attribute.string("name")!!.toUpperCase())
    oidClass.addField(field.build())

    val staticField = FieldSpec
            .builder(ParameterizedTypeName.get(ClassName.get(OID_PACKAGE, "AbsoluteOid"),
                    ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"),
                            type)),
                    attribute.string("name")!!.toUpperCase(), Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
            .initializer(initializer)
    oidClass.addField(staticField.build())
}

fun generateDocument(parent: TypeSpec.Builder?, document: JsonObject): TypeSpec.Builder {

    // Determine class name
    val className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, document.string("name")!!.replace("/$".toRegex(), "").split("/").last())

    val attributes: JsonArray<JsonObject>? = document.array("attributes")

    val oidClass = TypeSpec.classBuilder(className + "Oid").addModifiers(Modifier.PUBLIC).superclass(ParameterizedTypeName
            .get(ClassName.get(OID_PACKAGE, "AbsoluteOid"), ClassName.get(ST_PACKAGE, "STDocument")))

    // Add constructor
    val constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(String::class.java, "path")
            .addStatement("super(\"\$L\", path)", project.name)
    oidClass.addMethod(constructor.build())

    if (parent != null) {
        // Add OID field
        val field = FieldSpec
                .builder(ClassName.get(project.name + ".state", className + "Oid"),
                        className.toLowerCase(), Modifier.PUBLIC, Modifier.FINAL) //
                .initializer("new \$L(\"\$L\")", className + "Oid", document.string("name"))
        parent.addField(field.build())

        // Add resolve method
        if (document.string("name")!!.endsWith("/")) {
            val resolveMethod = MethodSpec.methodBuilder(className.toLowerCase()).addModifiers(Modifier.PUBLIC)
                .addParameter(String::class.java, "id")
                .returns(ClassName.get(project.name + ".state", className + "Oid"))
                .addStatement("return new \$L(this.toString() + \"/\$L/\" + id)", className + "Oid", className.toLowerCase())
            parent.addMethod(resolveMethod.build())
        }
    }
    if (attributes != null) {
        for (entry in attributes) {
            processAttribute(oidClass, document, entry)
        }
    }

    return oidClass
}

project.afterEvaluate {

    val specification = project.file("state.json")

    // Prepare to add dependencies on these tasks
    val generateProto = project.tasks.findByName("generateProto")
    val compileJava = project.tasks.findByName("compileJava")
    val clean = project.tasks.findByName("clean")

    if (specification.exists()) {
        val generateOids by tasks.creating(DefaultTask::class) {

            doLast {
                val oidClasses = HashMap<String, TypeSpec.Builder>()

                // Load the schema
                val schema = Klaxon().parseJsonArray(specification.reader()) as JsonArray<JsonObject>

                // Root class
                val root = generateDocument(null, JsonObject(mapOf("name" to "Instance")))

                // Add root method
                val rootMethod = MethodSpec.methodBuilder("InstanceOid").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ClassName.get(project.name + ".state", "InstanceOid"))
                    .addStatement("return new InstanceOid(\"\")")
                root.addMethod(rootMethod.build())
                oidClasses.put("", root)

                // Generate classes
                schema.forEach {
                    val name = it.string("name")!!
                    oidClasses.put(name.replace("/+$".toRegex(), ""), generateDocument(oidClasses.get(name.replace("/+[^/]+/*$".toRegex(), "")), it))
                }

                // Write classes
                oidClasses.values.forEach {
                    JavaFile.builder(project.name + ".state", it.build())
                            .addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
                            .skipJavaLangImports(true).build().writeTo(project.file("gen/main/java"))
                }
            }
        }
        if (generateProto != null) {
            generateOids.dependsOn(generateProto)
        }
        if (compileJava != null) {
            compileJava.dependsOn(generateOids)
        }
    } else {
        throw RuntimeException("Specification not found")
    }

    // Remove generated sources in clean task
    val cleanGeneratedSources by tasks.creating(Delete::class) {
        delete(project.file("gen/main/java"))
    }
    if (clean != null) {
        clean.dependsOn(cleanGeneratedSources)
    }
}
