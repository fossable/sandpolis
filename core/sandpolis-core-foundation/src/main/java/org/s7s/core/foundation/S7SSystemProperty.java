//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record S7SSystemProperty(String name, Optional<String> value) {

	private static final Logger log = LoggerFactory.getLogger(S7SSystemProperty.class);

	public S7SSystemProperty(String name, Optional<String> value) {
		this.name = name;
		this.value = value;

		if (value.isPresent()) {
			log.trace("Loaded system property: {} -> \"{}\"", name, value.get());
		}
	}

	public static S7SSystemProperty of(String name) {
		return new S7SSystemProperty(name, Optional.ofNullable(System.getProperty(name)));
	}
}
