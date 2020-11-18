package com.sandpolis.client.lifegem.ui.server_manager

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class ServerManagerView: View("Server Manager") {

    val controller: ServerManagerController by inject()

    override val root = tabpane {
        //tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab("Servers") {

        }
        tab("Listeners") {

        }
        tab("Users") {

        }
        tab("Agent Groups") {

        }
    }
}