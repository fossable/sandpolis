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

package com.sandpolis.client.lifegem.ui.agent_manager

import com.sandpolis.client.lifegem.ui.common.pane.ExtendPane
import com.sandpolis.client.lifegem.state.FxProfile
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import com.sandpolis.core.instance.state.st.ephemeral.EphemeralDocument
import tornadofx.*

class AgentManagerView : Fragment() {

    val profile: FxProfile by param()

    private val model =
            object : ViewModel() {
                val extendBottom = bind { SimpleObjectProperty<Region>() }
            }

    val menuList = vbox {

    }

    val content: Region = borderpane {
        center = label("center")
    }

    override val root =
        borderpane {
            left =
                titledpane("Hostname") {
                    setCollapsible(false)
                    setPrefWidth(100.0)
                    vbox {
                        label("hostname")
                        label("public ip")
                        label("os")
                    }
                    flowpane {
                        setHgap(10.0)
                        setVgap(10.0)
                        setAlignment(Pos.CENTER)

                        button("P") {
                            tooltip("Poweroff the host")
                        }
                        button("R") {
                            tooltip("Restart the host")
                        }
                        button("C") {
                            tooltip("Establish a persistent connection to the host")
                        }
                        button("T") {
                            tooltip("Terminate the persistent connection to the host")
                        }
                    }
                    vbox {

                    }
                }
            center = ExtendPane(content).apply {
                regionBottomProperty().bind(model.extendBottom)
            }
        }
}
