//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.installer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a {@link Path} on the system which may receive installation
 * artifacts.
 */
public final class InstallPath {

	private List<Object> candidates;

	/**
	 * Get the highest precedence {@link Path}.
	 *
	 * @return
	 */
	public Optional<Path> evaluate() {
		return candidates.stream().map(this::convert).filter(Objects::nonNull).findFirst();
	}

	/**
	 * Get a list of path candidates that are writable.
	 *
	 * @return A list of writable path candidates
	 */
	public List<Path> evaluateWritable() {
		return candidates.stream().map(this::convert).filter(Objects::nonNull).filter(this::isWritable)
				.collect(Collectors.toList());
	}

	private Path convert(Object candidate) {
		if (candidate instanceof String)
			candidate = Paths.get((String) candidate);

		if (candidate instanceof InstallPath)
			candidate = ((InstallPath) candidate).evaluate().orElse(null);

		if (candidate instanceof Path)
			return (Path) candidate;

		return null;
	}

	/**
	 * Determine whether a {@link Path} is writable, or if it does not exist, some
	 * parent path is writable.
	 *
	 * @param path The input path
	 * @return Whether the path is writable
	 */
	private boolean isWritable(Path path) {
		if (Files.exists(path))
			return Files.isWritable(path);
		if (path.equals(path.getParent()))
			// Reached the root path
			return false;
		return isWritable(path.getParent());
	}

	private InstallPath() {
	}

	public static InstallPath of(Object... candidates) {
		InstallPath path = new InstallPath();
		path.candidates = Arrays.stream(candidates).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
		return path;
	}
}
