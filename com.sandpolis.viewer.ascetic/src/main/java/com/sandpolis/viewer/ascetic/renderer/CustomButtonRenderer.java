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
package com.sandpolis.viewer.ascetic.renderer;

import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Button.ButtonRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;

public class CustomButtonRenderer implements ButtonRenderer {
	@Override
	public TerminalPosition getCursorLocation(Button component) {
		return null;
	}

	@Override
	public TerminalSize getPreferredSize(Button component) {
		return new TerminalSize(TerminalTextUtils.getColumnWidth(component.getLabel()) + 5, 3);
	}

	@Override
	public void drawComponent(TextGUIGraphics graphics, Button button) {
		ThemeDefinition themeDefinition = button.getThemeDefinition();
		graphics.applyThemeStyle(themeDefinition.getNormal());
		TerminalSize size = graphics.getSize();
		graphics.drawLine(1, 0, size.getColumns() - 3, 0, Symbols.SINGLE_LINE_HORIZONTAL);
		graphics.drawLine(1, size.getRows() - 1, size.getColumns() - 3, size.getRows() - 1,
				Symbols.SINGLE_LINE_HORIZONTAL);
		graphics.drawLine(0, 1, 0, size.getRows() - 2, Symbols.SINGLE_LINE_VERTICAL);
		graphics.drawLine(size.getColumns() - 2, 1, size.getColumns() - 2, size.getRows() - 2,
				Symbols.SINGLE_LINE_VERTICAL);
		graphics.setCharacter(0, 0, Symbols.SINGLE_LINE_TOP_LEFT_CORNER);
		graphics.setCharacter(size.getColumns() - 2, 0, Symbols.SINGLE_LINE_TOP_RIGHT_CORNER);
		graphics.setCharacter(size.getColumns() - 2, size.getRows() - 1, Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER);
		graphics.setCharacter(0, size.getRows() - 1, Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER);

		// Fill the inner part of the box
		graphics.drawLine(1, 1, size.getColumns() - 3, 1, ' ');

		// Draw the text inside the button
		if (button.isFocused()) {
			graphics.applyThemeStyle(themeDefinition.getActive());
		}
		graphics.putString(2, 1, TerminalTextUtils.fitString(button.getLabel(), size.getColumns() - 5));
	}
}
