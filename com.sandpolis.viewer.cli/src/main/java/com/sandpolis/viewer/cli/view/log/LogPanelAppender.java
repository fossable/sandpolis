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
package com.sandpolis.viewer.cli.view.log;

import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.TextBox;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class LogPanelAppender extends AppenderBase<ILoggingEvent> {

	private TextBox textbox;

	public LogPanelAppender(TextBox textbox) {
		this.textbox = textbox;

		setContext((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		textbox.addLine(eventObject.getFormattedMessage());
	}

}
