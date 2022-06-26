//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance;

public abstract class InitTask {

	public abstract TaskOutcome run(TaskOutcome.Factory outcome) throws Exception;

	public abstract String description();

	public boolean fatal() {
		return false;
	}

	public boolean enabled() {
		return true;
	}

	/**
	 * Represents the outcome of an {@link InitTask}.
	 */
	public record TaskOutcome(String name, boolean success, boolean skipped, long duration, String reason,
			Throwable exception) {

		public record Factory(String name, long start) {

			public static Factory of(String name) {
				return new Factory(name, System.currentTimeMillis());
			}

			/**
			 * Mark the task as complete.
			 *
			 * @param result The task result
			 * @return A completed {@link TaskOutcome}
			 */
			public TaskOutcome completed(boolean result) {
				return new TaskOutcome(name, result, false, System.currentTimeMillis() - start, null, null);
			}

			/**
			 * Mark the task as failed.
			 *
			 * @return A completed {@link TaskOutcome}
			 */
			public TaskOutcome failed() {
				return new TaskOutcome(name, false, false, System.currentTimeMillis() - start, null, null);
			}

			/**
			 * Mark the task as failed with an exception.
			 *
			 * @param t The relevant exception
			 * @return A completed {@link TaskOutcome}
			 */
			public TaskOutcome failed(Exception t) {
				return new TaskOutcome(name, false, false, System.currentTimeMillis() - start, t.getMessage(), t);
			}

			/**
			 * Mark the task as failed with a comment.
			 *
			 * @param comment The relevant comment
			 * @return A completed {@link TaskOutcome}
			 */
			public TaskOutcome failed(String comment) {
				return new TaskOutcome(name, false, false, System.currentTimeMillis() - start, comment, null);
			}

			/**
			 * Mark the task as skipped.
			 *
			 * @return A completed {@link TaskOutcome}
			 */
			public TaskOutcome skipped() {
				return new TaskOutcome(name, false, true, System.currentTimeMillis() - start, null, null);
			}

			/**
			 * Mark the task as succeeded.
			 *
			 * @return A completed {@link TaskOutcome}
			 */
			public TaskOutcome succeeded() {
				return new TaskOutcome(name, true, false, System.currentTimeMillis() - start, null, null);
			}
		}
	}
}
