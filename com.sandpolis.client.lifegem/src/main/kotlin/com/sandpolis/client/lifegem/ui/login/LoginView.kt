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

package com.sandpolis.client.lifegem.ui.login

import com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore
import com.sandpolis.client.lifegem.ui.common.pane.CarouselPane
import com.sandpolis.client.lifegem.ui.main.MainView
import com.sandpolis.core.foundation.util.ValidationUtil
import com.sandpolis.core.client.cmd.LoginCmd
import com.sandpolis.core.client.cmd.ServerCmd
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.image.Image
import tornadofx.*
import kotlin.concurrent.timer

class LoginView : View("Login") {

    val controller: LoginController by inject()

    private enum class LoginPhase {
        SERVER_SELECT, DIRECT_USER_SELECT, CLOUD_SERVER_SELECT, DIRECT_PLUGIN_SELECT
    }

    private val model =
        object : ViewModel() {
            val loginPhase = bind { SimpleObjectProperty<LoginPhase>() }
            val bannerImage = bind { SimpleObjectProperty<Image>() }

            // Whether a connection or login attempt is currently pending
            val pending = bind { SimpleBooleanProperty() }
        }

    private val directLoginModel =
        object : ViewModel() {
            val address = bind { SimpleStringProperty() }
            val status = bind { SimpleStringProperty() }
        }

    private val cloudLoginModel =
        object : ViewModel() {
            val username = bind { SimpleStringProperty() }
            val password = bind { SimpleStringProperty() }
            val token = bind { SimpleStringProperty() }
        }

    private val directUserSelectModel =
            object : ViewModel() {
                val address = bind { SimpleStringProperty() }
                val version = bind { SimpleStringProperty() }
            }

    val serverSelection =
        squeezebox(multiselect = false) {
            fold("Direct Login", expanded = true) {
                collapsibleProperty().bind(model.pending.not())
                form {
                    fieldset(labelPosition = Orientation.VERTICAL) {
                        field("Server Address") {
                            textfield(directLoginModel.address) {
                                disableProperty().bind(model.pending)
                                required()
                                validator {
                                    if (text != null) {
                                        val index = text.lastIndexOf(":")
                                        if (index == -1) {
                                            if (!ValidationUtil.address(text)) {
                                                error("Invalid DNS name or IP address")
                                            } else {
                                                null
                                            }
                                        } else {
                                            if (!ValidationUtil.address(text.substring(0, index))) {
                                                error("Invalid DNS name or IP address")
                                            } else if (!ValidationUtil.port(text.substring(index + 1))) {
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
                                    !change.isAdded ||
                                        change.controlNewText.let {
                                            it.matches("^[A-Za-z0-9\\.\\-:]*$".toRegex())
                                        }
                                }
                            }
                        }
                    }
                }
                buttonbar {
                    label(directLoginModel.status)
                    button("Connect") {
                        disableProperty().bind(model.pending)
                        action {
                            directLoginModel.commit {
                                model.pending.set(true)
                                val index = directLoginModel.address.get().lastIndexOf(":")
                                val address = if (index != -1) directLoginModel.address.get().substring(0, index) else directLoginModel.address.get()
                                val port = if (index != -1) directLoginModel.address.get().substring(index + 1).toInt() else 8768

                                directLoginModel.status.set("Attempting connection to: " + address)

                                runAsync {
                                    ConnectionStore.connect(address, port).await()
                                } ui {
                                    if (it.isSuccess()) {
                                        directLoginModel.status.set("Downloading server metadata")
                                        runAsync {
                                            ServerCmd.async().target(it.get()).getBanner().toCompletableFuture().join()
                                        } ui {
                                            directUserSelectModel.version.set(it.getVersion())
                                            model.loginPhase.set(LoginPhase.DIRECT_USER_SELECT)
                                            model.pending.set(false)
                                        }
                                    } else {
                                        directLoginModel.status.set("Failed to connect to specified server")
                                        model.pending.set(false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            fold("Cloud Login") {
                collapsibleProperty().bind(model.pending.not())
                form {
                    fieldset(labelPosition = Orientation.VERTICAL) {
                        field("Username") {
                            textfield(cloudLoginModel.username) {
                                disableProperty().bind(model.pending)
                                required()
                            }
                        }
                        hbox(10) {
                            field("Password") {
                                passwordfield(cloudLoginModel.password) {
                                    disableProperty().bind(model.pending)
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
                        disableProperty().bind(model.pending)
                    }
                }
            }
            fold("Skip Login") {
                collapsibleProperty().bind(model.pending.not())
                text(
                    "It's possible to continue without logging in, but most functionality will be unavailable")
                buttonbar {
                    button("Continue without server") {
                        action {
                            replaceWith(MainView::class, transition = ViewTransition.FadeThrough(1.seconds))
                        }
                    }
                }
            }
        }

    val userSelection =
        squeezebox {
            fold("Server Information", expanded = true) {
                form {
                    fieldset {
                        field("Address") { label(directUserSelectModel.address) }
                        field("Version") { label(directUserSelectModel.version) }
                        field("Certificate") { label("7.0.0") }
                        field("Latency") {
                            progressindicator {
                                timer(daemon = true, period = 1000) {

                                }
                            }
                            label()
                        }
                    }
                }
            }
            fold("User Credentials", expanded = true) {
                isCollapsible = false
                form {
                    fieldset(labelPosition = Orientation.VERTICAL) {
                        field("Username") {
                            textfield() {
                            // required()
                            }
                        }
                        field("Password") {
                            passwordfield() {
                            // required()
                            }
                        }
                        field("2FA Token") {
                            passwordfield() {
                            // required()
                            }
                        }
                    }
                }
            }
        }

    val pluginSelection =
        borderpane {
        // center = tableview(FXCollections.observableArrayList<FxPlugin>(mapTableContent.entries))
        // {
        //   readonlyColumn("Name", FxPlugin::name)
        //  readonlyColumn("Identifier", FxPlugin::packageId)
        // readonlyColumn("Trusted By", FxPlugin::trustAnchor)
        // }
        }

    override val root =
        borderpane {
            top = imageview(resources["/image/sandpolis-640.png"])
            center =  CarouselPane(serverSelection).apply {
                add("", userSelection)
                add("", pluginSelection)
            }
        }
}
