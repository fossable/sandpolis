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
package com.sandpolis.server.gen;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.server.gen.generator.MegaGen;

/**
 * Since performing generations is a costly and infrequent operation, GenConfigs
 * are executed serially. In the rare case that multiple generation requests are
 * received at nearly the same time, they are queued in the order of receipt. If
 * the queue reaches maximum capacity, some GenConfigs will be dropped and their
 * sender notified.
 * 
 * Adding elements to the generation queue triggers a worker thread. The worker
 * sequentially generates each GenConfig in the queue and sends the resulting
 * artifact to the requested location. Once the queue is exhausted, the worker
 * is shutdown.
 * 
 * When the queue is empty (which should be 99.9% of the time), this
 * architecture incurs very low memory overhead. The cost to start a new worker
 * when a GenConfig enters an empty queue is acceptable, so a thread pool should
 * not be used.
 * 
 * TODO: Use RateLimiter to prevent abuse of the generators. An authenticated
 * (and malicious) viewer can repeatedly fill the queue which would lead to
 * sustained high CPU usage on the server.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class GQ {
	private GQ() {
	}

	private static final int CAPACITY = 5;

	/**
	 * The queue which holds pending GenConfigs.
	 */
	private static final Queue<GenConfig> gq = new ArrayBlockingQueue<>(CAPACITY);

	/**
	 * The thread that processes each GenConfig in the queue.
	 */
	private static Thread worker;

	/**
	 * Add a new job to the queue.
	 * 
	 * @param config
	 *            The GenConfig to add.
	 * @return True if the GenConfig was accepted into the queue. False otherwise.
	 */
	public static synchronized boolean add(GenConfig config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (gq.offer(config)) {
			// A new task was added; launch the worker thread if necessary
			launchWorker();
			return true;
		}

		return false;
	}

	/**
	 * Launch the worker thread to process the queue unless it is already running.
	 */
	private static void launchWorker() {
		if (worker == null || !worker.isAlive()) {
			worker = new Thread(() -> {
				GenConfig config;
				while ((config = gq.poll()) != null) {
					Generator generator;
					switch (config.getOutputType()) {
					case JAR:
						generator = new MegaGen(config);
						break;
					default:
						generator = null;
						break;
					}

					generator.generate();
				}

				worker = null;
			});
			worker.start();
		}
	}
}
