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

import com.sandpolis.core.util.Platform.InstanceFlavor;

/**
 * This converter replaces {@link InstanceFlavor}s with their numeric
 * identifier.
 *
 * @author cilki
 * @since 5.0.0
 */
@Converter
public class InstanceFlavorConverter implements AttributeConverter<InstanceFlavor, Integer> {

	@Override
	public Integer convertToDatabaseColumn(InstanceFlavor flavor) {
		return flavor.getNumber();
	}

	@Override
	public InstanceFlavor convertToEntityAttribute(Integer dbData) {
		return InstanceFlavor.forNumber(dbData);
	}

}
