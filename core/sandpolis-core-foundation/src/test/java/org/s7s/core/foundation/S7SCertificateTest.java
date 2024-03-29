//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7SCertificateTest {

	/**
	 * An old base-64 certificate for testing purposes.
	 */
	static final String testCert = "Q2VydGlmaWNhdGU6CiAgICBEYXRhOgogICAgICAgIFZlcnNpb246IDMgKDB4MikKICAgICAgICBTZXJpYWwgTnVtYmVyOiAxICgweDEpCiAgICBTaWduYXR1cmUgQWxnb3JpdGhtOiBzaGExV2l0aFJTQUVuY3J5cHRpb24KICAgICAgICBJc3N1ZXI6IEM9VVMsIFNUPVRleGFzLCBMPUF1c3RpbiwgTz1TdWJ0ZXJyYW5lYW4gU2VjdXJpdHksIENOPVN1YnRlcnJhbmVhbiBTZWN1cml0eSBSb290IENBL2VtYWlsQWRkcmVzcz1hZG1pbkBzdWJ0ZXJyYW5lYW4tc2VjdXJpdHkuY29tCiAgICAgICAgVmFsaWRpdHkKICAgICAgICAgICAgTm90IEJlZm9yZTogTWF5IDI3IDA1OjAxOjIxIDIwMTcgR01UCiAgICAgICAgICAgIE5vdCBBZnRlciA6IE1heSAyNyAwNTowMToyMSAyMDE4IEdNVAogICAgICAgIFN1YmplY3Q6IEM9VVMsIFNUPVRleGFzLCBPPVN1YnRlcnJhbmVhbiBTZWN1cml0eSwgT1U9U3VidGVycmFuZWFuIFNlY3VyaXR5LCBDTj1kZWJ1Zy9lbWFpbEFkZHJlc3M9YWRtaW5Ac3VidGVycmFuZWFuLXNlY3VyaXR5LmNvbQogICAgICAgIFN1YmplY3QgUHVibGljIEtleSBJbmZvOgogICAgICAgICAgICBQdWJsaWMgS2V5IEFsZ29yaXRobTogcnNhRW5jcnlwdGlvbgogICAgICAgICAgICAgICAgUHVibGljLUtleTogKDIwNDggYml0KQogICAgICAgICAgICAgICAgTW9kdWx1czoKICAgICAgICAgICAgICAgICAgICAwMDphZjo4OTo5ZDozYzpjNjo3YTo1Mzo5ZjoxYjo1ZDpmODo2MzoyNTo3MjoKICAgICAgICAgICAgICAgICAgICA3MzowOTpkYjoyYzo2Nzo2MTpmMjpkODpiYTowZTpiMjplNTpkMjpiNTozNToKICAgICAgICAgICAgICAgICAgICBmYjpkMTpkYzphYjo5Yjo2YjozMzo0NTpiMzo2NTpiYjoyZTo0NzpjMTphNjoKICAgICAgICAgICAgICAgICAgICAyMTo1Nzo4Mjo5Njo3ZDozMjo3YjoxYzphMTo5MTo5YjphZDpjYjoxZDoyZDoKICAgICAgICAgICAgICAgICAgICA5OTplMToxZTo4NDowNzowMzoyYToyMzozODo1Nzo2OTowOTphYzoyYzpkNjoKICAgICAgICAgICAgICAgICAgICA0Yjo4Yzo1YTo2ZToyNTo2NDpmYjo0NDpjOTo1MToyZjo4MToyZjo5ZTo3MToKICAgICAgICAgICAgICAgICAgICBmNDpkNjozMDo1Yzo0NToxYjpjYzo4NTo2NzoxODpjYTplODo5MDo5MzpkMDoKICAgICAgICAgICAgICAgICAgICBlMTo1NDoyOTpmYjo0ZTo4NzoyMzo2Zjo2MTo2ZjpkYzphZjoxNzozZDo4YzoKICAgICAgICAgICAgICAgICAgICBiNzo3Yjo2YTphNjpkYjpiZDplZTo0MTo5NjpkOTozMDo5YTo1MTozZTplNDoKICAgICAgICAgICAgICAgICAgICA5MDo2NjplOTpmMzo0NTphOTo3NTo1ZToxYTo0NjowMjpmZTo0ODoyZDphYjoKICAgICAgICAgICAgICAgICAgICBjYjowZDo5Yzo2NjpkMzpmZjpmNTo5ZTo0MzozNTpmMjo4YjphMTpiYzozYzoKICAgICAgICAgICAgICAgICAgICBjNTpjNjpmZjpmNzoxMjoyMzozNzo4MzoyNjplOToxZTo4Yjo0ZDplYjowNToKICAgICAgICAgICAgICAgICAgICBhZToxOTpkZTo2MTowZDoyYzo3Njo5Yjo2MDpjNDo4NTowYzo2ZTpjNjo5ODoKICAgICAgICAgICAgICAgICAgICA5ZjowMzoyYTpjMTozZjo2ZTpiOTowNDpiNTo5Njo5Mzo3MDpmNjpiMjo3NDoKICAgICAgICAgICAgICAgICAgICA5YToyZDo3MTpiZjozOToyNjpjNDo0NzpkYjo4NTo5MDpkNzoxZToxYzo5MDoKICAgICAgICAgICAgICAgICAgICBkNTplNTo4ZDozYjplMjphNzoyNzo4ZTozNzoyNDowZToyNjo5ODoxMTo0NDoKICAgICAgICAgICAgICAgICAgICA2NDpmYzo1MjpjYTpmOToxMzo5NTowMjo5Zjo2NjpiNTpjYTpkYjo2Yzo3NDoKICAgICAgICAgICAgICAgICAgICBlZjphNQogICAgICAgICAgICAgICAgRXhwb25lbnQ6IDY1NTM3ICgweDEwMDAxKQogICAgICAgIFg1MDl2MyBleHRlbnNpb25zOgogICAgICAgICAgICBYNTA5djMgQmFzaWMgQ29uc3RyYWludHM6IAogICAgICAgICAgICAgICAgQ0E6RkFMU0UKICAgICAgICAgICAgWDUwOXYzIFN1YmplY3QgS2V5IElkZW50aWZpZXI6IAogICAgICAgICAgICAgICAgMjA6OUY6Njg6OTg6QUU6RkM6ODI6M0I6MjY6OTk6QUU6Njc6NkE6Mjk6NUI6NDQ6REE6MjA6NzI6NDcKICAgICAgICAgICAgWDUwOXYzIEF1dGhvcml0eSBLZXkgSWRlbnRpZmllcjogCiAgICAgICAgICAgICAgICBrZXlpZDowQjo0MTo0MTo4NjpGQjo0MTozNDozRTpGQzpDRDpCODozOTpFNTozNTo5Mjo1RDo5Njo0ODozNTo2MwogICAgICAgICAgICAgICAgRGlyTmFtZTovQz1VUy9TVD1UZXhhcy9MPUF1c3Rpbi9PPVN1YnRlcnJhbmVhbiBTZWN1cml0eS9DTj1TdWJ0ZXJyYW5lYW4gU2VjdXJpdHkgUm9vdCBDQS9lbWFpbEFkZHJlc3M9YWRtaW5Ac3VidGVycmFuZWFuLXNlY3VyaXR5LmNvbQogICAgICAgICAgICAgICAgc2VyaWFsOkIxOjVGOjZEOkM4OjdDOjc1OkUzOjEzCgogICAgICAgICAgICBOZXRzY2FwZSBDQSBSZXZvY2F0aW9uIFVybDogCiAgICAgICAgICAgICAgICBodHRwczovL3d3dy5leGFtcGxlLmNvbS9leGFtcGxlLWNhLWNybC5wZW0KICAgIFNpZ25hdHVyZSBBbGdvcml0aG06IHNoYTFXaXRoUlNBRW5jcnlwdGlvbgogICAgICAgICA0Mzo5MTpjZjo3MDo5MDo1NjoyMDo5MDpmZjplYjplMTo5NjozOTplMjowZDo4ODo0ODo4ZDoKICAgICAgICAgOTA6ZWY6NjA6Zjc6ZGY6NjQ6ZTA6NTg6MGM6NTU6MGU6MjI6MzU6OWQ6YzA6ZTI6MDU6M2I6CiAgICAgICAgIDNmOjA5OmRkOmE3Ojk1OjE4OjYyOjgxOmFlOjcwOmQ4OjA3OjJiOjM5Ojg3Ojc3OmY4OjFmOgogICAgICAgICA5YTpmZTpkNzo3NDo1NzpmYTpkMzo0Yjo0YzozMDpmMjphMjo4NDo0YjpjMTphMDo1YTozNDoKICAgICAgICAgNzA6OGE6ZGM6YWM6ZGQ6ZTQ6ZDM6NjY6ZjE6NTE6YmU6NTc6Mjk6NDk6Y2I6YTg6ZDI6NGM6CiAgICAgICAgIDFjOjE1OmUxOmZhOmI2OjFlOjVjOjg3OmVjOjMwOjIyOmY0OjQ1OmY0OmIyOjY0OjY0OjY3OgogICAgICAgICBlYjoxNDo5OTplNzpjYzo5Yzo3ODpjZDowMzpiOTpjOTo2MzoyMzo3NDpiMzozODplMjpiNDoKICAgICAgICAgMjA6MzQ6YmM6NjU6YTM6ZmE6NDU6NjE6Njk6ZTM6YTc6Y2I6OGM6YmQ6NDc6ZmU6MTY6MWU6CiAgICAgICAgIDA3OmVlOmFlOmEwOmZiOjRjOmU0OjJjOjM2OjAzOjY1OmFmOmM1OjdmOjczOmEyOjI5Ojg1OgogICAgICAgICAyODozZjowYjo3ODpmYToxNzo0Nzo3NTo0Yjo5YTpiNzo0ZDpjNjpiZTozZjplZDo5MjozNzoKICAgICAgICAgM2M6M2E6ZmY6YmE6Mzg6YTM6NmM6ODU6M2U6YjA6ZDE6OWY6ZTY6YzA6ODI6NTY6ZmY6NTY6CiAgICAgICAgIDRlOmY4OjFhOmE3OmY0OmVmOmQ4OjQ5OjFmOjRiOmEzOmYwOmVjOjhiOmNhOjQ2Ojk5OmFiOgogICAgICAgICA5Zjo3Njo0NDoyODozYzpjMzpjMTozZTpiZDo0NTpmNjpkNzoxYzo2NDozNzo0YTpmYjo2ZjoKICAgICAgICAgNzI6YzY6NTc6ZGM6Mjc6ZDI6NmE6YTc6OTk6ZWU6NGQ6NjQ6YzM6YjY6NTg6ZjQ6Mjc6YTk6CiAgICAgICAgIDhkOjc5OmQwOjQ0Ci0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlGQ1RDQ0EvR2dBd0lCQWdJQkFUQU5CZ2txaGtpRzl3MEJBUVVGQURDQnBqRUxNQWtHQTFVRUJoTUNWVk14CkRqQU1CZ05WQkFnTUJWUmxlR0Z6TVE4d0RRWURWUVFIREFaQmRYTjBhVzR4SGpBY0JnTlZCQW9NRlZOMVluUmwKY25KaGJtVmhiaUJUWldOMWNtbDBlVEVtTUNRR0ExVUVBd3dkVTNWaWRHVnljbUZ1WldGdUlGTmxZM1Z5YVhSNQpJRkp2YjNRZ1EwRXhMakFzQmdrcWhraUc5dzBCQ1FFV0gyRmtiV2x1UUhOMVluUmxjbkpoYm1WaGJpMXpaV04xCmNtbDBlUzVqYjIwd0hoY05NVGN3TlRJM01EVXdNVEl4V2hjTk1UZ3dOVEkzTURVd01USXhXakNCblRFTE1Ba0cKQTFVRUJoTUNWVk14RGpBTUJnTlZCQWdNQlZSbGVHRnpNUjR3SEFZRFZRUUtEQlZUZFdKMFpYSnlZVzVsWVc0ZwpVMlZqZFhKcGRIa3hIakFjQmdOVkJBc01GVk4xWW5SbGNuSmhibVZoYmlCVFpXTjFjbWwwZVRFT01Bd0dBMVVFCkF3d0ZaR1ZpZFdjeExqQXNCZ2txaGtpRzl3MEJDUUVXSDJGa2JXbHVRSE4xWW5SbGNuSmhibVZoYmkxelpXTjEKY21sMGVTNWpiMjB3Z2dFaU1BMEdDU3FHU0liM0RRRUJBUVVBQTRJQkR3QXdnZ0VLQW9JQkFRQ3ZpWjA4eG5wVApueHRkK0dNbGNuTUoyeXhuWWZMWXVnNnk1ZEsxTmZ2UjNLdWJhek5GczJXN0xrZkJwaUZYZ3BaOU1uc2NvWkdiCnJjc2RMWm5oSG9RSEF5b2pPRmRwQ2F3czFrdU1XbTRsWlB0RXlWRXZnUytlY2ZUV01GeEZHOHlGWnhqSzZKQ1QKME9GVUtmdE9oeU52WVcvY3J4YzlqTGQ3YXFiYnZlNUJsdGt3bWxFKzVKQm02Zk5GcVhWZUdrWUMva2d0cThzTgpuR2JULy9XZVF6WHlpNkc4UE1YRy8vY1NJemVESnVrZWkwM3JCYTRaM21FTkxIYWJZTVNGREc3R21KOERLc0UvCmJya0V0WmFUY1BheWRKb3RjYjg1SnNSSDI0V1ExeDRja05YbGpUdmlweWVPTnlRT0pwZ1JSR1Q4VXNyNUU1VUMKbjJhMXl0dHNkTytsQWdNQkFBR2pnZ0ZITUlJQlF6QUpCZ05WSFJNRUFqQUFNQjBHQTFVZERnUVdCQlFnbjJpWQpydnlDT3lhWnJtZHFLVnRFMmlCeVJ6Q0Iyd1lEVlIwakJJSFRNSUhRZ0JRTFFVR0crMEUwUHZ6TnVEbmxOWkpkCmxrZzFZNkdCcktTQnFUQ0JwakVMTUFrR0ExVUVCaE1DVlZNeERqQU1CZ05WQkFnTUJWUmxlR0Z6TVE4d0RRWUQKVlFRSERBWkJkWE4wYVc0eEhqQWNCZ05WQkFvTUZWTjFZblJsY25KaGJtVmhiaUJUWldOMWNtbDBlVEVtTUNRRwpBMVVFQXd3ZFUzVmlkR1Z5Y21GdVpXRnVJRk5sWTNWeWFYUjVJRkp2YjNRZ1EwRXhMakFzQmdrcWhraUc5dzBCCkNRRVdIMkZrYldsdVFITjFZblJsY25KaGJtVmhiaTF6WldOMWNtbDBlUzVqYjIyQ0NRQ3hYMjNJZkhYakV6QTUKQmdsZ2hrZ0JodmhDQVFRRUxCWXFhSFIwY0hNNkx5OTNkM2N1WlhoaGJYQnNaUzVqYjIwdlpYaGhiWEJzWlMxagpZUzFqY213dWNHVnRNQTBHQ1NxR1NJYjNEUUVCQlFVQUE0SUJBUUJEa2M5d2tGWWdrUC9yNFpZNTRnMklTSTJRCjcyRDMzMlRnV0F4VkRpSTFuY0RpQlRzL0NkMm5sUmhpZ2E1dzJBY3JPWWQzK0IrYS90ZDBWL3JUUzB3dzhxS0UKUzhHZ1dqUndpdHlzM2VUVFp2RlJ2bGNwU2N1bzBrd2NGZUg2dGg1Y2grd3dJdlJGOUxKa1pHZnJGSm5uekp4NAp6UU81eVdNamRMTTQ0clFnTkx4bG8vcEZZV25qcDh1TXZVZitGaDRIN3E2ZyswemtMRFlEWmEvRmYzT2lLWVVvClB3dDQraGRIZFV1YXQwM0d2ai90a2pjOE92KzZPS05zaFQ2dzBaL213SUpXLzFaTytCcW45Ty9ZU1I5TG8vRHMKaThwR21hdWZka1FvUE1QQlByMUY5dGNjWkRkSysyOXl4bGZjSjlKcXA1bnVUV1REdGxqMEo2bU5lZEJFCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K";

	@Test
	@DisplayName("Parse a base-64 encoded certificate")
	void parseString() throws CertificateException {

		assertEquals(new Date(1495861281000L), S7SCertificate.of(testCert).certificate().getNotBefore());
		assertEquals(new Date(1527397281000L), S7SCertificate.of(testCert).certificate().getNotAfter());

	}

	@Test
	@DisplayName("Parse a certificate")
	void parseBytes() throws CertificateException {

		assertEquals(new Date(1495861281000L),
				S7SCertificate.of(Base64.getDecoder().decode(testCert)).certificate().getNotBefore());
		assertEquals(new Date(1527397281000L),
				S7SCertificate.of(Base64.getDecoder().decode(testCert)).certificate().getNotAfter());
	}

	@Test
	@DisplayName("Check the format of a certificate's info string")
	void getInfoString() throws CertificateException {
		assertEquals(
				"Signature:(SHA1withRSA)1.2.840.113549.1.9.1=#161f61646d696e4073756274657272616e65616e2d73656375726974792e636f6d,CN=debug,OU=SubterraneanSecurity,O=SubterraneanSecurity,ST=Texas,C=USValidityNotBefore:"
						+ (new Date(1495861281000L) + "NotAfter:" + new Date(1527397281000L)).replaceAll("\\s", "")
						+ "Publickey:(RSA)30:82:01:22:30:0d:06:09:2a:86:48:86:f7:0d:01:01:01:05:00:03:82:01:0f:00:30:82:01:0a:02:82:01:01:00:af:89:9d:3c:c6:7a:53:9f:1b:5d:f8:63:25:72:73:09:db:2c:67:61:f2:d8:ba:0e:b2:e5:d2:b5:35:fb:d1:dc:ab:9b:6b:33:45:b3:65:bb:2e:47:c1:a6:21:57:82:96:7d:32:7b:1c:a1:91:9b:ad:cb:1d:2d:99:e1:1e:84:07:03:2a:23:38:57:69:09:ac:2c:d6:4b:8c:5a:6e:25:64:fb:44:c9:51:2f:81:2f:9e:71:f4:d6:30:5c:45:1b:cc:85:67:18:ca:e8:90:93:d0:e1:54:29:fb:4e:87:23:6f:61:6f:dc:af:17:3d:8c:b7:7b:6a:a6:db:bd:ee:41:96:d9:30:9a:51:3e:e4:90:66:e9:f3:45:a9:75:5e:1a:46:02:fe:48:2d:ab:cb:0d:9c:66:d3:ff:f5:9e:43:35:f2:8b:a1:bc:3c:c5:c6:ff:f7:12:23:37:83:26:e9:1e:8b:4d:eb:05:ae:19:de:61:0d:2c:76:9b:60:c4:85:0c:6e:c6:98:9f:03:2a:c1:3f:6e:b9:04:b5:96:93:70:f6:b2:74:9a:2d:71:bf:39:26:c4:47:db:85:90:d7:1e:1c:90:d5:e5:8d:3b:e2:a7:27:8e:37:24:0e:26:98:11:44:64:fc:52:ca:f9:13:95:02:9f:66:b5:ca:db:6c:74:ef:a5:02:03:01:00:01",
				S7SCertificate.of(testCert).getInfoString().replaceAll("\\s", ""));

	}

	@Test
	@DisplayName("Check timestamp validity of an expired certificate")
	void getValidity() throws CertificateException {
		assertFalse(S7SCertificate.of(testCert).checkValidity());
	}

}
