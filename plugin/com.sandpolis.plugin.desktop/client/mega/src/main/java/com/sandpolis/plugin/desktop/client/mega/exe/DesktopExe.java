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
package com.sandpolis.plugin.desktop.client.mega.exe;

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.stream.store.StreamStore.StreamStore;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.stream.store.OutboundStreamAdapter;
import com.sandpolis.plugin.desktop.client.mega.JavaDesktopSource;
import com.sandpolis.plugin.desktop.net.MessageDesktop.DesktopMSG;
import com.sandpolis.plugin.desktop.net.MsgDesktop.RQ_Screenshot;
import com.sandpolis.plugin.desktop.net.MsgDesktop.RS_Screenshot;
import com.sandpolis.plugin.desktop.net.MsgRd.EV_DesktopStream;
import com.sandpolis.plugin.desktop.net.MsgRd.RQ_DesktopStream;

public final class DesktopExe extends Exelet {

	@Auth
	@Handler(tag = DesktopMSG.RQ_SCREENSHOT_FIELD_NUMBER)
	public static MessageOrBuilder rq_screenshot(RQ_Screenshot rq) {
		var outcome = begin();

		try (var out = new ByteArrayOutputStream()) {
			BufferedImage screenshot = new Robot()
					.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(screenshot, "jpg", out);

			return RS_Screenshot.newBuilder().setData(ByteString.copyFrom(out.toByteArray()));
		} catch (Exception e) {
			return failure(outcome);
		}
	}

	@Auth
	@Handler(tag = DesktopMSG.RQ_DESKTOP_STREAM_FIELD_NUMBER)
	public static MessageOrBuilder rq_desktop_stream(ExeletContext context, RQ_DesktopStream rq) {
		var outcome = begin();
		// TODO use correct stream ID
		// Stream stream = new Stream();

		context.defer(() -> {
			JavaDesktopSource source = new JavaDesktopSource();
			source.addOutbound(new OutboundStreamAdapter<EV_DesktopStream>(rq.getId(), context.connector,
					context.request.getFrom(), ev -> {
						return Any.pack(DesktopMSG.newBuilder().setEvDesktopStream(ev).build(),
								"com.sandpolis.plugin.desktop");
					}));
			source.start();
		});

		return success(outcome);
	}

	@Auth
	@Handler(tag = DesktopMSG.EV_DESKTOP_STREAM_FIELD_NUMBER)
	public static void ev_desktop_stream(ExeletContext context, EV_DesktopStream ev) {
		StreamStore.streamData(context.request.getId(), ev);
	}

	private DesktopExe() {
	}
}
