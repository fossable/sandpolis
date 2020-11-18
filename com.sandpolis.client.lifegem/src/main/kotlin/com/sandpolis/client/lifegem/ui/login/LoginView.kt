package com.sandpolis.client.lifegem.ui.login

import com.sandpolis.client.lifegem.common.pane.CarouselPane
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.collections.FXCollections
import tornadofx.*
import com.sandpolis.core.foundation.util.ValidationUtil;

class LoginView: View("Login") {

    val controller: LoginController by inject()

    private val model = object : ViewModel() {
        val directServerAddress = bind { SimpleStringProperty() }
        val cloudUsername = bind { SimpleStringProperty() }
        val cloudPassword = bind { SimpleStringProperty() }
    }

    val nextButton = button("Connect") {
        action {
            model.commit {
                //controller.next()
                carousel.moveForward()
            }
        }
    }

    val serverSelectionPhase = squeezebox(multiselect = false) {
        fold("Direct Login", expanded = true) {
            expandedProperty().onChange {
                if (isExpanded) {
                    nextButton.text = "Connect"
                }
            }
            form {
                fieldset (labelPosition = Orientation.VERTICAL) {
                    field("Server Address") {
                        textfield(model.directServerAddress) {
                            validator {
                                if (!ValidationUtil.address(text)) {
                                    error("Invalid DNS name or IP address")
                                } else {
                                    null
                                }
                            }
                        }
                    }
                }
            }
        }
        fold("Cloud Login") {
            expandedProperty().onChange {
                if (isExpanded) {
                    nextButton.text = "Login"
                }
            }
            form {
                fieldset(labelPosition = Orientation.VERTICAL) {
                    field("Username") {
                        textfield(model.cloudUsername) {
                            //required()
                        }
                    }
                    field("Password") {
                        passwordfield(model.cloudPassword) {
                            //required()
                        }
                    }
                }
            }
        }
        fold("Skip Login") {
            expandedProperty().onChange {
                if (isExpanded) {
                    nextButton.text = "Proceed"
                }
            }
            label("It's possible to continue without logging in, but most functionality will be unavailable")
        }
    }

    val userSelectionPhase = squeezebox {
        fold("Server Information", expanded = true) {
            form {
                fieldset {
                    field("Address") {
                        label("127.0.0.1")
                    }
                    field("Version") {
                        label("7.0.0")
                    }
                    field("Certificate") {
                        label("7.0.0")
                    }
                    field("Latency") {
                        progressindicator {
                            progress = 0.0
                        }
                    }
                }
            }
        }
        fold("User Credentials", expanded = true) {
            isCollapsible = false
            form {
                fieldset(labelPosition = Orientation.VERTICAL) {
                    field("Username") {
                        textfield(model.cloudUsername) {
                            //required()
                        }
                    }
                    field("Password") {
                        passwordfield(model.cloudPassword) {
                            //required()
                        }
                    }
                    field("2FA Token") {
                        passwordfield(model.cloudPassword) {
                            //required()
                        }
                    }
                }
            }
        }
    }

    val pluginSelectionPhase = borderpane {
        //center = tableview(FXCollections.observableArrayList<FxPlugin>(mapTableContent.entries)) {
         //   readonlyColumn("Name", FxPlugin::name)
          //  readonlyColumn("Identifier", FxPlugin::packageId)
           // readonlyColumn("Trusted By", FxPlugin::trustAnchor)
        //}
    }

    val carousel: CarouselPane = CarouselPane("left", 600, serverSelectionPhase, userSelectionPhase)

    override val root = borderpane {
        top = label()
        center = carousel
        bottom = nextButton
    }
}
