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
package com.sandpolis.client.ascetic.view.about;

import static com.googlecode.lanterna.SGR.BOLD;
import static com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING;
import static com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER;

import java.util.Date;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.sandpolis.core.foundation.util.TextUtil;
import com.sandpolis.core.instance.Core;

public class AboutPanel extends Panel {

	public AboutPanel() {
		super(new GridLayout(2));
		init();
	}

	private void init() {

		Label art = new Label(TextUtil.getSandpolisArt());
		addComponent(art, GridLayout.createLayoutData(CENTER, BEGINNING, true, false, 2, 1));

		Label lbl_version = new Label("Sandpolis Version").addStyle(BOLD);
		addComponent(lbl_version);

		Label val_version = new Label(Core.SO_BUILD.getVersion());
		addComponent(val_version);

		Label lbl_timestamp = new Label("Build Timestamp").addStyle(BOLD);
		addComponent(lbl_timestamp);

		Label val_timestamp = new Label(new Date(Core.SO_BUILD.getTime()).toString());
		addComponent(val_timestamp);

		Label lbl_platform = new Label("Build Platform").addStyle(BOLD);
		addComponent(lbl_platform);

		Label val_platform = new Label(Core.SO_BUILD.getPlatform());
		addComponent(val_platform);

		Label lbl_java_version = new Label("Java Version").addStyle(BOLD);
		addComponent(lbl_java_version);

		Label val_java_version = new Label(
				System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
		addComponent(val_java_version);
	}

}
