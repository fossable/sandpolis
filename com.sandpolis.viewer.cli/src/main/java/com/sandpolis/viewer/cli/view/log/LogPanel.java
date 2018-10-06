package com.sandpolis.viewer.cli.view.log;

import java.util.Collections;

import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;

import ch.qos.logback.classic.Logger;

public class LogPanel extends BasicWindow {

	private LogPanelAppender appender;
	private TextBox log;

	public LogPanel() {
		init();

		appender = new LogPanelAppender(log);

		// Redirect logging
		Logger logger = (Logger) LoggerFactory.getLogger("com.sandpolis");
		logger.addAppender(appender);

	}

	private void init() {
		setHints(Collections.singleton(Window.Hint.FIXED_SIZE));
		setSize(new TerminalSize(100, 10));

		log = new TextBox().setReadOnly(true);
		setComponent(log);
	}

	public LogPanelAppender getAppender() {
		return appender;
	}

}
