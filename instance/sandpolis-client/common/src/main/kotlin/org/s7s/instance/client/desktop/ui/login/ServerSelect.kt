//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.login

import org.s7s.instance.client.desktop.ui.main.MainView
import org.s7s.core.client.cmd.ServerCmd
import org.s7s.core.foundation.S7SString
import org.s7s.core.instance.state.InstanceOids.ConnectionOid
import org.s7s.core.instance.connection.ConnectionStore
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import tornadofx.*

class ServerSelect(val parentView: LoginView) : Fragment() {

    private val directLoginModel = object : ViewModel() {
        val address = bind { SimpleStringProperty() }
        val status = bind { SimpleStringProperty() }
        val status_color = bind { SimpleObjectProperty<Paint>(Color.BLACK) }
    }

    private val cloudLoginModel = object : ViewModel() {
        val username = bind { SimpleStringProperty() }
        val password = bind { SimpleStringProperty() }
        val token = bind { SimpleStringProperty() }
    }

    override val root = squeezebox(multiselect = false) {
        fold("Direct Login", expanded = true) {
            collapsibleProperty().bind(parentView.model.pending.not())
            form {
                fieldset(labelPosition = Orientation.VERTICAL) {
                    field("Server Address") {
                        textfield(directLoginModel.address) {
                            disableProperty().bind(parentView.model.pending)
                            required()
                            validator {
                                if (text != null) {
                                    val index = text.lastIndexOf(":")
                                    if (index == -1) {
                                        if (!S7SString.of(text).isIPv4()) {
                                            error("Invalid DNS name or IP address")
                                        } else {
                                            null
                                        }
                                    } else {
                                        if (!S7SString.of(text.substring(0, index)).isIPv4()) {
                                            error("Invalid DNS name or IP address")
                                        } else if (!S7SString.of(text.substring(index + 1)).isPort()) {
                                            error("Invalid port")
                                        } else {
                                            null
                                        }
                                    }
                                } else {
                                    null
                                }
                            }
                            filterInput { change ->
                                !change.isAdded || change.controlNewText.matches("^[A-Za-z0-9\\.\\-:]*$".toRegex())
                            }
                        }
                    }
                }
            }
            buttonbar {
                label(directLoginModel.status) {
                    textFillProperty().bind(directLoginModel.status_color)
                }
                button("Connect") {
                    disableProperty().bind(parentView.model.pending)
                    action {
                        directLoginModel.commit {
                            parentView.model.pending.set(true)
                            val index = directLoginModel.address.get().lastIndexOf(":")
                            val address = if (index != -1) directLoginModel.address.get()
                                .substring(0, index) else directLoginModel.address.get()
                            val port =
                                if (index != -1) directLoginModel.address.get().substring(index + 1).toInt() else 8768

                            directLoginModel.status.set("Attempting connection to: $address")

                            runAsync {
                                ConnectionStore.ConnectionStore.connect(address, port).await()
                            } ui {
                                if (it.isSuccess) {
                                    parentView.model.connection = it.get()
                                    directLoginModel.status.set("Downloading server metadata")
                                    runAsync {
                                        ServerCmd.async().target(it.get()).banner.toCompletableFuture().join()
                                    } ui {
                                        directLoginModel.status.set("")

                                        // Set banner information
                                        parentView.model.bannerVersion.set(it.version)
                                        parentView.model.serverAddress.set(
                                            parentView.model.connection.get(ConnectionOid.REMOTE_ADDRESS).asString()
                                        )

                                        if (parentView.model.connection.get(ConnectionOid.CERTIFICATE_VALID).asBoolean()) {
                                            parentView.model.serverCertStatus.set("Certificate Valid")
                                        } else {
                                            parentView.model.serverCertStatus.set("Certificate Invalid")
                                        }

                                        // Advance the phase
                                        parentView.model.loginPhase.set(LoginView.LoginPhase.DIRECT_USER_SELECT)
                                        parentView.model.pending.set(false)
                                    }
                                } else {
                                    directLoginModel.status.set("Failed to connect to specified server")
                                    parentView.model.pending.set(false)
                                }
                            }
                        }
                    }
                }
            }
        }
        fold("Cloud Login") {
            collapsibleProperty().bind(parentView.model.pending.not())
            form {
                fieldset(labelPosition = Orientation.VERTICAL) {
                    field("Username") {
                        textfield(cloudLoginModel.username) {
                            disableProperty().bind(parentView.model.pending)
                            required()
                        }
                    }
                    hbox(10) {
                        field("Password") {
                            passwordfield(cloudLoginModel.password) {
                                disableProperty().bind(parentView.model.pending)
                                required()
                            }
                        }
                        field("2FA Token") {
                            textfield(cloudLoginModel.token) {
                                required()
                                prefColumnCountProperty().set(6)
                                filterInput { change ->
                                    !change.isAdded ||
                                            change.controlNewText.let {
                                                it.matches("^[0-9]*$".toRegex()) && it.length <= 6
                                            }
                                }
                            }
                        }
                    }
                }
            }
            buttonbar {
                button("Login") {
                    disableProperty().bind(parentView.model.pending)
                }
            }
        }
        fold("Skip Login") {
            collapsibleProperty().bind(parentView.model.pending.not())
            text(
                "It's possible to continue without logging in, but most functionality will be unavailable"
            )
            buttonbar {
                button("Continue without server") {
                    action {
                        replaceWith(MainView::class, transition = ViewTransition.FadeThrough(1.seconds))
                    }
                }
            }
        }
    }
}