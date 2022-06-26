//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.login

import org.s7s.core.instance.state.InstanceOids.ProfileOid.PluginOid
import org.s7s.core.instance.state.InstanceOids.ConnectionOid
import org.s7s.core.client.cmd.LoginCmd
import org.s7s.core.client.cmd.ServerCmd
import org.s7s.core.instance.plugin.PluginStore
import org.s7s.core.instance.state.oid.Oid
import org.s7s.core.instance.state.STCmd
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.util.Duration
import tornadofx.*
import java.util.*
import kotlin.concurrent.timer
import org.s7s.core.protocol.Session.RS_Login;

class UserSelect(val parentView: LoginView) : Fragment() {

    private val model = object : ViewModel() {
        val latency_timer = bind { SimpleObjectProperty<Timer>() }
        val latency_visual = bind { SimpleDoubleProperty() }
        val latency = bind { SimpleStringProperty() }
        val username = bind { SimpleStringProperty() }
        val password = bind { SimpleStringProperty() }
        val token = bind { SimpleStringProperty() }
        val status = bind { SimpleStringProperty() }
        val status_color = bind { SimpleObjectProperty<Paint>(Color.BLACK) }
    }

    override val root = squeezebox(fillHeight = false) {
        fold("Server Information", expanded = true) {
            isCollapsible = false
            form {
                fieldset {
                    field("Address") { label(parentView.model.serverAddress) }
                    field("Version") { label(parentView.model.bannerVersion) }
                    field("Certificate") { label(parentView.model.serverCertStatus) }
                    field("Latency") {
                        hbox(10) {
                            progressindicator {
                                progressProperty().bind(model.latency_visual)
                            }
                            label(model.latency)
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
                            textfield(model.username) {
                                //required()
                            }
                        }
                        field("Password") {
                            passwordfield(model.password) {
                                //required()
                            }
                        }
                        field("2FA Token") {
                            textfield(model.token) {
                                //required()
                                prefColumnCountProperty().set(6)
                                filterInput { change ->
                                    !change.isAdded || change.controlNewText.let {
                                        it.matches("^[0-9]*$".toRegex()) && it.length <= 6
                                    }
                                }
                            }
                        }
                    }
                }
                buttonbar {
                    label(model.status)
                    button("Login") {
                        action {
                            model.status.set("Attempting login")
                            model.status_color.set(Color.BLACK)
                            runAsync {
                                LoginCmd.async().target(parentView.model.connection)
                                    .login(model.username.get(), model.password.get())
                                    .toCompletableFuture().join()
                            } ui {
                                if (it == RS_Login.LOGIN_OK) {
                                    model.status.set("Loading plugins")
                                    runAsync {
                                        STCmd.async().target(parentView.model.connection).snapshot(Oid.of("/profile/*/plugin", parentView.model.connection.get(ConnectionOid.REMOTE_UUID).asString())).toCompletableFuture().join()
                                    } ui {
                                        model.status.set("")

                                        // Check for missing plugins
                                        var nextPhase = LoginView.LoginPhase.COMPLETE
                                        it.forEachDocument {
                                            parentView.model.plugins.add(it)

                                            if (PluginStore.PluginStore.getByPackageId(it.attribute(PluginOid.PACKAGE_ID).asString()).isEmpty) {
                                                nextPhase = LoginView.LoginPhase.DIRECT_PLUGIN_SELECT
                                            }
                                        }

                                        parentView.model.loginPhase.set(nextPhase)
                                    }
                                } else {
                                    parentView.model.pending.set(false)
                                    model.status.set("Login failed")
                                    model.status_color.set(Color.RED)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        parentView.model.loginPhase.addListener { _, _, n ->
            if (n == LoginView.LoginPhase.DIRECT_USER_SELECT) {
                model.latency_timer.set(timer(daemon = true, period = 1000) {
                    runAsync {
                        ServerCmd.async().target(parentView.model.connection).ping().toCompletableFuture().join()
                    } ui {
                        // Run the indicator animation
                        model.latency_visual.set(0.0)
                        model.latency.set("$it ms")
                        Timeline(
                            KeyFrame(
                                calculatePingVisual(it),
                                KeyValue(model.latency_visual, 1.0)
                            )
                        ).play()
                    }
                })
            } else {
                // Cancel the timer
                if (model.latency_timer.get() != null) {
                    model.latency_timer.get().cancel()
                }
            }
        }
    }

    /**
     * The amount of time to wait between pings in milliseconds.
     */
    private val PING_PERIOD: Long = 1500

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
}