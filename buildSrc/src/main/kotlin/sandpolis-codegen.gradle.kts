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
import com.beust.klaxon.Klaxon

open class AttributeSpec(val name: String, val type: String, val description: String? = null, val list: Boolean? = false, val immutable: Boolean? = false, val osquery: String? = null, val id: Boolean? = false) {

    /*init {
        require(SourceVersion.isIdentifier(name)) {
            "Missing attribute name"
        }
    }*/

    fun getAttributeObjectType(): TypeName {
        return ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"), getAttributeType())
    }

    fun getAttributeType(): TypeName {
        return if (type.contains("/")) {
            // The attribute is a relation
            ClassName.get(OID_PACKAGE, "Oid")
        } else if (type.endsWith("[]")) {
            ArrayTypeName.of(ClassName.bestGuess(type.replace("[]", "")).unbox())
        } else {
            ClassName.bestGuess(type)
        }
    }

    fun simpleName(): String {
        return type.replace(".*\\.".toRegex(), "")
    }
}

open class DocumentSpec(val name: String, val attributes: Array<AttributeSpec>? = null) {

    /*init {
        require(SourceVersion.isIdentifier(name) || name.contains("/")) {
            "Missing attribute name"
        }
    }*/

    fun basePath(): String {
        val components = name.split("/").toTypedArray()
        return components[components.size - 1]
    }

    fun className(): String {
        val components = name.split("/").toTypedArray()
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, components[components.size - 1])
    }

    fun parentPath(): String {
        return name.replace("/+[^/]+/*$".toRegex(), "")
    }
}

val OID_PACKAGE = "com.sandpolis.core.instance.state.oid"
val ST_PACKAGE = "com.sandpolis.core.instance.state.st"

fun processAttribute(oidClass: TypeSpec.Builder, document: DocumentSpec, attribute: AttributeSpec) {

    // Add the attribute's OID field
    var initializer = CodeBlock.of("new AbsoluteOid<>(\"\$L\", \"\$L\")", project.name,
            document.name + "/" + attribute.name)

    // Add type data
    initializer = CodeBlock.of("\$L.setData(\$T.TYPE, \$T.class)", initializer,
            ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"), attribute.getAttributeType())

    // Add singularity data
    if (attribute.list!!) {
        initializer = CodeBlock.of("\$L.setData(\$T.SINGULARITY, false)", initializer,
                ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"))
    }

    // Add identity data
    if (attribute.immutable!!) {
        initializer = CodeBlock.of("\$L.setData(\$T.IMMUTABLE, true)", initializer,
                ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"))
    }

    // Add osquery data
    if (attribute.osquery != null) {
        initializer = CodeBlock.of("\$L.setData(\$T.OSQUERY, \"\$L\")", initializer,
                ClassName.get("com.sandpolis.core.instance.state.oid", "OidData"), attribute.osquery)
    }
    val field = FieldSpec
            .builder(ParameterizedTypeName.get(ClassName.get(OID_PACKAGE, "AbsoluteOid"),
                    ParameterizedTypeName.get(ClassName.get(ST_PACKAGE, "STAttribute"),
                            attribute.getAttributeType())),
                    attribute.name, Modifier.PUBLIC, Modifier.FINAL)
            .initializer(initializer)
    oidClass.addField(field.build())
}


fun generateDocument(parent: TypeSpec.Builder?, document: DocumentSpec): TypeSpec.Builder {

    val oidClass = TypeSpec.classBuilder("Oid").addModifiers(Modifier.PUBLIC, Modifier.STATIC).superclass(ParameterizedTypeName
            .get(ClassName.get(OID_PACKAGE, "AbsoluteOid"), ClassName.get(ST_PACKAGE, "STDocument")))

    // Add constructor
    val constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(String::class.java, "path")
            .addStatement("super(\"\$L\", path)", project.name)
    oidClass.addMethod(constructor.build())

    if (parent != null) {
        // Add OID field
        val field = FieldSpec
                .builder(ClassName.get(project.name + ".state", document.className() + "Oid"),
                        document.className().toLowerCase(), Modifier.PUBLIC, Modifier.FINAL) //
                .initializer("new \$L(\"\$L\")", document.className() + "Oid", document.name)
        parent.addField(field.build())
    }
    if (document.attributes != null) {
        for (entry in document.attributes) {
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
                val schema = Klaxon().parseArray<DocumentSpec>(specification.readText())

                // Generate classes
                schema!!.forEach {
                    oidClasses.put(it.name.replace("/+$".toRegex(), ""), generateDocument(oidClasses.get(it.parentPath()), it))
                }

                // Add an "id" field if there isn't one already
                /*val idCount = document.attributes.stream().filter<Boolean> { spec -> spec.id!! }.count()
                if (idCount > 1) {
                    throw RuntimeException("More than one attribute marked with 'id'")
                } else if (idCount == 0L) {
                    // Add id field
                    document.attributes.add(AttributeSpec(name = "id", type = "java.lang.String", id = true))
                }*/

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
