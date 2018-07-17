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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.RepeatedTest;

class JMXEstimatorTest {

	private JMXEstimator estimator = new JMXEstimator();

	@RepeatedTest(value = 8)
	void testEstimatePerformance() throws InterruptedException {
		assumeTrue(ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported());

		long timestamp = System.currentTimeMillis();
		double load = estimator.estimate();
		assertTrue(System.currentTimeMillis() - timestamp < WorkloadEstimator.PROFILE_TIME * 2,
				"Performance failure: workload estimator took longer than twice the profile time.");

		assertTrue(load <= 1.0, "Invalid load: " + load);
		assertTrue(load >= 0.0, "Invalid load: " + load);
	}
}
