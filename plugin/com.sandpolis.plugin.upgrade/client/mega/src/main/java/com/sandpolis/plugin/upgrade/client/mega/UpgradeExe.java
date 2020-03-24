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
package com.sandpolis.plugin.upgrade.client.mega;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Result.Outcome;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.plugin.upgrade.MessageUpgrade.UpgradeMSG;
import com.sandpolis.plugin.upgrade.MsgUpgrade.RQ_ManagerMetadata;
import com.sandpolis.plugin.upgrade.MsgUpgrade.RS_ManagerMetadata;

public final class UpgradeExe extends Exelet {

	@Auth
	@Handler(tag = UpgradeMSG.RQ_MANAGER_METADATA_FIELD_NUMBER)
	public static MessageOrBuilder rq_manager_metadata(RQ_ManagerMetadata rq) throws Exception {
		if (PackageManager.INSTANCE == null)
			return Outcome.newBuilder().setResult(false);

		return RS_ManagerMetadata.newBuilder().setVersion(PackageManager.INSTANCE.getManagerVersion())
				.setLocation(PackageManager.INSTANCE.getManagerLocation().toString());
	}

	private UpgradeExe() {
	}
}
