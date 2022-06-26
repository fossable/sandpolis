//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.filesystem.agent.java.exe;

import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import java.nio.file.Paths;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.s7s.core.foundation.S7SSystem;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.instance.stream.OutboundStreamAdapter;
import org.s7s.plugin.filesystem.DirectoryStreamSource;
import org.s7s.plugin.filesystem.Messages.RQ_DirectoryStream;
import org.s7s.plugin.filesystem.Messages.RS_DirectoryStream;
import org.s7s.plugin.filesystem.Messages.EV_DirectoryStream;
import org.s7s.plugin.filesystem.Messages.RQ_DeleteFile;
import org.s7s.plugin.filesystem.Messages.RS_DeleteFile;

public final class FilesystemExe extends Exelet {

	@Handler(auth = true)
	public static RS_DirectoryStream rq_directory_stream(ExeletContext context, RQ_DirectoryStream rq)
			throws Exception {
		String path;
		switch (S7SSystem.OS_TYPE) {
		case WINDOWS:
			path = rq.getPath().startsWith("/") ? rq.getPath().substring(1) : rq.getPath();
			if (path.equals("C:"))
				path = "/";
			break;
		default:
			path = rq.getPath();
		}

		var source = new DirectoryStreamSource(rq);
		var outbound = new OutboundStreamAdapter<EV_DirectoryStream>(rq.getStreamId(), context.connector,
				context.request.getFrom());
		StreamStore.add(source, outbound);

		context.defer(() -> {
			source.start();
		});

		return RS_DirectoryStream.DIRECTORY_STREAM_OK;
	}

	@Handler(auth = true)
	public static RS_DeleteFile rq_delete_file(RQ_DeleteFile rq) throws Exception {
		switch (S7SSystem.OS_TYPE) {
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

		return RS_DeleteFile.DELETE_FILE_OK;
	}

	private FilesystemExe() {
	}
}
