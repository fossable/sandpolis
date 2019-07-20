package com.sandpolis.client.mega.exe;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCTemp.RQ_Execute;
import com.sandpolis.core.proto.net.MCTemp.RQ_NicTotals;
import com.sandpolis.core.proto.net.MCTemp.RQ_Screenshot;
import com.sandpolis.core.proto.net.MCTemp.RS_Execute;
import com.sandpolis.core.proto.net.MCTemp.RS_NicTotals;
import com.sandpolis.core.proto.net.MCTemp.RS_Screenshot;
import com.sandpolis.core.proto.net.MSG;

import oshi.SystemInfo;

// Temporary:
// Delete this class once plugins are working
public class TempExe extends Exelet {

	@Auth
	@Handler(tag = MSG.Message.RQ_SCREENSHOT_FIELD_NUMBER)
	// Duplicated from DesktopExe
	public Message.Builder rq_screenshot(RQ_Screenshot rq) {
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
	@Handler(tag = MSG.Message.RQ_NIC_TOTALS_FIELD_NUMBER)
	// Duplicated from SysinfoExe
	public Message.Builder rq_nic_totals(RQ_NicTotals rq) {

		long upload = 0;
		long download = 0;

		for (var nif : new SystemInfo().getHardware().getNetworkIFs()) {
			nif.updateNetworkStats();
			download += nif.getBytesRecv();
			upload += nif.getBytesSent();
		}

		return RS_NicTotals.newBuilder().setUpload(upload).setDownload(download);
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_EXECUTE_FIELD_NUMBER)
	// Duplicated from ShellExe
	public Message.Builder rq_execute(RQ_Execute rq) throws Exception {
		Process p = Runtime.getRuntime().exec(rq.getCommand());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			p.waitFor();

			String line;
			StringBuffer buffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append("\n");
			}
			return RS_Execute.newBuilder().setResult(buffer.toString());
		}
	}
}
