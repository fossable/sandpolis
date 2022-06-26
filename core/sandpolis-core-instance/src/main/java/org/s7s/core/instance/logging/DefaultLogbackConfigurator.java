//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.logging;

import java.util.Arrays;

import org.s7s.core.instance.InstanceContext;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

public class DefaultLogbackConfigurator extends BasicConfigurator {

	/**
	 * The format string for plain output.
	 */
	protected static final String PLAIN = "%date{yyyy-MM-dd HH:mm:ss} [%-5level][%logger{0}] %msg%n";

	/**
	 * The format string for colorful output.
	 */
	protected static final String COLORFUL = "%gray(%date{yyyy-MM-dd HH:mm:ss}) %highlight([%-5level])[%logger{0}] %msg%n";

	@Override
	public void configure(LoggerContext lc) {

		var appender = new ConsoleAppender<ILoggingEvent>();
		appender.setContext(lc);
		appender.setName("Console");

		var encoder = new LayoutWrappingEncoder<ILoggingEvent>();
		encoder.setContext(lc);

		var layout = new PatternLayout();
		layout.setPattern(System.console() == null ? PLAIN : COLORFUL);
		layout.setContext(lc);
		layout.start();

		encoder.setLayout(layout);

		appender.setEncoder(encoder);
		appender.start();

		Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
		root.addAppender(appender);

		configureLevels(lc);
	}

	protected void configureLevels(LoggerContext lc) {
		for (var line : InstanceContext.LOG_LEVELS.get()) {
			var components = line.split("=");
			if (components.length == 2) {
				lc.getLogger(components[0]).setLevel(Level.toLevel(components[1]));
			}
		}
	}
}
