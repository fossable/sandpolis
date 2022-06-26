//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common

import org.s7s.core.instance.state.st.STDocument
import javafx.scene.control.TreeItem
import tornadofx.*

class STView(val document: STDocument) : Fragment() {

    override val root = treeview<STDocument> {
        root = TreeItem(document)

        cellFormat {
            text = it.oid().last()
        }

        populate { parent ->
            if (parent.value.documentCount() == 0) {
                null
            } else {
                FxUtil.newObservable(parent.value.oid())
            }
        }
    }
}