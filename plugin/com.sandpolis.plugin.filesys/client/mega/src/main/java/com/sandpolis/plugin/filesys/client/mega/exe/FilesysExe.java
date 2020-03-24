//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.plugin.filesys.client.mega.exe;

import java.nio.file.Paths;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Result.Outcome;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.util.SystemUtil;
import com.sandpolis.plugin.filesys.FsHandle;
import com.sandpolis.plugin.filesys.MessageFilesys.FilesysMSG;
import com.sandpolis.plugin.filesys.MsgFilesys.RQ_FileDelete;
import com.sandpolis.plugin.filesys.MsgFilesys.RQ_FileListing;
import com.sandpolis.plugin.filesys.MsgFilesys.RS_FileListing;

public final class FilesysExe extends Exelet {

	@Auth
	@Handler(tag = FilesysMSG.RQ_FILE_LISTING_FIELD_NUMBER)
	public static MessageOrBuilder rq_file_listing(RQ_FileListing rq) throws Exception {
		String path;
		switch (SystemUtil.OS_TYPE) {
		case WINDOWS:
			path = rq.getPath().startsWith("/") ? rq.getPath().substring(1) : rq.getPath();
			if (path.equals("C:"))
				path = "/";
			break;
		default:
			path = rq.getPath();
		}

		try (FsHandle handle = new FsHandle(path, rq.getOptions())) {
			return RS_FileListing.newBuilder().addAllListing(handle.list());
		}
	}

	@Auth
	@Handler(tag = FilesysMSG.RQ_FILE_DELETE_FIELD_NUMBER)
	public static MessageOrBuilder rq_file_delete(RQ_FileDelete rq) throws Exception {
		switch (SystemUtil.OS_TYPE) {
		case WINDOWS:
			for (var path : rq.getTargetList()) {
				MoreFiles.deleteRecursively(Paths.get(path.startsWith("/") ? path.substring(1) : path),
						RecursiveDeleteOption.ALLOW_INSECURE);
			}
			break;
		default:
			for (var path : rq.getTargetList()) {
				MoreFiles.deleteRecursively(Paths.get(path), RecursiveDeleteOption.ALLOW_INSECURE);
			}
		}

		return Outcome.newBuilder().setResult(true);
	}

	private FilesysExe() {
	}
}
