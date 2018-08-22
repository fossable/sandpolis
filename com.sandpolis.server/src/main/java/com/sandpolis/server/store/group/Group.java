/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.store.group;

import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Group.GroupStats;
import com.sandpolis.core.proto.pojo.Group.ProtoGroup;
import com.sandpolis.core.proto.util.Auth.KeyContainer;
import com.sandpolis.core.proto.util.Auth.KeyContainer.KeyPair;
import com.sandpolis.core.proto.util.Auth.PasswordContainer;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.auth.AuthenticationMechanism;
import com.sandpolis.server.auth.KeyMechanism;
import com.sandpolis.server.auth.PasswordMechanism;
import com.sandpolis.server.store.user.User;

/**
 * A {@link Group} is a collection of users that share permissions on a
 * collections of clients. A group has one owner, who has complete control over
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
@Table(name = "Groups")
public class Group implements ProtoType<ProtoGroup> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The group's unique ID.
	 */
	@Column(nullable = false, unique = true)
	private long groupId;

	/**
	 * The group's name.
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * The group's owner.
	 */
	@ManyToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(referencedColumnName = "username")
	private User owner;

	/**
	 * The group's members.
	 */
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "user_group", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	private List<User> members;

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
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
	private Collection<PasswordMechanism> passwords;

	/**
	 * The group's key authentication mechanisms.
	 */
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
	private Collection<KeyMechanism> keys;

	// JPA Constructor
	Group() {
	}

	/**
	 * Construct a new {@link Group} from a configuration.
	 * 
	 * @param config The configuration which should be prevalidated and complete
	 */
	public Group(GroupConfig config) {
		if (merge(ProtoGroup.newBuilder().setConfig(config).build()) != ErrorCode.NONE)
			throw new IllegalArgumentException();
	}

	public long getGroupId() {
		return groupId;
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

	public List<User> getMembers() {
		return members;
	}

	@Override
	public ErrorCode merge(ProtoGroup delta) {
		ErrorCode validity = ValidationUtil.validConfig(delta.getConfig());
		if (validity != ErrorCode.NONE)
			return validity;

		if (delta.hasConfig()) {
			if (delta.getConfig().hasName())
				setName(delta.getConfig().getName());
			if (delta.getConfig().hasOwner())
				;// TODO
		}

		if (delta.hasStats()) {
			if (delta.getStats().hasCtime())
				setCtime(delta.getStats().getCtime());
			if (delta.getStats().hasMtime())
				setMtime(delta.getStats().getMtime());
		}

		return ErrorCode.NONE;
	}

	@Override
	public ProtoGroup extract() {
		GroupConfig.Builder config = GroupConfig.newBuilder().setId(groupId).setName(name)
				.setOwner(owner.getUsername());

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
