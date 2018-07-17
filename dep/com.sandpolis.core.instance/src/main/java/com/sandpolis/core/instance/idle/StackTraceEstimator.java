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

import java.util.Map;

import com.google.common.collect.Sets;

/**
 * A {@link WorkloadEstimator} that uses stacktraces to determine workload.<br>
 * <br>
 * Two snapshots are taken of the {@link ThreadGroup}'s stacktraces. The
 * snapshots are compared to determine which {@link Thread}s are currently
 * running. This implementation weighs all {@link Thread}s equally, so the
 * presence of many sleeping {@link Thread}s can skew the accuracy.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class StackTraceEstimator implements WorkloadEstimator {

	@Override
	public double estimate() throws InterruptedException {
		Map<Thread, StackTraceElement[]> snapshot1 = Thread.getAllStackTraces();
		Thread.sleep(PROFILE_TIME);
		Map<Thread, StackTraceElement[]> snapshot2 = Thread.getAllStackTraces();

		// The number of alive threads
		int alive = 0;

		// The number of threads that changed
		int changed = 0;

		for (Thread thread : Sets.union(snapshot1.keySet(), snapshot2.keySet())) {
			if (!thread.isAlive() || thread.isInterrupted())
				continue;

			StackTraceElement[] stack1 = snapshot1.get(thread);
			StackTraceElement[] stack2 = snapshot2.get(thread);
			if (stack1 == null || stack2 == null || stack1.length == 0 || stack2.length == 0)
				continue;

			alive++;

			if (!stack1[0].equals(stack2[0]))
				changed++;
		}

		return (double) changed / (double) alive;
	}

}
