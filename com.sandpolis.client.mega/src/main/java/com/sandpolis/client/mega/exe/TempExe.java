package com.sandpolis.client.mega.exe;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.google.common.io.MoreFiles;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.sandpolis.client.mega.temp.FsHandle;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCTemp.RQ_Execute;
import com.sandpolis.core.proto.net.MCTemp.RQ_FileDelete;
import com.sandpolis.core.proto.net.MCTemp.RQ_FileListing;
import com.sandpolis.core.proto.net.MCTemp.RQ_Screenshot;
import com.sandpolis.core.proto.net.MCTemp.RS_Execute;
import com.sandpolis.core.proto.net.MCTemp.RS_FileListing;
import com.sandpolis.core.proto.net.MCTemp.RS_Screenshot;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.proto.net.MSG;

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
	@Handler(tag = MSG.Message.RQ_EXECUTE_FIELD_NUMBER)
	// Duplicated from ShellExe
	public Message.Builder rq_execute(RQ_Execute rq) throws Exception {
		Process p = Runtime.getRuntime().exec(rq.getCommand());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			int exit = p.waitFor();

			String line;
			StringBuffer buffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append("\n");
			}
			return RS_Execute.newBuilder().setResult(buffer.toString()).setExitCode(exit);
		}
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_FILE_LISTING_FIELD_NUMBER)
	// Duplicated from FilesysExe
	public Message.Builder rq_file_listing(RQ_FileListing rq) throws Exception {
		try (FsHandle handle = new FsHandle(rq.getPath(), rq.getOptions())) {
			return RS_FileListing.newBuilder().addAllListing(handle.list());
		}
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_FILE_DELETE_FIELD_NUMBER)
	// Duplicated from FilesysExe
	public Message.Builder rq_file_delete(RQ_FileDelete rq) throws Exception {
		MoreFiles.deleteRecursively(Paths.get(rq.getPath()));
		return Outcome.newBuilder().setResult(true);
	}
}
