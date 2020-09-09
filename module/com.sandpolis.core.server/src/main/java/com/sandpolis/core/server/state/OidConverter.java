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
package com.sandpolis.core.server.state;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.OidBase;
import com.sandpolis.core.instance.state.oid.STAttributeOid;
import com.sandpolis.core.instance.state.oid.STCollectionOid;
import com.sandpolis.core.instance.state.oid.STDocumentOid;

@Converter
public class OidConverter implements AttributeConverter<Oid, String> {

	@Override
	public String convertToDatabaseColumn(Oid value) {
		return value.toString();
	}

	@Override
	public Oid convertToEntityAttribute(String value) {
		switch (Integer.valueOf(value.replaceAll(".*\\.", "")) % 10) {
		case OidBase.SUFFIX_ATTRIBUTE:
			return new STAttributeOid<>(value);
		case OidBase.SUFFIX_COLLECTION:
			return new STCollectionOid<>(value);
		case OidBase.SUFFIX_DOCUMENT:
			return new STDocumentOid<>(value);
		default:
			throw new RuntimeException();
		}
	}
}
