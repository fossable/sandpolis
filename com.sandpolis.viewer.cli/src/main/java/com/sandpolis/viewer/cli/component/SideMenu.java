/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.viewer.cli.component;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.input.KeyStroke;

public class SideMenu extends ActionListBox {
	private SideMenuPanel parent;

	public SideMenu(SideMenuPanel parent) {
		this.parent = parent;
	}

	@Override
	public Result handleKeyStroke(KeyStroke key) {
		switch (key.getKeyType()) {
		case ArrowDown:
			parent.down();
			break;
		case ArrowUp:
			parent.up();
			break;
		case Enter:
			return Result.HANDLED;
		case PageDown:
			break;
		case PageUp:
			break;
		default:
			break;
		}
		return super.handleKeyStroke(key);
	}

}
