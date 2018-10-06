package com.sandpolis.viewer.cli.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Panel;

public class SideMenuPanel extends Panel {

	public static final Logger log = LoggerFactory.getLogger(SideMenuPanel.class);

	public SideMenuPanel() {
		super(new BorderLayout());
		init();
	}

	private List<Panel> panels;

	private SideMenu menu;
	private Panel content;

	private int selection = 0;

	private void init() {

		panels = new ArrayList<>();

		content = new Panel(new BorderLayout());
		addComponent(content, BorderLayout.Location.CENTER);

		menu = new SideMenu(this);
		addComponent(menu.withBorder(Borders.singleLine()), BorderLayout.Location.LEFT);

	}

	private void select(int s) {
		if (s < 0 || s >= panels.size())
			return;

		selection = s;

		// Update content panel
		content.removeAllComponents();
		content.addComponent(panels.get(s), BorderLayout.Location.CENTER);
	}

	public void add(String label, Panel panel) {
		menu.addItem(label, null);

		panels.add(panel);

		if (content.getChildCount() == 0)
			select(0);
	}

	public void up() {
		select(selection - 1);
	}

	public void down() {
		select(selection + 1);
	}

}
