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
import org.s7s.instance.client.desktop.plugin.AgentViewExtension
import tornadofx.*

class InventoryView : AgentViewExtension("Inventory") {

    override val root = squeezebox {
        fold("Metadata") {
            form {
                fieldset {
                    field("First contact") {
                        label()
                    }
                    field("Last contact") {
                        label()
                    }
                }
            }
        }
        fold("Settings") {}

        fold("Plugins") {}
    }

    override fun nowVisible(profile: STDocument) {

    }

    override fun nowInvisible() {
        
    }
}
