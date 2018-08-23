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
package com.sandpolis.server.auth;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.sandpolis.server.store.group.Group;

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
	 * @param password The password
	 */
	public PasswordMechanism(Group parent, String password) {
		this.group = Objects.requireNonNull(parent);
		this.password = Objects.requireNonNull(password);
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
