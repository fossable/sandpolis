/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.plugin.desktop.exe;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.imageio.ImageIO;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.util.ProtoUtil;
import com.sandpolis.plugin.desktop.net.Desktop.RQ_Screenshot;
import com.sandpolis.plugin.desktop.net.Desktop.RS_Screenshot;
import com.sandpolis.plugin.desktop.net.MSG;

public class DesktopExe extends Exelet {

	@Override
	public String getPluginPrefix() {
		return "com.sandpolis.plugin.desktop";
	}

	@Override
	public void reply(com.sandpolis.core.proto.net.MSG.Message msg, MessageOrBuilder payload) {
		connector.send(ProtoUtil.rs(msg, ProtoUtil.setPluginPayload(MSG.DesktopMessage.newBuilder(), payload)));
	}

	@Override
	public Message extractPayload(com.sandpolis.core.proto.net.MSG.Message msg) {
		return ProtoUtil.getPayload(ProtoUtil.getPayload(msg));
	}

	@Auth
	@Handler(tag = MSG.DesktopMessage.RQ_SCREENSHOT_FIELD_NUMBER)
	public Message.Builder rq_screenshot(RQ_Screenshot rq) {
		var outcome = begin();
		try (var in = new PipedInputStream(); var out = new PipedOutputStream(in)) {
			BufferedImage screenshot = new Robot()
					.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(screenshot, "jpg", out);

			return RS_Screenshot.newBuilder().setData(ByteString.readFrom(in));
		} catch (Exception e) {
			return failure(outcome);
		}
	}
}
