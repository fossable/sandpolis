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

import static org.fusesource.jansi.Ansi.ansi;

import java.util.Arrays;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * ASCII art and text utilities.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class AsciiUtil {
	private AsciiUtil() {
	}

	/**
	 * Get ascii art that says "sandpolis".
	 * 
	 * @return A String of 6 lines that each measure 43 characters wide
	 */
	public static String getSandpolisArt() {
		return "                     _             _ _     \n ___  __ _ _ __   __| |_ __   ___ | (_)___ \n/ __|/ _` | '_ \\ / _` | '_ \\ / _ \\| | / __|\n\\__ \\ (_| | | | | (_| | |_) | (_) | | \\__ \\\n|___/\\__,_|_| |_|\\__,_| .__/ \\___/|_|_|___/\n                      |_|                  ";
	}

	/**
	 * Colors available to {@link #toRainbow(String)}.
	 */
	private static final Color[] RAINBOW = Arrays.copyOfRange(Color.values(), 1, 7);

	/**
	 * Randomly colorize an input String with ANSI escape codes. There are no
	 * consecutive letters that are the same color in a proper rainbow.
	 * 
	 * @param text The text to colorize
	 * @return The colorized text
	 */
	public static String toRainbow(String text) {
		if (text == null)
			throw new IllegalArgumentException();

		Ansi ansi = ansi(text.length());

		Color last = null;
		for (int i = 0; i < text.length(); i++) {
			Color rand = RandUtil.nextItem(RAINBOW);
			if (rand == last) {
				i--;
				continue;
			}

			last = rand;
			ansi.fg(rand).a(text.charAt(i));
		}

		return ansi.reset().toString();
	}

}