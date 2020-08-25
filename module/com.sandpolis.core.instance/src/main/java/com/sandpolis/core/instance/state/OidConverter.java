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
package com.sandpolis.core.instance.state;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class OidConverter implements AttributeConverter<Oid<?>, String> {
	@Override
	public String convertToDatabaseColumn(Oid<?> value) {
		return value.toString();
	}

	@Override
	public Oid<?> convertToEntityAttribute(String value) {
		return new Oid<>(value);
	}
}
