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
package com.sandpolis.plugin.filesys.client.mega.exe;

import java.nio.file.Paths;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.protobuf.Message;
import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCTemp.RQ_FileDelete;
import com.sandpolis.core.proto.net.MCTemp.RQ_FileListing;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.plugin.filesys.FsHandle;
import com.sandpolis.plugin.filesys.net.MSG;

public class FilesysExe extends Exelet {

	@Auth
	@Handler(tag = MSG.FilesysMessage.RQ_FILE_LISTING_FIELD_NUMBER)
	public Message.Builder rq_file_listing(RQ_FileListing rq) throws Exception {
		String path;
		switch (PlatformUtil.queryOsType()) {
		case WINDOWS:
			path = rq.getPath().startsWith("/") ? rq.getPath().substring(1) : rq.getPath();
			if (path.equals("C:"))
				path = "/";
			break;
		default:
			path = rq.getPath();
		}

		try (FsHandle handle = new FsHandle(path)) {
			//return RS_FileListing.newBuilder().addAllListing(handle.list());
			return null;
		}
	}

	@Auth
	//@Handler(tag = MSG.FilesysMessage.RQ_FILE_DELETE_FIELD_NUMBER)
	public Message.Builder rq_file_delete(RQ_FileDelete rq) throws Exception {
		String path;
		switch (PlatformUtil.queryOsType()) {
		case WINDOWS:
			path = rq.getPath().startsWith("/") ? rq.getPath().substring(1) : rq.getPath();
			break;
		default:
			path = rq.getPath();
		}

		MoreFiles.deleteRecursively(Paths.get(path), RecursiveDeleteOption.ALLOW_INSECURE);
		return Outcome.newBuilder().setResult(true);
	}
}
