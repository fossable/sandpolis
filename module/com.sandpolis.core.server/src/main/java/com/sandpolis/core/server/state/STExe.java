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
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.State.ProtoDocument;
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
import com.sandpolis.core.net.stream.InboundStreamAdapter;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;

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
		config.direction = rq.getDirection();
		config.streamId = rq.getStreamId();
		config.updatePeriod = rq.getUpdatePeriod();

		switch (rq.getOidType()) {
		case ATTRIBUTE:
			// TODO
			break;
		case COLLECTION:
			var collection = new EntangledCollection(STStore.root().get(new STCollectionOid<>(rq.getOid())), config);
			switch (rq.getDirection()) {
			case BIDIRECTIONAL:
				StreamStore.add(collection.getSource(),
						new OutboundStreamAdapter<>(rq.getStreamId(), context.connector));
				StreamStore.add(new InboundStreamAdapter<>(rq.getStreamId(), context.connector, ProtoCollection.class),
						collection.getSink());
				break;
			case DOWNSTREAM:
				StreamStore.add(collection.getSource(),
						new OutboundStreamAdapter<>(rq.getStreamId(), context.connector));
				break;
			case UPSTREAM:
				StreamStore.add(new InboundStreamAdapter<>(rq.getStreamId(), context.connector, ProtoCollection.class),
						collection.getSink());
				break;
			default:
				return failure(outcome, "Unknown sync direction");
			}
			break;
		case DOCUMENT:
			var document = new EntangledDocument(STStore.root().get(new STDocumentOid<>(rq.getOid())), config);
			switch (rq.getDirection()) {
			case BIDIRECTIONAL:
				StreamStore.add(document.getSource(), new OutboundStreamAdapter<>(rq.getStreamId(), context.connector));
				StreamStore.add(new InboundStreamAdapter<>(rq.getStreamId(), context.connector, ProtoDocument.class),
						document.getSink());
				break;
			case DOWNSTREAM:
				StreamStore.add(document.getSource(), new OutboundStreamAdapter<>(rq.getStreamId(), context.connector));
				break;
			case UPSTREAM:
				StreamStore.add(new InboundStreamAdapter<>(rq.getStreamId(), context.connector, ProtoDocument.class),
						document.getSink());
				break;
			default:
				return failure(outcome, "Unknown sync direction");
			}
			break;
		default:
			return failure(outcome, "Unknown OID type");
		}

		return success(outcome);
	}
}
