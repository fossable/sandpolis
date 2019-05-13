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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.MainDispatch.InitializationTask;
import com.sandpolis.core.instance.MainDispatch.Task;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

class MainDispatchTest {

	@BeforeEach
	void setup() throws Exception {
		// Use reflection to manually reset MainDispatch
		Field tasks = MainDispatch.class.getDeclaredField("tasks");
		tasks.setAccessible(true);
		tasks.set(null, new ArrayList<Task>());

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

		Field flavor = MainDispatch.class.getDeclaredField("flavor");
		flavor.setAccessible(true);
		flavor.set(null, null);

		MainDispatch.register(EmptyTest.class);
		MainDispatch.register(SuccessTest.class);
		MainDispatch.register(NonfatalTest.class);
	}

	/**
	 * Test a main that performs no configuration
	 */
	static class EmptyTest {
		public static void main(String[] args) {
			assertArrayEquals(args, new String[] {});
		}
	}

	@Test
	@DisplayName("Dispatch a class that registers nothing")
	void dispatch_1() {
		MainDispatch.dispatch(EmptyTest.class, new String[] {}, Instance.CHARCOAL, InstanceFlavor.NONE);
	}

	/**
	 * Test a main that performs a successful configuration
	 */
	static class SuccessTest {
		public static void main(String[] args) {
			assertArrayEquals(args, new String[] { "37434" });
			MainDispatch.register(SuccessTest.setup);
		}

		@InitializationTask(name = "test", fatal = false)
		private static final Task setup = new Task((task) -> {
			return task.success();
		});
	}

	@Test
	@DisplayName("Dispatch a class that is successful")
	void dispatch_2() {
		MainDispatch.dispatch(SuccessTest.class, new String[] { "37434" }, Instance.CHARCOAL, InstanceFlavor.NONE);
	}

	/**
	 * Test a main that encounters a nonfatal error
	 */
	static class NonfatalTest {
		public static void main(String[] args) {
			assertArrayEquals(args, new String[] { "37434" });
			MainDispatch.register(NonfatalTest.setup);
		}

		@InitializationTask(name = "test", fatal = false)
		private static final Task setup = new Task((task) -> {
			return task.failure();
		});
	}

	@Test
	@DisplayName("Dispatch a class that encounters a non-fatal error")
	void dispatch_3() {
		MainDispatch.dispatch(NonfatalTest.class, new String[] { "37434" }, Instance.CHARCOAL, InstanceFlavor.NONE);
	}

}
