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
package com.sandpolis.core.net.store.network;

import com.sandpolis.core.proto.net.MCNetwork.LinkType;

/**
 * This class contains information about a link between two arbitrary instances.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class SockLink {

	/**
	 * The link type.
	 */
	private LinkType type;

	public SockLink(LinkType type) {
		this.type = type;
	}

	/**
	 * Get the {@link LinkType}.
	 *
	 * @return The link type
	 */
	public LinkType getType() {
		return type;
	}
}
