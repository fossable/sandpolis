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
package com.sandpolis.server.vanilla.gen.packager;

import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.server.vanilla.gen.Packager;

/**
 * This {@link Packager} produces a QR code as a PNG file.
 *
 * @author cilki
 * @since 5.0.0
 */
public class QrPackager extends Packager {
	private QrPackager() {
	}

	public static final QrPackager INSTANCE = new QrPackager();

	@Override
	public byte[] process(GenConfig config, Object payload) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFileExtension() {
		return "qr";
	}

}
