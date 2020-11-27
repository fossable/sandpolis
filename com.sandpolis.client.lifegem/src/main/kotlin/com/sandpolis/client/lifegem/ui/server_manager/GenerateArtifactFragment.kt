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

package com.sandpolis.client.lifegem.ui.server_manager

import com.sandpolis.client.lifegem.ui.common.pane.ExtendPane
import com.sandpolis.client.lifegem.state.FxGroup
import com.sandpolis.client.lifegem.state.FxListener
import com.sandpolis.client.lifegem.state.FxUser
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.layout.Region
import tornadofx.*

class GenerateArtifactFragment : Fragment() {

    val group: FxGroup by param()

    private val model = object : ViewModel() {
        val artifactType = bind { SimpleStringProperty() }
        val artifactFormat = bind { SimpleStringProperty() }
    }

    // The valid vanilla formats
    val vanillaFormats = listOf(".jar", ".go", ".sh", ".exe", ".py").asObservable()

    // The valid micro formats
    val microFormats = listOf(".elf", ".exe").asObservable()

    override val root = form {
        fieldset {
            field("Agent executable") {
                combobox<String>(model.artifactType) {
                    items = listOf("Vanilla", "Micro").asObservable()
                }
                combobox<String>(model.artifactFormat, vanillaFormats) {
                    model.artifactType.addListener { p, o, n ->
                        items = vanillaFormats
                    }
                }
            }
        }
        buttonbar {
            button("Generate")
        }
    }
}
