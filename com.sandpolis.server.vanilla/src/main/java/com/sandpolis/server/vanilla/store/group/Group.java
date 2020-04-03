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
package com.sandpolis.server.vanilla.store.group;

import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_GROUPNAME;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_ID;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.instance.Result.ErrorCode.OK;
import static com.sandpolis.core.util.ValidationUtil.group;
import static com.sandpolis.core.util.ValidationUtil.username;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import java.util.Collection;
import java.util.HashSet;
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
import com.sandpolis.core.instance.Auth.KeyContainer;
import com.sandpolis.core.instance.Auth.KeyContainer.KeyPair;
import com.sandpolis.core.instance.Auth.PasswordContainer;
import com.sandpolis.core.instance.Group.ProtoGroup;
import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.Result.ErrorCode;
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
	public Group(ProtoGroup config) {
		if (Group.valid(config) != ErrorCode.OK)
			throw new IllegalArgumentException();

		this.passwords = new HashSet<>();
		this.keys = new HashSet<>();

		merge(config);
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
	public void merge(ProtoGroup config) {

		if (config.hasId())
			this.id = config.getId();
		if (config.hasName())
			setName(config.getName());
		if (config.hasOwner())
			setOwner(UserStore.get(config.getOwner()).get());
		for (PasswordContainer password : config.getPasswordMechanismList()) {
			addPasswordMechanism(new PasswordMechanism(this, password.getPassword()));
		}

		if (config.hasCtime())
			setCtime(config.getCtime());
		if (config.hasMtime())
			setMtime(config.getMtime());
	}

	@Override
	public ProtoGroup serialize() {
		ProtoGroup.Builder config = ProtoGroup.newBuilder().setId(id).setName(name).setOwner(owner.getUsername())
				.setCtime(ctime).setMtime(mtime);

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

		return config.build();
	}

	/**
	 * Validate a {@link ProtoGroup}. A {@link ProtoType} is valid if all present
	 * fields pass value restrictions.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public static ErrorCode valid(ProtoGroup config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (config.hasName() && !group(config.getName()))
			return INVALID_GROUPNAME;
		if (config.hasOwner() && !username(config.getOwner()))
			return INVALID_USERNAME;
		for (String member : config.getMemberList())
			if (!username(member))
				return INVALID_USERNAME;

		return OK;
	}

	/**
	 * Check a {@link ProtoGroup} for completeness. A {@link ProtoType} is complete
	 * if all required fields are present.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public static ErrorCode complete(ProtoGroup config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (!config.hasId())
			return INVALID_ID;
		if (!config.hasOwner())
			return INVALID_USERNAME;

		return OK;
	}
}
