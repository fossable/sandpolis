package com.sandpolis.viewer.cli.view.main.hosts;

import com.googlecode.lanterna.gui2.table.Table;

public class HostList extends Table<String> {
	public HostList() {
		super("Hostname", "IP Address");
		init();
	}

	private void init() {

	}

}
