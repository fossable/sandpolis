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
package com.sandpolis.core.server.state;

import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.state.STStore.STStore;

import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletContext;
import com.sandpolis.core.net.msg.MsgState.RQ_STSnapshot;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.state.st.entangled.EntangledDocument;

public final class STExe extends Exelet {

	@Handler(auth = false)
	public static MessageLiteOrBuilder rq_st_snapshot(RQ_STSnapshot rq) {
		var oid = new AbsoluteOid<STDocument>(0, rq.getOid());

		return STStore.root().document(oid).snapshot();
	}

	@Handler(auth = false)
	public static MessageLiteOrBuilder rq_st_sync(ExeletContext context, RQ_STSync rq) {
		var outcome = begin();

		var config = new STSyncStruct();
		config.connection = context.connector;
		config.direction = rq.getDirection();
		config.streamId = rq.getStreamId();
		config.updatePeriod = rq.getUpdatePeriod();
		config.initiator = false;

		var oid = new AbsoluteOid<STDocument>(0, rq.getOid());

		new EntangledDocument(STStore.root().document(oid), config);

		return success(outcome);
	}
}
