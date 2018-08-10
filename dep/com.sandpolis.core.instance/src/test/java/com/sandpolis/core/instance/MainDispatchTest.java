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
package com.sandpolis.core.instance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.MainDispatch.IdleTask;
import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.ShutdownTask;
import com.sandpolis.core.instance.idle.IdleLoop;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Result.Outcome;

class MainDispatchTest {

	@BeforeEach
	void setup() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// Use reflection to manually reset MainDispatch
		Field tasks = MainDispatch.class.getDeclaredField("tasks");
		tasks.setAccessible(true);
		tasks.set(null, new ArrayList<Supplier<Outcome>>());

		Field shutdown = MainDispatch.class.getDeclaredField("shutdown");
		shutdown.setAccessible(true);
		shutdown.set(null, new ArrayList<Runnable>());

		Field idle = MainDispatch.class.getDeclaredField("idle");
		idle.setAccessible(true);
		idle.set(null, null);

		Field main = MainDispatch.class.getDeclaredField("main");
		main.setAccessible(true);
		main.set(null, MainDispatch.class);

		Field instance = MainDispatch.class.getDeclaredField("instance");
		instance.setAccessible(true);
		instance.set(null, null);
	}

	/**
	 * Test a main that performs no configuration
	 */
	static class Empty {
		public static void main(String[] args) {
			assertArrayEquals(args, new String[] {});
		}
	}

	@Test
	void testDispatchEmpty() {
		MainDispatch.dispatch(Empty.class, new String[] {}, Instance.CHARCOAL);
	}

	/**
	 * Test a main that performs a successful configuration
	 */
	static class NoFailures {
		public static void main(String[] args) {
			assertArrayEquals(args, new String[] { "37434" });
			MainDispatch.register(NoFailures::setup1, NoFailures.class, "setup1");
		}

		@InitializationTask(fatal = false)
		private static Outcome setup1() {
			return Outcome.newBuilder().setResult(true).build();
		}
	}

	@Test
	void testDispatchNoFailures() {
		MainDispatch.dispatch(NoFailures.class, new String[] { "37434" }, Instance.CHARCOAL);
	}

	/**
	 * Test a main that encounters a nonfatal error
	 */
	static class Nonfatal {
		public static void main(String[] args) {
			assertArrayEquals(args, new String[] { "37434" });
			MainDispatch.register(Nonfatal::setup1, Nonfatal.class, "setup1");
		}

		@InitializationTask(fatal = false)
		private static Outcome setup1() {
			return Outcome.newBuilder().setResult(false).build();
		}
	}

	@Test
	void testDispatchFatal() {
		MainDispatch.dispatch(Nonfatal.class, new String[] { "37434" }, Instance.CHARCOAL);
	}

	/**
	 * Test a main that configures an idle loop
	 */
	static class Idle {
		public static void main(String[] args) {
			MainDispatch.register(new IdleLoop());
			MainDispatch.registerIdle(Idle::idleTask);
		}

		@IdleTask
		private static boolean idleTask() {
			return true;
		}
	}

	@Test
	void testIdle() {
		MainDispatch.dispatch(Idle.class, new String[] {}, Instance.CHARCOAL);
	}

	/**
	 * Test a main that configures a shutdown task
	 */
	static class Shutdown {

		public static void main(String[] args) {
			MainDispatch.registerShutdown(Shutdown::shutdownTask);
		}

		@ShutdownTask
		private static void shutdownTask() {
		}
	}

	@Test
	void testShutdown() {
		MainDispatch.dispatch(Shutdown.class, new String[] {}, Instance.CHARCOAL);
	}

}
