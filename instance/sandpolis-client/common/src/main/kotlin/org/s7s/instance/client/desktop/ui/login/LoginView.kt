//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.login

import org.s7s.instance.client.desktop.ui.common.pane.CarouselPane
import org.s7s.instance.client.desktop.ui.main.MainView
import org.s7s.core.instance.state.st.STDocument
import org.s7s.core.instance.connection.Connection
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.Image
import tornadofx.*

class LoginView : View("Login") {

    enum class LoginPhase {
        SERVER_SELECT, DIRECT_USER_SELECT, CLOUD_SERVER_SELECT, DIRECT_PLUGIN_SELECT, COMPLETE
    }

    class LoginViewModel : ViewModel() {
        val loginPhase = bind { SimpleObjectProperty<LoginPhase>() }
        val bannerImage = bind { SimpleObjectProperty<Image>() }
        val bannerVersion = bind { SimpleStringProperty() }
        val serverAddress = bind { SimpleStringProperty() }
        val serverCertStatus = bind { SimpleStringProperty() }
        lateinit var connection: Connection

        val plugins: ObservableList<STDocument> = FXCollections.observableArrayList()

        // Whether a connection or login attempt is currently pending
        val pending = bind { SimpleBooleanProperty() }
    }
    val model = LoginViewModel()

    override val root = borderpane {
        top = borderpane {
            style {
                padding = box(8.px, 8.px, 8.px, 8.px)
            }
            center = imageview(resources["/image/sandpolis-640.png"])
        }
        center = CarouselPane(ServerSelect(this@LoginView).root).apply {
            add(LoginPhase.DIRECT_USER_SELECT.name, UserSelect(this@LoginView).root)
            add(LoginPhase.DIRECT_PLUGIN_SELECT.name, PluginSelect(this@LoginView).root)
            model.loginPhase.addListener { _, _, n ->
                if (n == LoginPhase.COMPLETE) {
                    replaceWith(MainView::class, transition = ViewTransition.FadeThrough(1.seconds))
                } else {
                    moveTo(n.name)
                }
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
