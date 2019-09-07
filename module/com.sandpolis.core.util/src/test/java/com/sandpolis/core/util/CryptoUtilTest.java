/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.util;

import static com.sandpolis.core.util.CryptoUtil.MD5;
import static com.sandpolis.core.util.CryptoUtil.SHA1;
import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.core.util.CryptoUtil.hash;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.io.BaseEncoding;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

class CryptoUtilTest {

	@Test
	@DisplayName("Check MD5 hashing")
	void hash_1() {
		assertEquals("8beb74403d6a82ff315227e68d88cea2", hash(MD5, "ee743085830995f96e4b430f6eb766bb"));

		// Collision
		byte[] c1 = BaseEncoding.base16().lowerCase().decode(
				"d131dd02c5e6eec4693d9a0698aff95c2fcab58712467eab4004583eb8fb7f8955ad340609f4b30283e488832571415a085125e8f7cdc99fd91dbdf280373c5bd8823e3156348f5bae6dacd436c919c6dd53e2b487da03fd02396306d248cda0e99f33420f577ee8ce54b67080a80d1ec69821bcb6a8839396f9652b6ff72a70");
		byte[] c2 = BaseEncoding.base16().lowerCase().decode(
				"d131dd02c5e6eec4693d9a0698aff95c2fcab50712467eab4004583eb8fb7f8955ad340609f4b30283e4888325f1415a085125e8f7cdc99fd91dbd7280373c5bd8823e3156348f5bae6dacd436c919c6dd53e23487da03fd02396306d248cda0e99f33420f577ee8ce54b67080280d1ec69821bcb6a8839396f965ab6ff72a70");

		assertArrayEquals(hash(MD5, c1), hash(MD5, c2));
		assertFalse(Arrays.equals(c1, c2));
	}

	@Test
	@DisplayName("Check SHA1 hashing")
	void hash_2() {
		assertEquals("7efd0fa234120db34ad5a65a7611c220440b5311",
				hash(SHA1, "86e3cc23b47fdf729347aa79a6296c8e836fcbe8"));
		assertEquals("a0e27b2407cf08e88681aff3d1e188561abb58f3",
				hash(SHA1, "5500219c29d1ef60e48ea0aa022f96507f0c8b69"));
		assertEquals("86e3cc23b47fdf729347aa79a6296c8e836fcbe8",
				hash(SHA1, "a0e27b2407cf08e88681aff3d1e188561abb58f3"));
	}

	@Test
	@DisplayName("Check SHA256 hashing")
	void hash_3() {
		assertEquals("a0265628e081e2fe7f6b41376a94a931067d12bad87f94d3191caa1bdf4f45b2",
				hash(SHA256, "232daaa5bb232a633886f525613fbeb80648e6b14503ff6987e91b237be8752e"));
		assertEquals("724a436ff8d5c293a29074b0b26855f9cd3e2e016efd0e1d9d1beaf6c09cdaf1",
				hash(SHA256, "dd7f816a80d355fac66d8d644c571e07eecf657c7fec5640ad37e79db7f95f9d"));
		assertEquals("232daaa5bb232a633886f525613fbeb80648e6b14503ff6987e91b237be8752e",
				hash(SHA256, "724a436ff8d5c293a29074b0b26855f9cd3e2e016efd0e1d9d1beaf6c09cdaf1"));
	}

	@Test
	@DisplayName("Check PBKDF2 verification")
	void pbkdf2_1() {
		assertTrue(CryptoUtil.PBKDF2.check("pa55w0rd",
				"2142:7D06806F24653DD5364C6BCFFEC86029:4cCVyYx+Re7OIE2fUcSpaV/OqpuV9/7XFae/xLEDSmZEILo6lDMV8IzaZFdcqfSR"));
		assertFalse(CryptoUtil.PBKDF2.check("pa55w0rd",
				"2142:7D06806F24653DD5364C6BCFFEC86029:UQWvabjl1dSWq21Edl+ME7lUb/L9KSKT90K2U6iPCtUGUbNiDnj5TdnGc6irJJgE"));

		assertTrue(CryptoUtil.PBKDF2.check("goodpass", CryptoUtil.PBKDF2.hash("goodpass")));
	}

	@Test
	@DisplayName("Check SAND5 verification")
	void sand5_1() {
		KeyPair pair1 = CryptoUtil.SAND5.generate();
		KeyPair pair2 = CryptoUtil.SAND5.generate();
		byte[] nonce = CryptoUtil.SAND5.getNonce();

		ReciprocalKeyPair server = new ReciprocalKeyPair(pair1.getPrivate().getEncoded(),
				pair2.getPublic().getEncoded());
		ReciprocalKeyPair client = new ReciprocalKeyPair(pair2.getPrivate().getEncoded(),
				pair1.getPublic().getEncoded());

		byte[] signature = CryptoUtil.SAND5.sign(server, nonce);
		assertTrue(CryptoUtil.SAND5.check(client, nonce, signature));
	}

}
