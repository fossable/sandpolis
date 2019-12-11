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
package com.sandpolis.core.util;

import java.util.Random;

/**
 * Utility methods for obtaining random values.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class RandUtil {
	private RandUtil() {
	}

	/**
	 * A fast PRNG not suitable for cryptographic applications.
	 */
	private static final Random insecureRandom = new Random();

	/**
	 * Get a random boolean value.
	 *
	 * @return The next boolean value from a Random object.
	 */
	public static boolean nextBoolean() {
		return insecureRandom.nextBoolean();
	}

	/**
	 * Get a random int value.
	 *
	 * @return The next int value from a Random object.
	 */
	public static int nextInt() {
		return insecureRandom.nextInt();
	}

	/**
	 * Get a random int value.
	 *
	 * @param lower The minimum bound (inclusive)
	 * @param upper The maximum bound (inclusive)
	 * @return The next int value from a Random object.
	 */
	public static int nextInt(int lower, int upper) {
		// Classic boundary formula
		return insecureRandom.nextInt(upper - lower + 1) + lower;
	}

	/**
	 * Get a random long value from the range [0, n].
	 *
	 * @param n The maximum bound (inclusive)
	 * @return The next random long
	 */
	public static long nextLong(long n) {
		return Math.abs(insecureRandom.nextLong()) % n;
	}

	/**
	 * Get a random long value from the specified range.
	 *
	 * @param lower The minimum bound (inclusive)
	 * @param upper The maximum bound (inclusive)
	 * @return The next random long
	 */
	public static long nextLong(long lower, long upper) {
		return nextLong(upper - lower + 1) + lower;
	}

	/**
	 * Get a random long value.
	 *
	 * @return The next random long
	 */
	public static long nextLong() {
		return insecureRandom.nextLong();
	}

	/**
	 * Generate a random alphabetic string of given length.
	 *
	 * @param characters The length of the random String.
	 * @return A new random String containing only [A-Z] and [a-z].
	 */
	public static String nextAlphabetic(int characters) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < characters; i++) {
			if (nextBoolean())
				buffer.append((char) nextInt('A', 'Z'));
			else
				buffer.append((char) nextInt('a', 'z'));
		}

		return buffer.toString();
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
			buffer.append((char) nextInt('0', '9'));
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
		return array[nextInt(0, array.length - 1)];
	}
}
