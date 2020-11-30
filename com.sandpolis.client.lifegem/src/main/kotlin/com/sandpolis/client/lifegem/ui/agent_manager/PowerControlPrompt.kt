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
import tornadofx.*

class PowerControlPrompt : Fragment() {

    val profile: FxProfile by param()

    override val root = titledpane("Run power operation") {
        collapsibleProperty().set(false)
        button("Power off")
    }
}