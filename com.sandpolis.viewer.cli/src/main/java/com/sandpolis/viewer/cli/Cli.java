package com.sandpolis.viewer.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.slpanels.SLAnimator;
import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.TextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.sandpolis.viewer.UI;
import com.sandpolis.viewer.Viewer;
import com.sandpolis.viewer.cli.view.main.MainWindow;

public class Cli implements UI {

	public static final Logger log = LoggerFactory.getLogger(Cli.class);

	public static Cli getCli() {
		return (Cli) Viewer.getUI();
	}

	private Screen screen;
	private WindowBasedTextGUI textGUI;

	@Override
	public void start() throws Exception {
		if (screen != null)
			throw new IllegalStateException();

		screen = new DefaultTerminalFactory().setForceTextTerminal(true).createScreen();
		screen.startScreen();

		textGUI = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
		((AsynchronousTextGUIThread) textGUI.getGUIThread()).start();

		// textGUI.addWindow(new LogPanel());
		MainWindow window = new MainWindow();
		textGUI.addWindow(window);
		textGUI.updateScreen();

		// Start the animator. If another Tween-able type is added in addition to
		// MovablePanel, this statement needs to move.
		SLAnimator.start(getCli().getThread(), window);

	}

	@Override
	public void stop() throws Exception {
		if (screen == null)
			throw new IllegalStateException();

		screen.stopScreen();
	}

	public TextGUIThread getThread() {
		return textGUI.getGUIThread();
	}

}
