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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.MoreFiles;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Generator.GenReport;
import com.sandpolis.core.util.TempUtil;
import com.sandpolis.server.gen.packager.BatPackager;
import com.sandpolis.server.gen.packager.ElfPackager;
import com.sandpolis.server.gen.packager.ExePackager;
import com.sandpolis.server.gen.packager.JarPackager;
import com.sandpolis.server.gen.packager.PyPackager;
import com.sandpolis.server.gen.packager.QrPackager;
import com.sandpolis.server.gen.packager.RbPackager;
import com.sandpolis.server.gen.packager.ShPackager;

/**
 * The parent class of all output generators.
 * 
 * @author cilki
 * @since 4.0.0
 */
public abstract class Generator {

	private static final Logger log = LoggerFactory.getLogger(Generator.class);

	/**
	 * The generator's configuration.
	 */
	protected GenConfig config;

	/**
	 * A temporary directory for the generator if it needs one.
	 */
	protected File temp;

	/**
	 * A report about the generation.
	 */
	protected GenReport.Builder report;

	/**
	 * The packager which is responsible for producing the final output.
	 */
	private Packager packager;

	protected Generator(GenConfig config) {
		if (config == null)
			throw new IllegalArgumentException();

		this.config = config;

		switch (config.getFormat()) {
		case BAT:
			packager = new BatPackager();
			break;
		case ELF:
			packager = new ElfPackager();
			break;
		case EXE:
			packager = new ExePackager();
			break;
		case JAR:
			packager = new JarPackager();
			break;
		case PY:
			packager = new PyPackager();
			break;
		case QR:
			packager = new QrPackager();
			break;
		case RB:
			packager = new RbPackager();
			break;
		case SH:
			packager = new ShPackager();
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Performs the generation synchronously.
	 */
	abstract protected Object run() throws Exception;

	public void generate() throws Exception {
		if (report != null)
			throw new IllegalStateException("A generator cannot be run more than once!");
		report = GenReport.newBuilder().setTimestamp(System.currentTimeMillis())
				.setDuration(System.currentTimeMillis());
		temp = TempUtil.getDir();

		try {
			packager.process(config, run());
		} finally {
			cleanup();
		}
	}

	/**
	 * Performs the clean up after generation. This method is idempotent.
	 */
	public void cleanup() {

		if (temp.exists()) {
			try {
				MoreFiles.deleteRecursively(temp.toPath());
			} catch (IOException e) {
				log.debug("Failed to delete temporary directory", e);
			} finally {
				report.setDuration(System.currentTimeMillis() - report.getDuration());
			}
		}
	}

	/**
	 * Get the generation report.
	 * 
	 * @return The generation report
	 */
	public GenReport getReport() {
		if (report == null)
			throw new IllegalStateException("The generator has not been started");

		return report.build();
	}

}
