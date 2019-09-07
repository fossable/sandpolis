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
