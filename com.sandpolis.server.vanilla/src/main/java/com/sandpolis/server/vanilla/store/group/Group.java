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
package com.sandpolis.server.vanilla.store.group;

import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.util.ConfigUtil;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Group.GroupStats;
import com.sandpolis.core.proto.pojo.Group.ProtoGroup;
import com.sandpolis.core.proto.util.Auth.KeyContainer;
import com.sandpolis.core.proto.util.Auth.KeyContainer.KeyPair;
import com.sandpolis.core.proto.util.Auth.PasswordContainer;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.server.vanilla.auth.AuthenticationMechanism;
import com.sandpolis.server.vanilla.auth.KeyMechanism;
import com.sandpolis.server.vanilla.auth.PasswordMechanism;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * A {@link Group} is a collection of users that share permissions on a
 * collection of clients. A group has one owner, who has complete control over
 * the group, and any number of members.<br>
 * <br>
 * Clients are always added to a group via an {@link AuthenticationMechanism}.
 * For example, if a group has a {@link PasswordMechanism} installed, clients
 * can supply the correct password during the authentication phase to be added
 * to the group.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class Group implements ProtoType<ProtoGroup> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The unique ID.
	 */
	@Column(nullable = false, unique = true)
	private String id;

	/**
	 * The group's name.
	 */
	@Column
	private String name;

	/**
	 * The group's owner.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(referencedColumnName = "db_id")
	private User owner;

	/**
	 * The group's members.
	 */
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "user_group", joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "db_id"), inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "db_id"))
	private Set<User> members;

	/**
	 * The group's creation timestamp.
	 */
	@Column(nullable = false)
	private long ctime;

	/**
	 * The group's last modification timestamp.
	 */
	@Column(nullable = false)
	private long mtime;

	/**
	 * The group's password authentication mechanisms.
	 */
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<PasswordMechanism> passwords;

	/**
	 * The group's key authentication mechanisms.
	 */
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<KeyMechanism> keys;

	// JPA Constructor
	Group() {
	}

	/**
	 * Construct a new {@link Group} from a configuration.
	 *
	 * @param config The configuration which should be prevalidated and complete
	 */
	public Group(GroupConfig config) {
		this.id = Objects.requireNonNull(config.getId());
		this.passwords = new HashSet<>();
		this.keys = new HashSet<>();

		if (merge(ProtoGroup.newBuilder().setConfig(config).build()) != ErrorCode.OK)
			throw new IllegalArgumentException();
	}

	public String getGroupId() {
		return id;
	}

	/**
	 * Install a new {@link PasswordMechanism} in the group.
	 *
	 * @param mechanism The new authentication mechanism
	 */
	public void addPasswordMechanism(PasswordMechanism mechanism) {
		passwords.add(mechanism);
	}

	/**
	 * Install a new {@link KeyMechanism} in the group.
	 *
	 * @param mechanism The new authentication mechanism
	 */
	public void addKeyMechanism(KeyMechanism mechanism) {
		keys.add(mechanism);
	}

	public KeyMechanism getKeyMechanism(long mechId) {
		return null;// TODO
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public long getCtime() {
		return ctime;
	}

	public void setCtime(long ctime) {
		this.ctime = ctime;
	}

	public long getMtime() {
		return mtime;
	}

	public void setMtime(long mtime) {
		this.mtime = mtime;
	}

	public Collection<User> getMembers() {
		return members;
	}

	public Collection<KeyMechanism> getKeys() {
		return keys;
	}

	public Collection<PasswordMechanism> getPasswords() {
		return passwords;
	}

	@Override
	public ErrorCode merge(ProtoGroup delta) {
		ErrorCode validity = ConfigUtil.valid(delta.getConfig());
		if (validity != ErrorCode.OK)
			return validity;

		if (delta.hasConfig()) {
			GroupConfig config = delta.getConfig();

			if (config.hasName())
				setName(config.getName());
			if (config.hasOwner())
				setOwner(UserStore.get(config.getOwner()).get());
			for (PasswordContainer password : config.getPasswordMechanismList()) {
				addPasswordMechanism(new PasswordMechanism(this, password.getPassword()));
			}
		}

		if (delta.hasStats()) {
			GroupStats stats = delta.getStats();

			if (stats.hasCtime())
				setCtime(stats.getCtime());
			if (stats.hasMtime())
				setMtime(stats.getMtime());
		}

		return ErrorCode.OK;
	}

	@Override
	public ProtoGroup extract() {
		GroupConfig.Builder config = GroupConfig.newBuilder().setId(id).setName(name).setOwner(owner.getUsername());

		for (User member : members) {
			config.addMember(member.getUsername());
		}
		for (PasswordMechanism passMech : passwords) {
			config.addPasswordMechanism(PasswordContainer.newBuilder().setPassword(passMech.getPassword()));
		}
		for (KeyMechanism keyMech : keys) {
			KeyPair.Builder client = KeyPair.newBuilder()
					.setSigner(ByteString.copyFrom(keyMech.getClient().getSigner()))
					.setVerifier(ByteString.copyFrom(keyMech.getClient().getVerifier()));
			KeyPair.Builder server = KeyPair.newBuilder()
					.setSigner(ByteString.copyFrom(keyMech.getServer().getSigner()))
					.setVerifier(ByteString.copyFrom(keyMech.getServer().getVerifier()));
			config.addKeyMechanism(KeyContainer.newBuilder().setClient(client).setServer(server));
		}
		GroupStats.Builder stats = GroupStats.newBuilder().setCtime(ctime).setMtime(mtime);
		return ProtoGroup.newBuilder().setConfig(config).setStats(stats).build();
	}

}
