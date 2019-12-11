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
package com.sandpolis.viewer.cli.view.main;

import java.util.Collections;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.sandpolis.viewer.cli.component.SideMenuPanel;
import com.sandpolis.viewer.cli.view.about.AboutPanel;

public class MainWindow extends BasicWindow {
	public MainWindow() {
		init();
	}

	private void init() {
		setHints(Collections.singleton(Window.Hint.EXPANDED));
		setTitle("Sandpolis");

		SideMenuPanel main = new SideMenuPanel();
		main.add("Hosts", new Panel(new BorderLayout()).addComponent(new Label("Not implemented yet...")));
		main.add("Users", new Panel(new GridLayout(1)).addComponent(new Label("Not implemented yet...")));
		main.add("Listeners", new Panel(new GridLayout(1)).addComponent(new Label("Not implemented yet...")));
		main.add("Settings", new Panel(new GridLayout(1)).addComponent(new Label("Not implemented yet...")));
		main.add("About", new AboutPanel());

		setComponent(main);

	}
}
