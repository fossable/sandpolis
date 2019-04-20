/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.util;

import static com.sandpolis.core.util.CryptoUtil.MD5;
import static com.sandpolis.core.util.CryptoUtil.SHA1;
import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

public class CryptoUtilTest {

	@Test
	public void testHashStringArray() {
		assertEquals("8beb74403d6a82ff315227e68d88cea2", CryptoUtil.hash(MD5, "ee743085830995f96e4b430f6eb766bb"));
		assertEquals("8beb74403d6a82ff315227e68d88cea2", CryptoUtil.hash(MD5, "ee743085830995f96e4b430f6eb766bb"));
		assertEquals("8beb74403d6a82ff315227e68d88cea2", CryptoUtil.hash(MD5, "ee743085830995f96e4b430f6eb766bb"));
		assertEquals("5500219c29d1ef60e48ea0aa022f96507f0c8b69",
				CryptoUtil.hash(SHA1, "8beb74403d6a82ff315227e68d88cea2"));
		assertEquals("a0e27b2407cf08e88681aff3d1e188561abb58f3",
				CryptoUtil.hash(SHA1, "5500219c29d1ef60e48ea0aa022f96507f0c8b69"));
		assertEquals("86e3cc23b47fdf729347aa79a6296c8e836fcbe8",
				CryptoUtil.hash(SHA1, "a0e27b2407cf08e88681aff3d1e188561abb58f3"));
		assertEquals("dd7f816a80d355fac66d8d644c571e07eecf657c7fec5640ad37e79db7f95f9d",
				CryptoUtil.hash(SHA256, "ee743085830995f96e4b430f6eb766bb"));
		assertEquals("724a436ff8d5c293a29074b0b26855f9cd3e2e016efd0e1d9d1beaf6c09cdaf1",
				CryptoUtil.hash(SHA256, "dd7f816a80d355fac66d8d644c571e07eecf657c7fec5640ad37e79db7f95f9d"));
		assertEquals("232daaa5bb232a633886f525613fbeb80648e6b14503ff6987e91b237be8752e",
				CryptoUtil.hash(SHA256, "724a436ff8d5c293a29074b0b26855f9cd3e2e016efd0e1d9d1beaf6c09cdaf1"));
	}

	@Test
	public void testPasswordHashing() {
		assertTrue(CryptoUtil.PBKDF2.check("pa55w0rd",
				"2142:7D06806F24653DD5364C6BCFFEC86029:4cCVyYx+Re7OIE2fUcSpaV/OqpuV9/7XFae/xLEDSmZEILo6lDMV8IzaZFdcqfSR"));
		assertFalse(CryptoUtil.PBKDF2.check("pa55w0rd",
				"2142:7D06806F24653DD5364C6BCFFEC86029:UQWvabjl1dSWq21Edl+ME7lUb/L9KSKT90K2U6iPCtUGUbNiDnj5TdnGc6irJJgE"));

		assertTrue(CryptoUtil.PBKDF2.check("goodpass", CryptoUtil.PBKDF2.hash("goodpass")));
	}

	@Test
	public void testSandSigning() {
		KeyPair keypair = CryptoUtil.SAND5.generate();
		KeyPair keypair2 = CryptoUtil.SAND5.generate();
		byte[] nonce = CryptoUtil.SAND5.getNonce();

		ReciprocalKeyPair server = new ReciprocalKeyPair(keypair.getPrivate().getEncoded(),
				keypair2.getPublic().getEncoded());
		ReciprocalKeyPair client = new ReciprocalKeyPair(keypair2.getPrivate().getEncoded(),
				keypair.getPublic().getEncoded());

		byte[] signature = CryptoUtil.SAND5.sign(server, nonce);
		assertTrue(CryptoUtil.SAND5.check(client, nonce, signature));
	}

}
