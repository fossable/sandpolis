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

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.plugin.desktop.net.MCDesktop.RQ_Screenshot;
import com.sandpolis.plugin.desktop.net.MCDesktop.RS_Screenshot;
import com.sandpolis.plugin.desktop.net.MSG;

public final class DesktopExe extends Exelet {

	@Auth
	@Handler(tag = MSG.DesktopMessage.RQ_SCREENSHOT_FIELD_NUMBER)
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

	private DesktopExe() {
	}
}
