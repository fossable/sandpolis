//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device;

import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;

public class Device extends AbstractSTDomainObject {

	Device(STDocument document) {
		super(document);
	}

	public String getId() {
		return "";
	}
}
