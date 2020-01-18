package com.sandpolis.viewer.ascetic.view.main.listeners;

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
