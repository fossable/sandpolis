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

package com.sandpolis.client.lifegem.ui.main

import com.sandpolis.client.lifegem.ui.common.pane.CarouselPane
import com.sandpolis.client.lifegem.state.FxProfile
import javafx.collections.ObservableList
import javafx.collections.FXCollections
import tornadofx.*

class MainView : View("Main") {

    val profiles: ObservableList<FxProfile> = FXCollections.observableArrayList()

    val hostList = tableview(profiles) {
        readonlyColumn("UUID", FxProfile::uuidProperty)
    }

    override val root = borderpane {
        top {
            menubar {
                menu("Interface") {
                    item("List View", "Shortcut+L")
                    item("Graph View", "Shortcut+G")
                    separator()
                    item("Move to system tray") {
                        tooltip("The application will continue running in the background and the UI will be hidden.")
                    }
                }
                menu("Management")
                menu("Help") {
                    item("About")
                }
            }
        }
        center = CarouselPane(hostList).apply {

        }
    }
}