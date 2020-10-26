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
package com.sandpolis.client.ascetic.component;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.input.KeyStroke;

public class SideMenu extends ActionListBox {
	private SideMenuPanel parent;

	public SideMenu(SideMenuPanel parent) {
		this.parent = parent;
	}

	@Override
	public Result handleKeyStroke(KeyStroke key) {
		switch (key.getKeyType()) {
		case ArrowDown:
			parent.down();
			break;
		case ArrowUp:
			parent.up();
			break;
		case Enter:
			return Result.HANDLED;
		case PageDown:
			break;
		case PageUp:
			break;
		default:
			break;
		}
		return super.handleKeyStroke(key);
	}

}
