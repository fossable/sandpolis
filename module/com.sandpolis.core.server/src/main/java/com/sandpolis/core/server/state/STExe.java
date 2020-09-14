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
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.state.STStore.STStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.state.oid.STAttributeOid;
import com.sandpolis.core.instance.state.oid.STCollectionOid;
import com.sandpolis.core.instance.state.oid.STDocumentOid;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletContext;
import com.sandpolis.core.net.msg.MsgState.RQ_STSnapshot;
import com.sandpolis.core.net.msg.MsgState.RQ_STSync;
import com.sandpolis.core.net.state.EntangledCollection;
import com.sandpolis.core.net.state.EntangledDocument;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;

public final class STExe extends Exelet {

	@Handler(auth = false)
	public static MessageOrBuilder rq_st_snapshot(RQ_STSnapshot rq) {
		switch (rq.getOidType()) {
		case ATTRIBUTE:
			return STStore.root().get(new STAttributeOid<>(rq.getOid())).snapshot();
		case COLLECTION:
			return STStore.root().get(new STCollectionOid<>(rq.getOid())).snapshot();
		case DOCUMENT:
			return STStore.root().get(new STDocumentOid<>(rq.getOid())).snapshot();
		default:
			throw new RuntimeException();
		}
	}

	@Handler(auth = false)
	public static MessageOrBuilder rq_st_sync(ExeletContext context, RQ_STSync rq) {
		var outcome = begin();

		var config = new STSyncStruct();
		config.connection = context.connector;
		config.direction = rq.getDirection();
		config.streamId = rq.getStreamId();
		config.updatePeriod = rq.getUpdatePeriod();
		config.initiator = false;

		switch (rq.getOidType()) {
		case ATTRIBUTE:
			// TODO
			break;
		case COLLECTION:
			new EntangledCollection(STStore.root().get(new STCollectionOid<>(rq.getOid())), config);
			break;
		case DOCUMENT:
			new EntangledDocument(STStore.root().get(new STDocumentOid<>(rq.getOid())), config);
			break;
		default:
			return failure(outcome, "Unknown OID type");
		}

		return success(outcome);
	}
}
