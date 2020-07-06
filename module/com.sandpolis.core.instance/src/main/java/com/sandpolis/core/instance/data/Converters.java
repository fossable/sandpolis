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
package com.sandpolis.core.instance.data;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.sandpolis.core.foundation.util.CertUtil;

/**
 * This class contains a set of database converters for use in JPA entities.
 *
 * @author cilki
 * @since 6.2.0
 */
public final class Converters {

	private Converters() {
	}

	@Converter
	public static class Certificate implements AttributeConverter<X509Certificate, java.lang.String> {
		@Override
		public java.lang.String convertToDatabaseColumn(X509Certificate cert) {
			try {
				return Base64.getEncoder().encodeToString(cert.getEncoded());
			} catch (CertificateEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public X509Certificate convertToEntityAttribute(java.lang.String dbData) {
			try {
				return CertUtil.parseCert(dbData);
			} catch (CertificateException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
