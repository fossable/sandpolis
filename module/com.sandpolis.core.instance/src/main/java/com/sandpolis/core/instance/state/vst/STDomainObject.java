//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.state.vst;

import com.sandpolis.core.foundation.Result.ErrorCode;

public interface STDomainObject {

	/**
	 * An {@link IncompleteObjectException} is thrown when a {@link VirtObject} is
	 * not {@link #complete} when expected to be.
	 */
	public static class IncompleteObjectException extends RuntimeException {
		private static final long serialVersionUID = -6332437282463564387L;
	}

	/**
	 * A {@link VirtObject} is complete if all required fields are present.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public default ErrorCode complete() {
		return ErrorCode.OK;
	}

	/**
	 * A {@link VirtObject} is valid if all present attributes pass value
	 * restrictions.
	 *
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public default ErrorCode valid() {
		return ErrorCode.OK;
	}
}
