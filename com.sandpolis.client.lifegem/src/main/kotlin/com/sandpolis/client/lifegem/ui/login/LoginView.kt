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

import com.sandpolis.client.lifegem.state.FxPlugin
import com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore
import com.sandpolis.client.lifegem.ui.common.pane.CarouselPane
import com.sandpolis.client.lifegem.ui.main.MainView
import com.sandpolis.core.foundation.util.ValidationUtil
import com.sandpolis.core.client.cmd.LoginCmd
import com.sandpolis.core.client.cmd.ServerCmd
import com.sandpolis.core.net.connection.Connection
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.image.Image
import javafx.animation.Timeline
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.util.Duration
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.collections.ObservableList
import java.util.Timer
import tornadofx.*
import kotlin.concurrent.timer
import com.sandpolis.core.instance.state.oid.InstanceOid
import com.sandpolis.core.net.state.STCmd
import java.util.Objects

class LoginView : View("Login") {

    val controller: LoginController by inject()

    /**
     * The amount of time to wait between pings in milliseconds.
     */
    private val PING_PERIOD: Long = 1500

    private enum class LoginPhase {
        SERVER_SELECT, DIRECT_USER_SELECT, CLOUD_SERVER_SELECT, DIRECT_PLUGIN_SELECT
    }

    private val model =
        object : ViewModel() {
            val loginPhase = bind { SimpleObjectProperty<LoginPhase>() }
            val bannerImage = bind { SimpleObjectProperty<Image>() }
            lateinit var connection: Connection

            // Whether a connection or login attempt is currently pending
            val pending = bind { SimpleBooleanProperty() }
        }

    private val directLoginModel =
        object : ViewModel() {
            val address = bind { SimpleStringProperty() }
            val status = bind { SimpleStringProperty() }
            val status_color = bind { SimpleObjectProperty<Paint>(Color.BLACK) }
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
                val latency_timer = bind { SimpleObjectProperty<Timer>() }
                val latency_visual = bind { SimpleDoubleProperty() }
                val latency = bind { SimpleStringProperty() }
                val version = bind { SimpleStringProperty() }
                val username = bind { SimpleStringProperty() }
                val password = bind { SimpleStringProperty() }
                val token = bind { SimpleStringProperty() }
                val status = bind { SimpleStringProperty() }
                val status_color = bind { SimpleObjectProperty<Paint>(Color.BLACK) }
            }

    /**
     * Scale the ping approximation so it's easier for the user to distinguish
     * between small pings and large pings.
     *
     * @param ping The last ping value
     * @return A duration representative of the given ping value
     */
    private fun calculatePingVisual(ping: Long): Duration {
        return Duration.millis(java.lang.Math.min(PING_PERIOD, 4 * ping + 80).toDouble())
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
                    label(directLoginModel.status) {
                        textFillProperty().bind(directLoginModel.status_color)
                    }
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
                                        model.connection = it.get()
                                        directLoginModel.status.set("Downloading server metadata")
                                        runAsync {
                                            ServerCmd.async().target(it.get()).getBanner().toCompletableFuture().join()
                                        } ui {
                                            directLoginModel.status.set("")

                                            // Set banner information
                                            directUserSelectModel.version.set(it.getVersion())
                                            directUserSelectModel.address.set(model.connection.getRemoteAddress())

                                            // Advance the phase
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

    init {
        model.loginPhase.addListener { _,_,n ->
            if (n == LoginPhase.DIRECT_USER_SELECT) {
                directUserSelectModel.latency_timer.set(timer(daemon = true, period = 1000) {
                    runAsync {
                        ServerCmd.async().target(model.connection).ping().toCompletableFuture().join()
                    } ui {
                        // Run the indicator animation
                        directUserSelectModel.latency_visual.set(0.0)
                        directUserSelectModel.latency.set("${it} ms")
                        Timeline(KeyFrame(calculatePingVisual(it), KeyValue(directUserSelectModel.latency_visual, 1.0))).play()
                    }
                })
            } else {
                // Cancel the timer
                if (directUserSelectModel.latency_timer.get() != null) {
                    directUserSelectModel.latency_timer.get().cancel()
                }
            }
        }
    }

    val userSelection =
        squeezebox(fillHeight = false) {
            fold("Server Information", expanded = true) {
                isCollapsible = false
                form {
                    fieldset {
                        field("Address") { label(directUserSelectModel.address) }
                        field("Version") { label(directUserSelectModel.version) }
                        field("Certificate") { label("7.0.0") }
                        field("Latency") {
                            hbox(10) {
                                progressindicator {
                                    progressProperty().bind(directUserSelectModel.latency_visual)
                                }
                                label(directUserSelectModel.latency)
                            }
                        }
                    }
                }
            }
            fold("User Credentials", expanded = true) {
                isCollapsible = false
                form {
                    fieldset(labelPosition = Orientation.VERTICAL) {
                        hbox(10) {
                            field("Username") {
                                textfield(directUserSelectModel.username) {
                                    //required()
                                }
                            }
                            field("Password") {
                                passwordfield(directUserSelectModel.password) {
                                    //required()
                                }
                            }
                            field("2FA Token") {
                                textfield(directUserSelectModel.token) {
                                    //required()
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
                    buttonbar {
                        label(directUserSelectModel.status)
                        button("Login"){
                            action {
                                directUserSelectModel.status.set("Attempting login")
                                directUserSelectModel.status_color.set(Color.BLACK)
                                runAsync {
                                    LoginCmd.async().target(model.connection).login(directUserSelectModel.username.get(), directUserSelectModel.password.get()).toCompletableFuture().join()
                                } ui {
                                    if (it.getResult()) {
                                        directUserSelectModel.status.set("Loading plugins")
                                        runAsync {
                                            //STCmd.async().target(model.connection).snapshot(InstanceOid.InstanceOid().profile(model.connection.getRemoteUuid()).plugin).toCompletableFuture().join()
                                        } ui {
                                            directUserSelectModel.status.set("")
                                            replaceWith(MainView::class, transition = ViewTransition.FadeThrough(1.seconds))
                                        }
                                    } else {
                                        model.pending.set(false)
                                        directUserSelectModel.status.set("Login failed")
                                        directUserSelectModel.status_color.set(Color.RED)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    val plugins: ObservableList<FxPlugin> = FXCollections.observableArrayList()

    val pluginSelection = borderpane {
        center = tableview(plugins) {
            readonlyColumn("Name", FxPlugin::nameProperty)
            readonlyColumn("Identifier", FxPlugin::packageIdProperty)
        }
    }

    override val root = borderpane {
        top = borderpane {
            style {
                padding = box(8.px, 8.px, 8.px, 8.px)
            }
            center = imageview(resources["/image/sandpolis-640.png"])
        }
        center =  CarouselPane(serverSelection).apply {
            add(LoginPhase.DIRECT_USER_SELECT.name, userSelection)
            add(LoginPhase.DIRECT_PLUGIN_SELECT.name, pluginSelection)
            model.loginPhase.addListener { _,_,n ->
                moveTo(n.name)
            }
        }
    }

    /*private fun setBannerImage(nextImage: Image) {
        Objects.requireNonNull(nextImage)
        if (nextImage == bannerImage.getImage()) return
        val fade = FadeTransition(Duration.millis(300), bannerImage)
        fade.setFromValue(1.0)
        fade.setToValue(0.0)
        fade.setOnFinished({ event ->
            bannerImage.setImage(nextImage)
            fade.setOnFinished(null)
            fade.setRate(-fade.getRate())
            fade.play()
        })
        fade.play()
    }*/
}
