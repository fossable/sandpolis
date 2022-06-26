//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.update.agent.java;

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.foundation.Result.Outcome;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.plugin.update.msg.MsgUpgrade.RQ_ManagerMetadata;
import org.s7s.plugin.update.msg.MsgUpgrade.RS_ManagerMetadata;

public final class UpdateExe extends Exelet {

	@Handler(auth = true)
	public static MessageLiteOrBuilder rq_manager_metadata(RQ_ManagerMetadata rq) throws Exception {
		if (PackageManager.INSTANCE == null)
			return Outcome.newBuilder().setResult(false);

		return RS_ManagerMetadata.newBuilder().setVersion(PackageManager.INSTANCE.getManagerVersion())
				.setLocation(PackageManager.INSTANCE.getManagerLocation().toString());
	}

	private UpdateExe() {
	}
}
