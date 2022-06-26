//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.client.lifegem

import org.s7s.core.instance.state.st.STDocument
import org.s7s.plugin.shell.client.lifegem.TerminalView
import org.s7s.instance.client.desktop.plugin.AgentViewExtension
import tornadofx.*

class ShellView : AgentViewExtension("Shell") {
    override fun nowVisible(profile: STDocument) {
    }

    override fun nowInvisible() {
    }

    val term = TerminalView()

    override val root = titledpane("Shell") {
        content = term
    }

    init {
        term.prefWidthProperty().bind(this.root.widthProperty())
        term.prefHeightProperty().bind(this.root.heightProperty())
    }
}