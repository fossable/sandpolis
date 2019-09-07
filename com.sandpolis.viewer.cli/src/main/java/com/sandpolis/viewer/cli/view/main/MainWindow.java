/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
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
