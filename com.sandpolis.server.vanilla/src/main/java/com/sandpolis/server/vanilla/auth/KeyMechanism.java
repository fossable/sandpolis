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

import java.security.KeyPair;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;
import com.sandpolis.server.vanilla.store.group.Group;

/**
 * An {@link AuthenticationMechanism} that uses a SAND5 keypair for
 * authentication.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
@Table(name = "KeyMechanisms")
public class KeyMechanism extends AuthenticationMechanism {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The mechanism ID.
	 */
	@Column(nullable = false, unique = true)
	private long id;

	@ManyToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(referencedColumnName = "db_id")
	private Group group;

	@OneToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(referencedColumnName = "db_id")
	private ReciprocalKeyPair client;

	@OneToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(referencedColumnName = "db_id")
	private ReciprocalKeyPair server;

	/**
	 * Create a new {@link KeyMechanism} with the given attributes.
	 *
	 * @param group  The group associated with the mechanism
	 * @param client The client keypair
	 * @param server The server keypair
	 */
	public KeyMechanism(Group group, ReciprocalKeyPair client, ReciprocalKeyPair server) {
		if (group == null)
			throw new IllegalArgumentException();
		if (client == null)
			throw new IllegalArgumentException();
		if (server == null)
			throw new IllegalArgumentException();

		this.group = group;
		this.client = client;
		this.server = server;
	}

	KeyMechanism() {
	}

	/**
	 * Get the client-side keypair.
	 *
	 * @return The client's keypair
	 */
	public ReciprocalKeyPair getClient() {
		return client;
	}

	/**
	 * Get the server-side keypair.
	 *
	 * @return The server's keypair
	 */
	public ReciprocalKeyPair getServer() {
		return server;
	}

	/**
	 * Generate a new {@link KeyMechanism} for the given {@link Group}.
	 *
	 * @param group The {@link Group} to receive the new mechanism
	 * @return A new {@link KeyMechanism}
	 */
	public static KeyMechanism generate(Group group) {
		if (group == null)
			throw new IllegalArgumentException();

		KeyPair k1 = CryptoUtil.SAND5.generate();
		KeyPair k2 = CryptoUtil.SAND5.generate();

		return new KeyMechanism(group,
				// The client keypair (consists of k1's private key and k2's public key)
				new ReciprocalKeyPair(k1.getPrivate().getEncoded(), k2.getPublic().getEncoded()),
				// The server keypair (consists of k1's public key and k2's private key)
				new ReciprocalKeyPair(k2.getPrivate().getEncoded(), k1.getPublic().getEncoded()));
	}

}
