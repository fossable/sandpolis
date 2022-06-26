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
import org.s7s.instance.client.desktop.ui.common.pane.CarouselPane
import org.s7s.core.foundation.Platform
import org.s7s.core.instance.plugin.PluginStore
import org.s7s.core.instance.state.InstanceOids.ProfileOid.AgentOid
import org.s7s.core.instance.state.st.STDocument
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.TreeItem
import javafx.scene.layout.Region
import tornadofx.*

class AgentManagerView : Fragment() {

    val profile: STDocument by param()

    private val model = object : ViewModel() {
        val extendBottom = bind { SimpleObjectProperty<Region>() }
    }

    val views = listOf(InventoryView(), BootagentView())

    val pluginViews = PluginStore.PluginStore.getHandles(AgentViewExtension::class.java).toList()

    val carousel = CarouselPane().apply {
        directionProperty().set(Side.TOP)

        views.forEach {
            add(it.name, it.root)
        }

        pluginViews.forEach {
            add(it.name, it.root)
        }
    }

    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 400.0

        left = titledpane(profile.attribute(AgentOid.HOSTNAME).asString()) {
            alignment = Pos.CENTER
            isCollapsible = false
            prefWidth = 150.0
            style(append = true) {
                padding = box(5.px)
            }
            vbox {
                when (profile.attribute(AgentOid.OS_TYPE).asOsType()) {
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
                hbox {
                    imageview("image/flag/US.png")
                    vbox {
                        label("127.0.0.1")
                        label("United States")
                    }
                }
                hbox {
                    imageview()
                    label("54 days")
                }
            }
            flowpane {
                hgap = 10.0
                vgap = 10.0
                alignment = Pos.CENTER

                button("P") {
                    tooltip("Power controls")
                }
                button("C") {
                    tooltip("Connection controls")
                }
                button("T") {
                    tooltip("")
                }
            }
            treeview<AgentViewExtension> {
                isShowRoot = false

                root = TreeItem(object : AgentViewExtension("Root") {
                    override val root = pane {}
                    override fun nowVisible(profile: STDocument) {}
                    override fun nowInvisible() {}
                })

                cellFormat {
                    text = it.name
                }

                populate { parent ->
                    if (parent == root) {
                        listOf(views, pluginViews).flatten()
                    } else {
                        null
                    }
                }

                onUserSelect {
                    views.forEach(AgentViewExtension::nowInvisible)
                    it.nowVisible(profile)
                    carousel.moveTo(it.name)
                }
            }
        }
        center = carousel
    }
}
