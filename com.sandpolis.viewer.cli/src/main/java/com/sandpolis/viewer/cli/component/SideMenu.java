package com.sandpolis.viewer.cli.component;

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
