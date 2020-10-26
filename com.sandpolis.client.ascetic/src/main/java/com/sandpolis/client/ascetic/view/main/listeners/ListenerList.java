//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.client.ascetic.view.main.listeners;

import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.table.Table;

public class ListenerList extends Panel {

	private Table<String> listeners;

	public ListenerList() {
		super(new BorderLayout());
		{
			listeners = new Table<>("ID", "Name", "Owner", "Port");

			addComponent(listeners, BorderLayout.Location.CENTER);
		}

		{
			Panel controls = new Panel(new LinearLayout());
			controls.addComponent(new Label("Add Listener"));

			addComponent(controls, BorderLayout.Location.BOTTOM);
		}
	}
}
