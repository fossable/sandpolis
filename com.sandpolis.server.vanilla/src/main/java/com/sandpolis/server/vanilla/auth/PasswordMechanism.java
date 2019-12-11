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
package com.sandpolis.server.vanilla.auth;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.sandpolis.server.vanilla.store.group.Group;

/**
 * An {@link AuthenticationMechanism} that uses a simple textual password for
 * authentication.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class PasswordMechanism extends AuthenticationMechanism {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The mechanism ID.
	 */
	@Column(nullable = false)
	private long id;

	/**
	 * The parent {@link Group}.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(referencedColumnName = "db_id")
	private Group group;

	/**
	 * The mechanism password which is stored unhashed because reversibility is
	 * important.
	 */
	@Column(nullable = false)
	private String password;

	/**
	 * Create a new {@link PasswordMechanism}.
	 *
	 * @param parent   The parent group
	 * @param password The password
	 */
	public PasswordMechanism(Group parent, String password) {
		this.group = Objects.requireNonNull(parent);
		this.password = Objects.requireNonNull(password);
	}

	PasswordMechanism() {
	}

	/**
	 * Get the mechanism ID.
	 *
	 * @return The mechanism ID (mechID)
	 */
	public long getId() {
		return id;
	}

	/**
	 * Get the mechanism's password.
	 *
	 * @return The password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Get the mechanism's parent {@link Group}.
	 *
	 * @return The group
	 */
	public Group getGroup() {
		return group;
	}

}
