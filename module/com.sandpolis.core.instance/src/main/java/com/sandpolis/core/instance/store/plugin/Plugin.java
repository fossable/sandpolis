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
package com.sandpolis.core.instance.store.plugin;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.google.protobuf.ByteString;
import com.sandpolis.core.proto.net.MCPlugin.PluginDescriptor;

/**
 * Represents an installed Sandpolis plugin.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
public final class Plugin {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The plugin's package identifier.
	 */
	@Column(nullable = false, unique = true)
	private String id;

	/**
	 * The plugin's Maven group and artifact name.
	 */
	@Column(nullable = false, unique = true)
	private String coordinate;

	/**
	 * The plugin's user-friendly name.
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * The plugin's version string.
	 */
	@Column(nullable = false)
	private String version;

	/**
	 * The plugin's textual description.
	 */
	@Column(nullable = true)
	private String description;

	/**
	 * The plugin's icon.
	 */
	@Column(nullable = true)
	private byte[] icon;

	/**
	 * Whether the plugin is enabled.
	 */
	@Column(nullable = false)
	private boolean enabled;

	/**
	 * The plugin artifact's hash.
	 */
	@Column(nullable = false)
	private byte[] hash;

	/**
	 * The size in bytes of the mega component.
	 */
	@Column(nullable = true)
	private int component_size_mega;

	/**
	 * The size in bytes of the micro component.
	 */
	@Column(nullable = true)
	private int component_size_micro;

	/**
	 * The size in bytes of the jfx component.
	 */
	@Column(nullable = true)
	private int component_size_jfx;

	/**
	 * The size in bytes of the cli component.
	 */
	@Column(nullable = true)
	private int component_size_cli;

	public Plugin(String id, String coordinate, String name, String version, String description, boolean enabled,
			byte[] hash) {
		this.id = Objects.requireNonNull(id);
		this.coordinate = Objects.requireNonNull(coordinate);
		this.name = Objects.requireNonNull(name);
		this.version = Objects.requireNonNull(version);
		this.hash = Objects.requireNonNull(hash);
		this.description = description;
		this.enabled = enabled;
	}

	Plugin() {
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public byte[] getIcon() {
		return icon;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public byte[] getHash() {
		return hash;
	}

	/**
	 * Build a {@link PluginDescriptor} from the object.
	 *
	 * @return A new {@link PluginDescriptor}
	 */
	public PluginDescriptor toDescriptor() {
		var plugin = PluginDescriptor.newBuilder().setId(getId()).setCoordinate(coordinate).setName(getName())
				.setVersion(getVersion()).setEnabled(isEnabled());

		if (icon != null)
			plugin.setIcon(ByteString.copyFrom(getIcon()));
		if (description != null)
			plugin.setDescription(getDescription());
		return plugin.build();
	}

}
