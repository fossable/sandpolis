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
package com.sandpolis.server.vanilla.stream;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;

/**
 * A {@link Stream} is an ephemeral flow of events between two endpoints in the
 * network.
 *
 * @author cilki
 * @since 2.0.0
 */
@MappedSuperclass
public abstract class Stream {// implements ProtoType<ProtoStream> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The unique ID.
	 */
	@Column(nullable = false, unique = true)
	private int id;

	/**
	 * UUIDs of all upstream endpoints.
	 */
	@ElementCollection
	@CollectionTable(name = "UpstreamEndpoints", joinColumns = @JoinColumn(name = "db_id"))
	@Column(name = "upstream")
	private List<String> upstreamEndpoints;

	/**
	 * UUIDs of all downstream endpoints.
	 */
	@ElementCollection
	@CollectionTable(name = "DownstreamEndpoints", joinColumns = @JoinColumn(name = "db_id"))
	@Column(name = "downstream")
	private List<String> downstreamEndpoints;

	/**
	 * Indicates whether the stream is currently able to be resumed.
	 *
	 * @return Whether the stream is resumable
	 */
	public boolean resumable() {
		return false;
	}

}
