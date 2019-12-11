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
package com.sandpolis.viewer.cli.view.about;

import static com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING;
import static com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER;

import java.util.Date;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.AbstractComponent;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.util.AsciiUtil;
import com.sandpolis.core.util.RandUtil;

public class AboutPanel extends Panel {

	public AboutPanel() {
		super(new GridLayout(2));
		init();
	}

	private void init() {

		Label art = new Label(AsciiUtil.getSandpolisArt());
		art.setLayoutData(GridLayout.createLayoutData(CENTER, BEGINNING, true, false, 2, 1));
		addComponent(art);

		Label lbl_version = new Label("Sandpolis Version").addStyle(SGR.BOLD);
		addComponent(lbl_version);

		Label val_version = new Label(Core.SO_BUILD.getVersion() + " (Build: " + Core.SO_BUILD.getNumber() + ")");
		addComponent(val_version);

		Label lbl_timestamp = new Label("Build Timestamp").addStyle(SGR.BOLD);
		addComponent(lbl_timestamp);

		Label val_timestamp = new Label(new Date(Core.SO_BUILD.getTime()).toString());
		addComponent(val_timestamp);

		Label lbl_platform = new Label("Build Platform").addStyle(SGR.BOLD);
		addComponent(lbl_platform);

		Label val_platform = new Label(Core.SO_BUILD.getPlatform());
		addComponent(val_platform);

		Label lbl_java_version = new Label("Java Version").addStyle(SGR.BOLD);
		addComponent(lbl_java_version);

		Label val_java_version = new Label(
				System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
		addComponent(val_java_version);

		Label lbl_description = new Label(
				"Wondering what a terminal UI like this one is doing in a modern software project like Sandpolis? I'm glad you asked. This super lightweight interface is for those situations where the full GUI isn't needed/wanted. Sometimes you can get things done faster if you don't need to touch the mouse. Admittedly, this UI is very niche, but also just as supported and secure as the regular GUI (since they both use the same engine under-the-hood).")
						.setLabelWidth(10);
		lbl_description.setLayoutData(GridLayout.createLayoutData(BEGINNING, BEGINNING, false, true, 2, 1));
		addComponent(lbl_description);

		Firework f1 = new Firework(0, 0, 20);
		addComponent(f1);

		new Thread(() -> {
			while (true) {
				f1.step();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	private class Firework extends AbstractComponent<Firework> {

		private char[][] map;
		private int x;
		private int y;

		private int xc;
		private int yc;

		private int size;

		private int step;

		public Firework(int x, int y, int size) {
			this.x = x;
			this.xc = size / 2;
			this.y = y;
			this.yc = size / 2;
			this.size = size;
			this.setPreferredSize(new TerminalSize(size, size));

			map = new char[size][size];
			for (int i = 0; i < size; i++)
				for (int j = 0; j < size; j++)
					map[i][j] = ' ';
			step = 0;
		}

		@Override
		protected ComponentRenderer<Firework> createDefaultRenderer() {
			return new ComponentRenderer<AboutPanel.Firework>() {

				@Override
				public TerminalSize getPreferredSize(Firework component) {
					return new TerminalSize(component.size, component.size);
				}

				@Override
				public void drawComponent(TextGUIGraphics graphics, Firework component) {
					for (int j = 0; j < component.size; j++)
						for (int i = 0; i < component.size; i++)
							graphics.setCharacter(x + i, y + j, component.map[j][i]);

				}
			};
		}

		public boolean step() {
			for (int j = 0; j < size; j++) {
				for (int i = 0; i < size; i++) {

					// Calculate distance to center
					int d = (int) Math.sqrt(Math.pow(xc - i, 2) + Math.pow(yc - j, 2));
					if (step == d)
						map[j][i] = RandUtil.nextAlphabetic(1).charAt(0);
					else if (step > d) {
						if (RandUtil.nextBoolean())
							map[j][i] = ' ';
					}
				}
			}
			step++;
			invalidate();
			return true;
			//
		}

	}

}
