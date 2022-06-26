//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.update.agent.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.s7s.plugin.update.msg.MsgUpgrade.Package;
import org.s7s.plugin.update.agent.java.library.Apt;
import org.s7s.plugin.update.agent.java.library.Pacman;

/**
 * A package manager performs operations such as:
 * <ul>
 * <li>Downloading packages from a remote repository</li>
 * <li>Installing packages onto the local system</li>
 * <li>Removing packages from the local system</li>
 * </ul>
 *
 * @since 7.0.0
 */
public abstract class PackageManager {

	/**
	 * A global (thread-safe) handle on the system's package manager or {@code null}
	 * if no compatible manager was found.
	 */
	public static final PackageManager INSTANCE = detectManager();

	private static PackageManager detectManager() {
		PackageManager pm;

		if ((pm = new Pacman()).getManagerCompatibility())
			return pm;
		if ((pm = new Apt()).getManagerCompatibility())
			return pm;

		return null;
	}

	/**
	 * Determine the package manager's compatibility.
	 *
	 * @return Whether the package manager is compatible with this system
	 */
	public boolean getManagerCompatibility() {
		try {
			return !getManagerVersion().isEmpty() && Files.exists(getManagerLocation());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Get the location of the package manager's binary on the filesystem.
	 *
	 * @return The package manager's file path
	 * @throws Exception
	 */
	public abstract Path getManagerLocation() throws Exception;

	/**
	 * Get the package manager's version string.
	 *
	 * @return The local version
	 * @throws Exception
	 */
	public abstract String getManagerVersion() throws Exception;

	/**
	 * Remove old packages from the local package cache.
	 *
	 * @throws Exception
	 */
	public abstract void clean() throws Exception;

	/**
	 * Get all currently installed packages.
	 *
	 * @return All locally installed packages
	 * @throws Exception
	 */
	public abstract List<Package> getInstalled() throws Exception;

	/**
	 * Gather advanced metadata for the given package.
	 *
	 * @param name The package name
	 * @return The package metadata
	 * @throws Exception
	 */
	public abstract Package getMetadata(String name) throws Exception;

	/**
	 * Get all packages that are currently outdated.
	 *
	 * @return All packages that have a newer version available
	 * @throws Exception
	 */
	public abstract List<Package> getOutdated() throws Exception;

	/**
	 * Install the given packages onto the local system.
	 *
	 * @param packages The packages to install
	 * @throws Exception
	 */
	public abstract void install(List<String> packages) throws Exception;

	/**
	 * Synchronize the local package database with all remote repositories.
	 *
	 * @throws Exception
	 */
	public abstract void refresh() throws Exception;

	/**
	 * Remove the given packages from the local system.
	 *
	 * @param packages The packages to uninstall
	 * @throws Exception
	 */
	public abstract void remove(List<String> packages) throws Exception;

	/**
	 * Upgrade the given packages to the latest available version.
	 *
	 * @param packages The packages to upgrade
	 * @throws Exception
	 */
	public abstract void upgrade(List<String> packages) throws Exception;
}
