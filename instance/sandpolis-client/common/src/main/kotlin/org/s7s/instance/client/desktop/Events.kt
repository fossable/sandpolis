//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui

import javafx.scene.control.TitledPane
import tornadofx.*

object Events {

    class MainViewChangeEvent(val view: String) : FXEvent()

    class MainMenuOpenEvent(val view: TitledPane) : FXEvent()
}
