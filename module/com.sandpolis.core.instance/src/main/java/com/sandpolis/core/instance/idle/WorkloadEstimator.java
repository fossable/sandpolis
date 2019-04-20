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

/**
 * A {@link WorkloadEstimator} estimates JVM usage for efficient idle
 * scheduling. Implementations should be low overhead as to not defeat the
 * purpose of deferring idle task execution during heavy usage.
 * 
 * @author cilki
 * @since 5.0.0
 */
public interface WorkloadEstimator {

	/**
	 * The number of milliseconds between profiling snapshots.
	 */
	public static final int PROFILE_TIME = 50;

	/**
	 * Estimate current application workload.
	 * 
	 * @return A value between 0 and 1 (inclusive) that roughly represents the
	 *         application's workload as a percentage
	 */
	public double estimate() throws InterruptedException;

}