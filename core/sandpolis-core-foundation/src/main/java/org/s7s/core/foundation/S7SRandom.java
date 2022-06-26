//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

public final class S7SRandom {

	public static final RandomGenerator insecure = RandomGenerator.getDefault();

	public static final RandomGenerator secure = new SecureRandom();

	/**
	 * Generate a random alphabetic string of given length.
	 *
	 * @param characters The length of the random String.
	 * @return A new random String containing only [A-Z] and [a-z].
	 */
	public static String nextAlphabetic(int characters) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < characters; i++) {
			if (insecure.nextBoolean())
				buffer.append((char) insecure.nextInt('A', 'Z'));
			else
				buffer.append((char) insecure.nextInt('a', 'z'));
		}

		return buffer.toString();
	}

	/**
	 * Get a random item from an array.
	 *
	 * @param array The source array
	 * @return A random item from the array
	 */
	public static <E> E nextItem(E[] array) {
		return array[insecure.nextInt(0, array.length - 1)];
	}

	public static int nextNonzeroInt() {

		int next;

		// Instead of looping, let's just do a couple of if statements given how rare it
		// is to pull a zero
		next = insecure.nextInt();
		if (next != 0)
			return next;

		next = insecure.nextInt();
		if (next != 0)
			return next;

		next = insecure.nextInt();
		if (next != 0)
			return next;

		throw new RuntimeException(
				"The probability of reaching this statement is 1.262E-29 for any given invocation (assuming the PRNG is uniform)");
	}

	/**
	 * Generate a random numeric string of given length.
	 *
	 * @param digits The length of the random String.
	 * @return A new random String containing only [0-9].
	 */
	public static String nextNumeric(int digits) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < digits; i++) {
			buffer.append((char) insecure.nextInt('0', '9'));
		}

		return buffer.toString();
	}

	private S7SRandom() {
	}
}
