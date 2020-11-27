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

class ServerManagerView : View("Server Manager") {

    val controller: ServerManagerController by inject()

    val users: ObservableList<FxUser> = FXCollections.observableArrayList()
    val listeners: ObservableList<FxListener> = FXCollections.observableArrayList()
    val groups: ObservableList<FxGroup> = FXCollections.observableArrayList()

    override  val root = drawer {
        item("Servers") {}
        item("Listeners") {
            borderpane {
                top =
                    buttonbar {
                        button("Add") {}

                        button("Delete") {}
                    }
                center =
                    tableview(listeners) {
                        readonlyColumn("Name", FxListener::nameProperty)
                    }
            }
        }
        item("Users") {
            borderpane {
                top =
                    buttonbar {
                        button("Add") {}

                        button("Delete") {}
                    }
                center =
                    tableview(users) {
                        readonlyColumn("Username", FxUser::usernameProperty)
                        //readonlyColumn("Password age", FxUser::name)
                        //readonlyColumn("Last login", FxUser::name)
                        //readonlyColumn("Login address", FxUser::name)
                    }
            }
        }
        item("Agent Groups") {
            borderpane {
                top =
                    buttonbar {
                        button("Add") {}

                        button("Import") {}
                    }
                center =
                    tableview(groups) {
                        readonlyColumn("Name", FxGroup::nameProperty)
                        //readonlyColumn("", FxGroup::name) {
                        //    cellFormat { graphic = button("1") }
                        //}
                    }
            }
        }
    }
}
