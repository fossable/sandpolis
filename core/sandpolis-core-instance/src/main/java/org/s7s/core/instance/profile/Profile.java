//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.profile;

import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;

/**
 * A {@link Profile} is a generic container that stores data for an instance.
 * Most of the data are stored in a tree structure similar to a document store.
 *
 * @since 4.0.0
 */
public class Profile extends AbstractSTDomainObject {

	public Profile(STDocument parent) {
		super(parent);
	}
}
