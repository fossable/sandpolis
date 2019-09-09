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
package com.sandpolis.core.net.loop;

import java.util.function.Supplier;

/**
 * This class models connection timeouts for a {@link ConnectionLoop} which may
 * remain static or exponentially increase over time.
 *
 * If an exponentially increasing timeout is desired, a maximum timeout must be
 * specified. Each iteration will have a successively longer timeout until the
 * maximum is achieved. This is useful for {@link ConnectionLoop}s that want to
 * ease up on a host that is consistently refusing connections.
 *
 * @author cilki
 * @since 5.0.0
 */
public class LoopCycle {

	/**
	 * The current timeout.
	 */
	private int timeout;

	/**
	 * The current iteration count.
	 */
	private int iteration;

	/**
	 * The exponential function that calculates each timeout.
	 */
	private final Supplier<Integer> exponential;

	/**
	 * Create an unchanging {@code LoopCycle}.
	 *
	 * @param timeout The timeout in milliseconds
	 */
	public LoopCycle(int timeout) {
		this(timeout, timeout);
	}

	/**
	 * Create a {@code LoopCycle} which increases at the default rate.
	 *
	 * @param timeout The initial timeout in milliseconds
	 * @param maximum The maximum timeout in milliseconds
	 */
	public LoopCycle(int timeout, int maximum) {
		this(timeout, maximum, 8);
	}

	/**
	 * Create a {@code LoopCycle} which increases with the given smoothness.
	 *
	 * @param timeout  The initial timeout in milliseconds
	 * @param maximum  The maximum timeout in milliseconds
	 * @param flatness The smoothness factor which controls how quickly the maximum
	 *                 is achieved
	 */
	public LoopCycle(int timeout, int maximum, double flatness) {
		if (timeout < 0)
			throw new IllegalArgumentException("Invalid initial timeout: " + timeout);
		if (maximum < timeout)
			throw new IllegalArgumentException(
					"The maximum timeout must be greater than or equal to the initial timeout");
		if (flatness < 1)
			throw new IllegalArgumentException("Invalid smoothing factor: " + flatness);

		this.timeout = timeout;

		this.exponential = () -> {
			return (int) Math.min(maximum, Math.exp(iteration / flatness) + timeout - 1);
		};
	}

	/**
	 * Retrieve the next timeout and increment the internal iteration counter.
	 *
	 * @return The next timeout which may be equal to or greater than the last value
	 *         that was returned
	 */
	public int nextTimeout() {
		iteration++;
		timeout = exponential.get();
		return timeout;
	}

	/**
	 * Retrieve the current timeout.
	 *
	 * @return The current timeout value
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Get the current iteration count.
	 *
	 * @return The current iteration count
	 */
	public int getIterations() {
		return iteration;
	}
}
