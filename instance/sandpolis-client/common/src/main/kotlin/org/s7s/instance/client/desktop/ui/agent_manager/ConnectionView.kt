//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.agent_manager

import org.s7s.instance.client.desktop.plugin.AgentViewExtension
import org.s7s.core.instance.state.st.STDocument
import org.s7s.core.instance.state.st.entangled.EntangledDocument
import tornadofx.*
import java.util.concurrent.CompletionStage

class ConnectionView : AgentViewExtension("Inventory") {

    var entangled: CompletionStage<EntangledDocument>? = null

    override val root = vbox {
        titledpane("Network Connections") {
        }
    }

    override fun nowVisible(profile: STDocument) {
    //    entangled = STCmd.async().sync(
    //        InstanceOids.InstanceOids().profile(profile.attribute(InstanceOids.ProfileOid.UUID).asString()).bootagent.gptpartition)
    }

    override fun nowInvisible() {
        entangled?.thenApply { it.close() }
    }
}