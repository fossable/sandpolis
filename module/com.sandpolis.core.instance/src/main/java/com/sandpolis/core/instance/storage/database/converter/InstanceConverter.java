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

import com.sandpolis.core.proto.util.Platform.Instance;

/**
 * This converter replaces {@link Instance}s with their numeric identifier.
 *
 * @author cilki
 * @since 5.0.0
 */
@Converter
public class InstanceConverter implements AttributeConverter<Instance, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Instance instance) {
		return instance.getNumber();
	}

	@Override
	public Instance convertToEntityAttribute(Integer dbData) {
		return Instance.forNumber(dbData);
	}

}
