//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.desktop.client.lifegem.ui;

import org.s7s.core.instance.stream.StreamSink;
import org.s7s.core.instance.stream.StreamSource;
import org.s7s.plugin.desktop.Messages.EV_DesktopStreamInput;
import org.s7s.plugin.desktop.Messages.EV_DesktopStreamOutput;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class RemoteDesktopView extends ImageView {

	private SimpleDoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);

	private WritableImage image;

	private StreamSource<EV_DesktopStreamInput> source;

	private StreamSink<EV_DesktopStreamOutput> sink;

	public RemoteDesktopView() {
		zoomLevel.addListener(l -> {
			if (getImage() != null) {
				setFitHeight(getImage().getHeight() * zoomLevel.get());
			}
		});

		sink = new StreamSink<>() {

			@Override
			public void onNext(EV_DesktopStreamOutput ev) {

				if (ev.getPixelData().isEmpty()) {

					// Copy an entire block already visible
					var copy = new WritableImage(ev.getWidth(), ev.getHeight());
					copy.getPixelWriter().setPixels(0, 0, ev.getWidth(), ev.getHeight(), image.getPixelReader(),
							ev.getSourceX(), ev.getSourceY());
					image.getPixelWriter().setPixels(ev.getDestX(), ev.getDestY(), ev.getWidth(), ev.getHeight(),
							copy.getPixelReader(), 0, 0);
				} else {

//					image.getPixelWriter().setPixels(ev.getDestX(), ev.getDestY(), ev.getWidth(), ev.getHeight(),
//							pixelFormat.get(), ev.getPixelData().asReadOnlyByteBuffer(), rawRect.getScanlineStride());
				}
			}
		};

		addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			source.submit(EV_DesktopStreamInput.newBuilder().build());
		});

		addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			source.submit(EV_DesktopStreamInput.newBuilder().build());
		});

		setImage(image);
	}

}
