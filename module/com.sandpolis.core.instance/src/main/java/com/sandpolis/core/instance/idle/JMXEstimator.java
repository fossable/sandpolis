/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.instance.idle;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * A stateful {@link WorkloadEstimator} that uses CPU tick history to determine
 * workload.<br>
 * <br>
 * 
 * @author cilki
 * @since 5.0.0
 */
public class JMXEstimator implements WorkloadEstimator {

	private ThreadMXBean mx = ManagementFactory.getThreadMXBean();

	@Override
	public double estimate() throws InterruptedException {
		// TODO implement

		return 0;
	}

	/**
	 * Get the number of CPU ticks used by the application.
	 * 
	 * @return The number of CPU ticks used by the application
	 */
	private long getTicks() {
		long ticks = 0;
		for (long id : mx.getAllThreadIds())
			ticks += mx.getThreadCpuTime(id);
		return ticks;
	}

}
