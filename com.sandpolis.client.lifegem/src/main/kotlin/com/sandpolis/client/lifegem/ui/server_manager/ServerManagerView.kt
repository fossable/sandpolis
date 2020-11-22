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
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.layout.Region
import tornadofx.*

class ServerManagerView : View("Server Manager") {

    class FxListener(val name: String) {}

    class FxUser(val name: String) {}

    class FxGroup(val name: String) {}

    val controller: ServerManagerController by inject()

    override val root =
        form {
            fieldset {
                field("Agent executable") {
                    combobox<String> { items = listOf("Vanilla", "Micro").asObservable() }
                    combobox<String> {
                        items = listOf(".jar", ".go", ".sh", ".exe", ".py").asObservable()
                    }
                }
            }
        }

    val root3 =
        borderpane {
            center =
                scrollpane {
                    setFitToWidth(true)
                    squeezebox {
                        fold("Metadata", expanded = true) {
                            setCollapsible(false)
                            form { fieldset { field("Group Name") { textfield() } } }
                        }
                        fold("Network") {
                            form {
                                fieldset(labelPosition = Orientation.VERTICAL) {
                                    field("Server Address") {
                                        textfield {
                                            filterInput { change ->
                                                !change.isAdded ||
                                                    change.controlNewText.let {
                                                        it.matches("^[A-Za-z0-9\\.\\-]*$".toRegex())
                                                    }
                                            }
                                        }
                                        button("Test Connection")
                                    }
                                    field("Connection Interval") {
                                        textfield() {
                                            tooltip("The connection interval in milliseconds")
                                        }
                                    }
                                    field {
                                        checkbox("Strict Certificates") {
                                            tooltip(
                                                "Whether the connection will fail if the server's certificate isn't trusted")
                                        }
                                        checkbox("Polling Mode") {
                                            tooltip(
                                                "Whether the connection will terminate if the server has nothing to send")
                                        }
                                    }
                                }
                            }
                        }
                        fold("Authentication") {
                            togglegroup {
                                radiobutton("Certificate") {
                                    tooltip(
                                        "A client certificate will be used to identify agents to the server")
                                }
                                radiobutton("Password")
                                radiobutton("No authentication") {
                                    tooltip("Agents will not use any form of authentication")
                                }
                            }
                        }
                        fold("Features") {
                            form {
                                hbox {
                                    fieldset("Platforms") {
                                        checkbox("Linux")
                                        checkbox("Windows")
                                        checkbox("macOS")
                                    }
                                    fieldset("Architectures") {
                                        checkbox("x86")
                                        checkbox("x86_64")
                                    }
                                }
                            }
                        }
                        fold("Runtime") {
                            checkbox("Remove installer on success")
                            checkbox("Recover from failures")
                            checkbox("Request highest privileges")
                            form {
                                fieldset {
                                    field("Memory Reservation") { slider(100) }
                                    field("CPU Limit") { slider(100) }
                                }
                            }
                        }
                    }
                }
            bottom = buttonbar { button("Create Group") }
        }

    val root2 =
        drawer {
            item("Servers") {}
            item("Listeners") {
                borderpane {
                    top =
                        buttonbar {
                            button("Add") {}

                            button("Delete") {}
                        }
                    center =
                        tableview(listOf(FxListener("test")).asObservable()) {
                            readonlyColumn("Name", FxListener::name)
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
                        tableview(listOf(FxUser("test")).asObservable()) {
                            readonlyColumn("Username", FxUser::name)
                            readonlyColumn("Password age", FxUser::name)
                            readonlyColumn("Last login", FxUser::name)
                            readonlyColumn("Login address", FxUser::name)
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
                        tableview(listOf(FxGroup("test")).asObservable()) {
                            readonlyColumn("Name", FxGroup::name)
                            readonlyColumn("", FxGroup::name) {
                                cellFormat { graphic = button("1") }
                            }
                        }
                }
            }
        }
}
