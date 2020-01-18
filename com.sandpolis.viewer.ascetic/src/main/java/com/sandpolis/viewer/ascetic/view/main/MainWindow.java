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
package com.sandpolis.viewer.ascetic.view.main;

import java.util.Collections;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.sandpolis.viewer.ascetic.view.about.AboutPanel;
import com.sandpolis.viewer.ascetic.view.main.hosts.HostList;
import com.sandpolis.viewer.ascetic.view.main.listeners.ListenerList;

public class MainWindow extends BasicWindow {

	private HostList clients;
	private ListenerList listeners;
	private AboutPanel about;

	private StatusBar status;

	private Panel content;

	public MainWindow() {
		setHints(Collections.singleton(Window.Hint.EXPANDED));

		clients = new HostList();
		listeners = new ListenerList();
		about = new AboutPanel();

		status = new StatusBar();

		{
			content = new Panel(new BorderLayout());
			content.addComponent(clients, BorderLayout.Location.CENTER);
		}

		{
			Panel root = new Panel(new BorderLayout());
			root.addComponent(content, BorderLayout.Location.CENTER);
			root.addComponent(status, BorderLayout.Location.BOTTOM);
			setComponent(root);
		}
	}

	@Override
	public boolean handleInput(KeyStroke key) {
		switch (key.getKeyType()) {
		case F1:
			content.removeAllComponents();
			content.addComponent(clients, BorderLayout.Location.CENTER);
			status.setSelected(0);
			break;
		case F2:
			content.removeAllComponents();
			content.addComponent(listeners, BorderLayout.Location.CENTER);
			status.setSelected(1);
			break;
		case F3:
			content.removeAllComponents();
			content.addComponent(about, BorderLayout.Location.CENTER);
			status.setSelected(2);
			break;
		default:
			return super.handleInput(key);
		}

		return true;
	}
}
