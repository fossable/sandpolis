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

package com.sandpolis.client.lifegem.ui.about

import com.sandpolis.client.lifegem.Client.UI
import com.sandpolis.client.lifegem.ui.common.pane.CarouselPane
import com.sandpolis.core.foundation.util.ValidationUtil
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import tornadofx.*

class AboutView : View("About") {

    override val root =
        borderpane {
            top = pane()
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
