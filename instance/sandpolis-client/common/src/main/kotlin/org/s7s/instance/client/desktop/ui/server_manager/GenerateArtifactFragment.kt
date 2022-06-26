//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.server_manager

import org.s7s.core.client.cmd.GroupCmd
import org.s7s.core.instance.Group
import org.s7s.core.instance.state.st.STDocument
import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Region
import tornadofx.*

class GenerateArtifactFragment(val extend: ObjectProperty<Region>) : Fragment() {

    val group: STDocument by param()

    private val model = object : ViewModel() {
        val kiloFormat = bind { SimpleStringProperty() }
        val microFormat = bind { SimpleStringProperty() }
        val nanoFormat = bind { SimpleStringProperty() }

        val cardWidth = bind { SimpleDoubleProperty() }
    }

    override val root = borderpane {
        model.cardWidth.bind(Bindings.divide(widthProperty(), 3))
        center = hbox {
                titledpane("Cross-platform Agent", collapsible = false) {

                    prefWidthProperty().bind(model.cardWidth)

                    label("This is the most commonly used agent because it has the most features. It runs on the JVM, so it's the most resource intensive out of the options.") {
                        isWrapText = true
                        enableWhen(Bindings.isNotNull(model.kiloFormat))
                    }
                    combobox<String>(
                        model.kiloFormat,
                        listOf("Runnable Java Archive (.jar)", "Windows Portable Executable (.exe)")
                    ) {
                        value = "Runnable Java Archive (.jar)"
                        model.kiloFormat.addListener { _, _, n ->
                            if (n != null) {
                                model.microFormat.set(null)
                                model.nanoFormat.set(null)
                            }
                        }
                    }
                }
                titledpane("Native Agent", collapsible = false) {

                    prefWidthProperty().bind(model.cardWidth)

                    label("This agent has fewer features than the cross-platform agent, but is implemented in Rust for a significant performance improvement.") {
                        isWrapText = true
                        enableWhen(Bindings.isNotNull(model.microFormat))
                    }
                    combobox<String>(
                        model.microFormat,
                        listOf("Executable and Linkable Format", "Windows Portable Executable (.exe)")
                    ) {
                        model.microFormat.addListener { _, _, n ->
                            if (n != null) {
                                model.kiloFormat.set(null)
                                model.nanoFormat.set(null)
                            }
                        }
                    }
                }
                titledpane("Minimal Native Agent", collapsible = false) {

                    prefWidthProperty().bind(model.cardWidth)

                    label("This agent has the fewest features, but is extremely lightweight which makes it suitable for embedded applications.") {
                        isWrapText = true
                        enableWhen(Bindings.isNotNull(model.nanoFormat))
                    }
                    combobox<String>(
                        model.nanoFormat,
                        listOf("Executable and Linkable Format", "Windows Portable Executable (.exe)")
                    ) {
                        model.nanoFormat.addListener { _, _, n ->
                            if (n != null) {
                                model.kiloFormat.set(null)
                                model.microFormat.set(null)
                            }
                        }
                    }
                }
        }
        bottom = buttonbar {
            button("Generate") {
                action {
                    runAsync {
                        val group = Group.GroupConfig.newBuilder()
                        GroupCmd.async().create(group.build()).toCompletableFuture().join()
                    } ui {
                        extend.set(null)
                    }
                }
            }
            button("Cancel") {
                action {
                    extend.set(null)
                }
            }
        }
    }
}
