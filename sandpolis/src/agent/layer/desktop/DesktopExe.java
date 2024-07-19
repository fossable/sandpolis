//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.desktop.agent.java.exe;

import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLiteOrBuilder;
import com.google.protobuf.UnsafeByteOperations;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.instance.stream.OutboundStreamAdapter;
import org.s7s.plugin.desktop.agent.java.JavaDesktopSource;
import org.s7s.plugin.desktop.Messages.EV_DesktopStreamOutput;
import org.s7s.plugin.desktop.Messages.RQ_DesktopStream;
import org.s7s.plugin.desktop.Messages.RS_DesktopStream;
import org.s7s.plugin.desktop.Messages.RQ_CaptureScreenshot;
import org.s7s.plugin.desktop.Messages.RS_CaptureScreenshot;

public final class DesktopExe extends Exelet {

	@Handler(auth = true)
	public static RS_CaptureScreenshot rq_screenshot(RQ_CaptureScreenshot rq) throws Exception {

		try (var out = new ByteArrayOutputStream()) {
			BufferedImage screenshot = new Robot()
					.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(screenshot, "jpg", out);

			return RS_CaptureScreenshot.newBuilder().setData(UnsafeByteOperations.unsafeWrap(out.toByteArray()))
					.build();
		}
	}

	@Handler(auth = true)
	public static RS_DesktopStream rq_desktop_stream(ExeletContext context, RQ_DesktopStream rq) {

		var source = new JavaDesktopSource();
		var outbound = new OutboundStreamAdapter<EV_DesktopStreamOutput>(rq.getStreamId(), context.connector,
				context.request.getFrom());
		StreamStore.add(source, outbound);

		context.defer(() -> {
			source.start();
		});

		return RS_DesktopStream.DESKTOP_STREAM_OK;
	}

	private DesktopExe() {
	}
}
