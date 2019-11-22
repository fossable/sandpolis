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
package com.sandpolis.installer.util;

import com.sandpolis.core.soi.Dependency;

import java.util.HashSet;
import java.util.Set;

/**
 * @author cilki
 * @since 5.1.2
 */
public final class InstallUtil {

	public static Set<String> computeDependencies(Dependency.SO_DependencyMatrix matrix, String coordinate) {
		Set<String> dependencies = new HashSet<>();
		computeDependencies(matrix, dependencies, coordinate);
		return dependencies;
	}

	/**
	 * Gather all dependencies of the artifact corresponding to the given
	 * coordinate.
	 *
	 * @param matrix       The dependency matrix
	 * @param dependencies The dependency set
	 * @param coordinate   The coordinate
	 */
	private static void computeDependencies(Dependency.SO_DependencyMatrix matrix, Set<String> dependencies,
			String coordinate) {
		if (dependencies.contains(coordinate))
			return;

		dependencies.add(coordinate);

		matrix.getArtifactList().stream()
				// Find the artifact in the matrix and iterate over its dependencies
				.filter(a -> a.getCoordinates().equals(coordinate)).findFirst().get().getDependencyList().stream()
				.map(matrix.getArtifactList()::get).map(a -> a.getCoordinates())
				.forEach(c -> computeDependencies(matrix, dependencies, c));

	}

	private InstallUtil() {
	}
}
