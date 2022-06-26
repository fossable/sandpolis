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
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.layout.Region
import tornadofx.*

class GroupCreatorView(val extend: ObjectProperty<Region>) : Fragment() {

    private val model = object : ViewModel() {
        val groupName = bind { SimpleStringProperty() }

        // Whether a connection test is currently pending
        val connectionTestPending = bind { SimpleBooleanProperty() }

        val connectionInterval = bind { SimpleIntegerProperty() }

        val pollingMode = bind { SimpleBooleanProperty() }
        val strictCerts = bind { SimpleBooleanProperty() }
    }

    override val root = borderpane {
        prefHeight = 400.0

        style {
            padding = box(0.px, 15.px)
        }

        center = scrollpane {
            isFitToWidth = true
            squeezebox (multiselect = false) {
                fold("Metadata", expanded = true) {
                    isCollapsible = false
                    form {
                        fieldset {
                            field("Group Name") {
                                textfield(model.groupName) {
                                    tooltip("The descriptive name of the group")
                                    filterInput { change ->
                                        !change.isAdded ||
                                                change.controlNewText.matches("^[A-Za-z0-9 ]*$".toRegex())
                                    }
                                }
                            }
                        }
                    }
                }
                fold("Network", expanded = true) {
                    form {
                        fieldset(labelPosition = Orientation.VERTICAL) {
                            field("Server Address") {
                                textfield {
                                    disableProperty().bind(model.connectionTestPending)
                                    filterInput { change ->
                                        !change.isAdded ||
                                                change.controlNewText.matches("^[A-Za-z0-9\\.\\-]*$".toRegex())
                                    }
                                }
                                button("Test Connection") {
                                    disableProperty().bind(model.connectionTestPending)
                                    tooltip("Attempt a test connection to the server")
                                }
                            }
                            field("Connection Type") {
                                togglegroup {
                                    togglebutton("Continuous") {

                                    }
                                    togglebutton("Polling") {

                                    }
                                }
                            }
                            field("Connection Interval") {
                                spinner(min = 100, max = 100000, editable = true, initialValue = 800, amountToStepBy = 100, property = model.connectionInterval) {
                                    tooltip("The connection interval in milliseconds")
                                }
                                label("ms")
                            }
                            field {
                                checkbox("Strict Certificates", model.strictCerts) {
                                    tooltip(
                                            "Whether the connection will fail if the server's certificate isn't trusted")
                                }
                            }
                        }
                    }
                }
                fold("Authentication") {
                    form {
                        fieldset(labelPosition = Orientation.VERTICAL) {
                            field {
                                togglegroup {
                                    togglebutton("Certificate") {
                                        tooltip("A client certificate will be used to authenticate agents")
                                    }
                                    togglebutton("Password") {
                                        tooltip("The given password will be used to authenticate agents")
                                    }
                                    togglebutton("None") {
                                        tooltip("Agents will not use any form of authentication")
                                    }
                                }
                            }
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
                            fieldset("Plugins") {
                                checkbox("org.s7s.plugin.desktop")
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
                            field("Memory Limit") {
                                slider(min = 1, max = 100, value = 50)
                                label("%")
                            }
                            field("CPU Limit") {
                                slider(min = 1, max = 100, value = 50)
                                label("%")
                            }
                        }
                    }
                }
            }
        }
        bottom = buttonbar {
            button("Create Group") {
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