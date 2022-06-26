//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.agent_manager

import org.s7s.core.instance.state.st.STDocument
import javafx.beans.property.ObjectProperty
import javafx.scene.layout.Region
import tornadofx.Fragment
import tornadofx.titledpane

class ShellNewTerminalDialog(val extend: ObjectProperty<Region>) : Fragment() {
    override val root = titledpane("New Terminal Session", collapsible = false) {

    }
}