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
package com.sandpolis.core.instance.storage.database.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.sandpolis.core.util.Platform.OsType;

/**
 * This converter replaces {@link OsType}s with their numeric identifier.
 *
 * @author cilki
 * @since 5.0.0
 */
@Converter
public class OsTypeConverter implements AttributeConverter<OsType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(OsType type) {
		return type.getNumber();
	}

	@Override
	public OsType convertToEntityAttribute(Integer dbData) {
		return OsType.forNumber(dbData);
	}

}
