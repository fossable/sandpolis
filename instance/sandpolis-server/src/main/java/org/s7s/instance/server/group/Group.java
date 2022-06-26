//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.group;

import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;

/**
 * A {@link Group} is a collection of users that share permissions on a
 * collection of clients. A group has one owner, who has complete control over
 * the group, and any number of members.
 *
 * <p>
 * Clients are always added to a group via an {@code AuthenticationMechanism}.
 * For example, if a group has a {@code PasswordMechanism} installed, clients
 * can supply the correct password during the authentication phase to be added
 * to the group.
 *
 * @since 5.0.0
 */
public class Group extends AbstractSTDomainObject {

	Group(STDocument parent) {
		super(parent);
	}

}
