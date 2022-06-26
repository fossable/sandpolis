//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.server_manager

import org.s7s.instance.client.desktop.ui.common.FxUtil
import org.s7s.core.instance.state.InstanceOids.InstanceOids
import org.s7s.core.instance.state.InstanceOids.GroupOid.OperationOid
import org.s7s.core.instance.state.st.STDocument
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.Region
import tornadofx.*

class GroupOperationLog(val extend: ObjectProperty<Region>, val group: STDocument) : Fragment() {

    override val root = titledpane("Operation Log", collapsible = false) {
        /*content = tableview(FxUtil.newObservable(InstanceOids().group(group.oid().last()).operation)) {
            column<STDocument, String>("Start Time") {
                FxUtil.newProperty(it.value.attribute(OperationOid.START_TIME))
            }
            column<STDocument, ProgressIndicator>("Progress") {
                SimpleObjectProperty(progressindicator {
                    progressProperty().bind(FxUtil.newProperty(it.value.attribute(OperationOid.PROGRESS)))
                })
            }
        }*/
    }
}