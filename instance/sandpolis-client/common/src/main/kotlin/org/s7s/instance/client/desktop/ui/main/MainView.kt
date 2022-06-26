//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.main

import org.s7s.instance.client.desktop.ui.Events.MainMenuOpenEvent
import org.s7s.instance.client.desktop.ui.Events.MainViewChangeEvent
import org.s7s.instance.client.desktop.ui.agent_manager.AgentManagerView
import org.s7s.instance.client.desktop.ui.common.FxUtil
import org.s7s.instance.client.desktop.ui.common.pane.CarouselPane
import org.s7s.instance.client.desktop.ui.common.pane.ExtendPane
import org.s7s.core.foundation.Platform
import org.s7s.core.instance.state.InstanceOids.ProfileOid.AgentOid
import org.s7s.core.instance.state.InstanceOids.InstanceOids
import org.s7s.core.instance.state.InstanceOids.ProfileOid
import org.s7s.core.instance.state.st.STDocument
import org.s7s.core.instance.state.oid.Oid
import org.s7s.core.instance.state.STCmd
import javafx.geometry.Side
import javafx.scene.control.TableView
import javafx.scene.layout.Pane
import tornadofx.*

class MainView : View("Main") {

    val profiles = FxUtil.newObservable(Oid.of("/profile")) /*{
        val attr = it.attribute(ProfileOid.INSTANCE_TYPE)
        attr.isPresent() && attr.asInstanceType() == Metatypes.InstanceType.AGENT;
    }*/

    val hostGraph = pane {

    }

    val hostList = tableview(profiles) {

        columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        column<STDocument, String>("Hostname") {
            FxUtil.newProperty(it.value.attribute(AgentOid.HOSTNAME))
        }
        column<STDocument, String>("Instance Type") {
            FxUtil.newProperty(it.value.attribute(ProfileOid.INSTANCE_TYPE))
        }
        column<STDocument, Pane>("OS Type") {
            FxUtil.newProperty(it.value.attribute(AgentOid.OS_TYPE)) { value ->
                when (value) {
                    Platform.OsType.LINUX -> hbox {
                        imageview("image/platform/linux.png")
                        label("Linux")
                    }
                    Platform.OsType.WINDOWS -> hbox {
                        imageview("image/platform/windows_10.png")
                        label("Windows")
                    }
                    Platform.OsType.MACOS -> hbox {
                        imageview("image/platform/osx.png")
                        label("macOS")
                    }
                    else -> hbox {
                        label("Unknown")
                    }
                }
            }
        }
        column<STDocument, String>("Uptime") {
            FxUtil.newProperty(it.value.attribute(AgentOid.START_TIME))
        }
        column<STDocument, String>("Last Contact") {
            FxUtil.newProperty(it.value.attribute(AgentOid.CONTACT_TIME))
        }
        column<STDocument, String>("Status") {
            FxUtil.newProperty(it.value.attribute(ProfileOid.STATUS))
        }
        column<STDocument, String>("Agent Path") {
            FxUtil.newProperty(it.value.attribute(AgentOid.LOCATION))
        }

        val expander = rowExpander {
            paddingLeft = 6
            tabpane {
                tab("Metadata") {
                    vbox {
                        hbox {
                            button("Reboot")
                        }
                        form {
                            fieldset("Test") {
                                field("UUID") {
                                    label(it.attribute(ProfileOid.UUID).asString())
                                }
                                field ("Upload traffic") {
                                    label(FxUtil.newProperty<String>(it.attribute(AgentOid.CONTACT_TIME)))
                                }
                            }
                        }
                    }
                }
            }
        }
        expander.isVisible = false
        selectionModel.selectedItemProperty().onChange {
            expander.getExpandedProperty(it).set(true)
        }

        contextmenu {
            item("Control Panel").action {
                find<AgentManagerView>(mapOf(AgentManagerView::profile to selectedItem)).openWindow()
            }
        }
    }

    val carousel = CarouselPane(hostList, hostGraph).apply {
        directionProperty().set(Side.TOP)
    }

    val extend = ExtendPane(carousel).apply {

    }

    override val root = borderpane {
        if ("" == "") {
            center = extend
            center.viewOrder = 2.0
            left<SideMenuView>()
        } else {
            center = carousel
            top<RegularMenuView>()
        }
    }

    override fun onDock() {
        //STCmd.async().sync(InstanceOids().profile)
    }

    override fun onUndock() {
    }

    init {
        subscribe<MainViewChangeEvent> { event ->
            when (event.view) {
                "list" -> carousel.moveTo(0)
                "graph" -> carousel.moveTo(1)
                else -> {
                }
            }
        }

        subscribe<MainMenuOpenEvent> { event ->
            if (event.view != extend.regionLeftProperty().get()) {
                extend.regionLeftProperty().set(event.view)
            } else {
                extend.regionLeftProperty().set(null)
            }
        }
    }
}
