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
import tornadofx.Fragment
import tornadofx.button
import tornadofx.titledpane

class PowerControlPrompt : Fragment() {

    val profile: STDocument by param()

    override val root = titledpane("Run power operation") {
        collapsibleProperty().set(false)
        button("Power off")
    }
}