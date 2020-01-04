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
package com.sandpolis.viewer.ascetic.view.log;

import java.util.Collections;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;

public class LogPanel extends BasicWindow {

	private TextBox log;

	public LogPanel() {
		init();

		LogPanelAppender.instance.setOutput(log);
	}

	private void init() {
		setHints(Collections.singleton(Window.Hint.FIXED_SIZE));
		setSize(new TerminalSize(100, 10));

		log = new TextBox().setReadOnly(true);
		setComponent(log);
	}

}
