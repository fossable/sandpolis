//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.state;

import static org.s7s.core.instance.state.STStore.STStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.protocol.Stream.RQ_STStream;
import org.s7s.core.protocol.Stream.RS_STStream;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.instance.state.st.entangled.EntangledDocument;

public final class STExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(STExe.class);

	@Handler(auth = false)
	public static RS_STStream rq_st_stream(ExeletContext context, RQ_STStream rq) {

		log.debug("Received sync request for OID: {}", rq.getOid());

		var document = new EntangledDocument(STStore.get(Oid.of(rq.getOid())), config -> {
			config.connection = context.connector;
			config.direction = rq.getDirection();
			config.streamId = rq.getStreamId();
			config.updatePeriod = rq.getUpdatePeriod();
			config.initiator = false;
			config.permanent = rq.getPermanent();
		});

		return RS_STStream.ST_STREAM_OK;
	}
}
