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

import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.proto.pojo.Stream.ProtoStream;

/**
 * A {@link Stream} is an ephemeral flow of events between two endpoints in the
 * network.
 *
 * @author cilki
 * @since 2.0.0
 */
@MappedSuperclass
public abstract class Stream implements ProtoType<ProtoStream> {

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
