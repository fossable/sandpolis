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
import org.s7s.instance.client.desktop.ui.common.SvgUtil
import org.s7s.instance.client.desktop.ui.Events.MainMenuOpenEvent
import org.s7s.instance.client.desktop.ui.Events.MainViewChangeEvent
import org.s7s.instance.client.desktop.ui.common.tray.Tray;
import org.s7s.instance.client.desktop.ui.server_manager.ServerManagerView
import javafx.geometry.Orientation
import tornadofx.*

class SideMenuView : Fragment() {

    val interfaceMenu = titledpane("Interface", collapsible = false) {

        graphic = SvgUtil.getSvg("/image/common/terminal-window-line.svg", 24.0, 24.0, textFillProperty())

        vbox {
            button ("List View") {
                action {
                    fire(MainViewChangeEvent("list"))
                }
            }

            button ("Graph View") {
                action {
                    fire(MainViewChangeEvent("graph"))
                }
            }
        }

        vbox {
            button("Hide interface") {
                setDisable(!Tray.isSupported()) // TODO this seems to be slow
                tooltip("The application will continue running in the background and the UI will be hidden.")
            }
        }
    }

    val aboutMenu = titledpane("About", collapsible = false) {
        vbox {
            button("About") {
                action {
                    find<AboutView>().openWindow()
                }

                tooltip("Show the about window")
            }
            button("Open Documentation") {
                action {
                    find<ServerManagerView>().openWindow()
                }
            }
        }
    }

    override val root = toolbar {
        setOrientation(Orientation.VERTICAL)

        button {
            graphic = SvgUtil.getSvg("/image/common/terminal-window-line.svg", 16.0, 16.0, textFillProperty())

            action {
                fire(MainMenuOpenEvent(interfaceMenu))
            }
        }

        button {
            graphic = SvgUtil.getSvg("/image/common/settings-5-line.svg", 16.0, 16.0, textFillProperty())
        }

        button {
            graphic = SvgUtil.getSvg("/image/common/settings-5-line.svg", 16.0, 16.0, textFillProperty())
        }

        button {
            graphic = SvgUtil.getSvg("/image/common/question-line.svg", 16.0, 16.0, textFillProperty())

            action {
                fire(MainMenuOpenEvent(aboutMenu))
            }
        }
    }
}
