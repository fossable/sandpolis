//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.main

import org.s7s.instance.client.desktop.ui.about.AboutView
import org.s7s.instance.client.desktop.ui.server_manager.ServerManagerView
import tornadofx.*

class RegularMenuView : Fragment() {

    override val root = menubar {
        menu("Interface") {
            item("List View", "Shortcut+L")
            item("Graph View", "Shortcut+G")
            separator()
            item("Move to system tray") {
                tooltip("The application will continue running in the background and the UI will be hidden.")
            }
        }
        menu("Management") {
            item("Server Manager") {
                action {
                    find<ServerManagerView>().openWindow()
                }
            }
        }
        menu("Help") {
            item("About") {
                action {
                    find<AboutView>().openWindow()
                }
                tooltip("Show the about window")
            }
        }
    }
}
