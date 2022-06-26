//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.about

import org.s7s.instance.client.desktop.init.LifegemLoadUserInterface.UI
import javafx.application.ConditionalFeature
import org.s7s.instance.client.desktop.ui.common.StlUtil
import javafx.animation.Animation
import javafx.scene.SubScene
import javafx.scene.SceneAntialiasing
import javafx.animation.KeyFrame
import javafx.scene.transform.Rotate
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.StringProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.MeshView
import javafx.util.Duration
import java.io.IOException
import tornadofx.*

class AboutView : View("About") {

    private var x = 0.0
    private val xSpeed = 0.1
    private var z = 0.0
    private val zSpeed = 0.0

    override val root =
        stackpane {
            if (Platform.isSupported(ConditionalFeature.SCENE3D)) {

                // Load 3D mesh from resource
                val meshView = MeshView(StlUtil.parse(javaClass.getResourceAsStream("/mesh/sandpolis.stl")))

                // Set position
                meshView.layoutXProperty().bind(Bindings.divide(widthProperty(), 2.0))
                meshView.layoutYProperty().bind(Bindings.divide(heightProperty(), 2.0))

                // Set material
                val sample = PhongMaterial(Color.rgb(247, 213, 145))
                sample.specularColor = Color.rgb(247, 213, 145)
                sample.specularPower = 16.0
                meshView.material = sample
                val kf = KeyFrame(Duration.millis(2.0), {
                    meshView.transforms.setAll(Rotate(x, Rotate.X_AXIS), Rotate(z, Rotate.Z_AXIS))
                    x += xSpeed
                    z += zSpeed
                })
                meshView.addEventHandler(MouseEvent.MOUSE_DRAGGED) { event: MouseEvent? ->
                    // TODO
                }
                val tl = Timeline(kf)
                tl.cycleCount = Animation.INDEFINITE
                tl.play()

                subscene(depthBuffer = true, antiAlias = SceneAntialiasing.BALANCED) {
                    group {
                        add(meshView)
                    }
                }
            } else {
                // Just show a static image
                imageview("/image/view/about/banner.png")
            }
            borderpane {
            center =
                form {
                    fieldset("Build") {
                        field("Java Version") { label("") }
                        field("Gradle Version") { label("") }
                        field("Timestamp") { label("") }
                        field("Platform") { label("") }
                    }
                    fieldset("Runtime") {
                        field("Java Version") { label("") }
                        field("Sandpolis Version") { label("") }
                        field("Application Uptime") { label("") }
                    }
                }
            bottom =
                buttonbar {
                    button("Website") {
                        action {
                            UI.getApplication()
                                .getHostServices()
                                .showDocument("https://sandpolis.com")
                        }
                    }
                    button("Github") {
                        action {
                            UI.getApplication()
                                .getHostServices()
                                .showDocument("https://github.com/sandpolis/sandpolis")
                        }
                    }
                }
            }
        }
}
