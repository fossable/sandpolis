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
package com.sandpolis.viewer.cli;

import com.sandpolis.core.instance.Instance;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.viewer.Viewer;

/**
 * This stub is the entry point for CLI Viewer instances. Control is given to
 * {@link MainDispatch} for initialization.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class Main {
	private Main() {
	}

	public static void main(String[] args) {
		Viewer.registerUI(new Cli());
		MainDispatch.dispatch(Viewer.class, args, Instance.VIEWER);
	}

}
