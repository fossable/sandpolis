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
package com.sandpolis.server.vanilla.store.user;

import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_EMAIL;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.instance.Result.ErrorCode.OK;
import static com.sandpolis.core.util.ValidationUtil.email;
import static com.sandpolis.core.util.ValidationUtil.username;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.Result.ErrorCode;
import com.sandpolis.core.instance.User.ProtoUser;
import com.sandpolis.server.vanilla.store.group.Group;

/**
 * Represents a user account on the server.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class User implements ProtoType<ProtoUser> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The unique ID.
	 */
	@Column(nullable = false, unique = true)
	private long id;

	/**
	 * The user's unique username.
	 */
	@Column(nullable = false, length = 30, unique = true)
	private String username;

	/**
	 * The user's optional email address.
	 */
	@Column
	private String email;

	/**
	 * The user's password hash.
	 */
	@Column(nullable = false, length = 120)
	private String hash;

	/**
	 * The user creation timestamp.
	 */
	@Column(nullable = false)
	private long creation;

	/**
	 * An optional expiration timestamp.
	 */
	@Column
	private long expiration;

	@ManyToMany(mappedBy = "members")
	private List<Group> groups;

	@Transient
	private int cvid;

	// JPA Constructor
	User() {
	}

	/**
	 * Construct a new {@link User} from a configuration.
	 *
	 * @param config The configuration which should be prevalidated and complete
	 */
	public User(ProtoUser config) {
		if (User.valid(config) != ErrorCode.OK)
			throw new IllegalArgumentException();

		merge(config);
	}

	public long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public User setUsername(String username) {
		this.username = username;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public User setEmail(String email) {
		this.email = email;
		return this;
	}

	public String getHash() {
		return hash;
	}

	public User setHash(String hash) {
		this.hash = hash;
		return this;
	}

	public long getCreation() {
		return creation;
	}

	public User setCreation(long creation) {
		this.creation = creation;
		return this;
	}

	public long getExpiration() {
		return expiration;
	}

	public User setExpiration(long expiration) {
		this.expiration = expiration;
		return this;
	}

	public int getCvid() {
		return cvid;
	}

	public User setCvid(int cvid) {
		this.cvid = cvid;
		return this;
	}

	@Override
	public void merge(ProtoUser config) {

		if (config.hasId())
			this.id = config.getId();
		if (config.hasUsername())
			setUsername(config.getUsername());
		if (config.hasEmail())
			setEmail(config.getEmail());
		if (config.hasExpiration())
			setExpiration(config.getExpiration());
		if (config.hasCtime())
			setCreation(config.getCtime());

	}

	@Override
	public ProtoUser serialize() {
		return ProtoUser.newBuilder().setUsername(username).setEmail(email).setExpiration(expiration).setCtime(creation)
				.build();
	}

	/**
	 * Validate a {@link ProtoUser}. A {@link ProtoType} is valid if all present
	 * fields pass value restrictions.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public static ErrorCode valid(ProtoUser config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (config.hasUsername() && !username(config.getUsername()))
			return INVALID_USERNAME;
		if (config.hasEmail() && !email(config.getEmail()))
			return INVALID_EMAIL;

		return OK;
	}

	/**
	 * Check a {@link ProtoUser} for completeness. A {@link ProtoType} is complete
	 * if all required fields are present.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public static ErrorCode complete(ProtoUser config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (!config.hasUsername())
			return INVALID_USERNAME;

		return OK;
	}
}
