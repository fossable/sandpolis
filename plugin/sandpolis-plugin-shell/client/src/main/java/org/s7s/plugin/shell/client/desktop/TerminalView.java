//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.client.lifegem;

import java.io.Reader;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class TerminalView extends Pane {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private static class TerminalConfig {

		@JsonProperty("use-default-window-copy")
		private boolean useDefaultWindowCopy = true;

		@JsonProperty("clear-selection-after-copy")
		private boolean clearSelectionAfterCopy = true;

		@JsonProperty("copy-on-select")
		private boolean copyOnSelect = false;

		@JsonProperty("ctrl-c-copy")
		private boolean ctrlCCopy = true;

		@JsonProperty("ctrl-v-paste")
		private boolean ctrlVPaste = true;

		@JsonProperty("cursor-color")
		private String cursorColor = "black";

		@JsonProperty(value = "background-color")
		private String backgroundColor = "white";

		@JsonProperty("font-size")
		private int fontSize = 14;

		@JsonProperty(value = "foreground-color")
		private String foregroundColor = "black";

		@JsonProperty("cursor-blink")
		private boolean cursorBlink = false;

		@JsonProperty("scrollbar-visible")
		private boolean scrollbarVisible = true;

		@JsonProperty("enable-clipboard-notice")
		private boolean enableClipboardNotice = true;

		@JsonProperty("scroll-wheel-move-multiplier")
		private double scrollWhellMoveMultiplier = 0.1;

		@JsonProperty("font-family")
		private String fontFamily = "\"DejaVu Sans Mono\", \"Everson Mono\", FreeMono, \"Menlo\", \"Terminal\", monospace";

		@JsonProperty(value = "user-css")
		private String userCss = "data:text/plain;base64," + "eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0=";
	}

	private final WebView webView;
	private final ReadOnlyIntegerWrapper columnsProperty;
	private final ReadOnlyIntegerWrapper rowsProperty;
	private final ObjectProperty<Reader> inputReaderProperty;
	private final ObjectProperty<Reader> errorReaderProperty;
	private TerminalConfig terminalConfig = new TerminalConfig();
	protected final CountDownLatch countDownLatch = new CountDownLatch(1);

	public TerminalView() {
		webView = new WebView();
		columnsProperty = new ReadOnlyIntegerWrapper(150);
		rowsProperty = new ReadOnlyIntegerWrapper(10);
		inputReaderProperty = new SimpleObjectProperty<>();
		errorReaderProperty = new SimpleObjectProperty<>();

		inputReaderProperty.addListener((observable, oldValue, newValue) -> {
			new Thread(() -> {
				printReader(newValue);
			}).start();
		});

		errorReaderProperty.addListener((observable, oldValue, newValue) -> {
			new Thread(() -> {
				printReader(newValue);
			}).start();
		});

		webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			getWindow().setMember("app", this);
		});
		webView.prefHeightProperty().bind(heightProperty());
		webView.prefWidthProperty().bind(widthProperty());

		webEngine().load(TerminalView.class.getResource("/hterm.html").toExternalForm());
	}

//	@WebkitCall(from = "hterm")
	// WebKit Call
	public String getPrefs() {
		try {
			return new ObjectMapper().writeValueAsString(getTerminalConfig());
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void updatePrefs(TerminalConfig terminalConfig) {
		if (getTerminalConfig().equals(terminalConfig)) {
			return;
		}

		setTerminalConfig(terminalConfig);
		final String prefs = getPrefs();

		Platform.runLater(() -> {
			try {
				getWindow().call("updatePrefs", prefs);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
	}

//	@WebkitCall(from = "hterm")
	public void resizeTerminal(int columns, int rows) {
		columnsProperty.set(columns);
		rowsProperty.set(rows);
	}

//	@WebkitCall
	public void onTerminalInit() {
		Platform.runLater(() -> {
			getChildren().add(webView);
		});
	}

//	@WebkitCall
	/**
	 * Internal use only
	 */
	public void onTerminalReady() {
		new Thread(() -> {
			try {
				focusCursor();
				countDownLatch.countDown();
			} catch (final Exception e) {
			}
		}).start();
	}

	private void printReader(Reader bufferedReader) {
		try {
			int nRead;
			final char[] data = new char[1 * 1024];

			while ((nRead = bufferedReader.read(data, 0, data.length)) != -1) {
				final StringBuilder builder = new StringBuilder(nRead);
				builder.append(data, 0, nRead);
				print(builder.toString());
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

//	@WebkitCall(from = "hterm")
	public void copy(String text) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(text);
		clipboard.setContent(clipboardContent);
	}

	public void onTerminalFxReady(Runnable onReadyAction) {
		new Thread(() -> {
//			ThreadHelper.awaitLatch(countDownLatch);

			if (Objects.nonNull(onReadyAction)) {
				new Thread(onReadyAction).start();
			}
		}).start();
	}

	protected void print(String text) {
//		ThreadHelper.awaitLatch(countDownLatch);
//		ThreadHelper.runActionLater(() -> {
//			getTerminalIO().call("print", text);
//		});

	}

	public void focusCursor() {
		Platform.runLater(() -> {
			webView.requestFocus();
			getTerminal().call("focus");
		});
	}

	private JSObject getTerminal() {
		return (JSObject) webEngine().executeScript("t");
	}

	private JSObject getTerminalIO() {
		return (JSObject) webEngine().executeScript("t.io");
	}

	public JSObject getWindow() {
		return (JSObject) webEngine().executeScript("window");
	}

	private WebEngine webEngine() {
		return webView.getEngine();
	}

	public TerminalConfig getTerminalConfig() {
		if (Objects.isNull(terminalConfig)) {
			terminalConfig = new TerminalConfig();
		}
		return terminalConfig;
	}

	public void setTerminalConfig(TerminalConfig terminalConfig) {
		this.terminalConfig = terminalConfig;
	}

	public ReadOnlyIntegerProperty columnsProperty() {
		return columnsProperty.getReadOnlyProperty();
	}

	public int getColumns() {
		return columnsProperty.get();
	}

	public ReadOnlyIntegerProperty rowsProperty() {
		return rowsProperty.getReadOnlyProperty();
	}

	public int getRows() {
		return rowsProperty.get();
	}

	public ObjectProperty<Reader> inputReaderProperty() {
		return inputReaderProperty;
	}

	public Reader getInputReader() {
		return inputReaderProperty.get();
	}

	public void setInputReader(Reader reader) {
		inputReaderProperty.set(reader);
	}

	public ObjectProperty<Reader> errorReaderProperty() {
		return errorReaderProperty;
	}

	public Reader getErrorReader() {
		return errorReaderProperty.get();
	}

	public void setErrorReader(Reader reader) {
		errorReaderProperty.set(reader);
	}

}
