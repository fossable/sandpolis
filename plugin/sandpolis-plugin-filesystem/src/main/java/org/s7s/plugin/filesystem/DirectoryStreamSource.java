//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.filesystem;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.s7s.core.instance.stream.StreamSource;
import org.s7s.plugin.filesystem.Messages.EV_DirectoryStream;
import org.s7s.plugin.filesystem.Messages.EV_DirectoryStream.DirectoryEntry;
import org.s7s.plugin.filesystem.Messages.EV_DirectoryStream.DirectoryEntry.Type;
import org.s7s.plugin.filesystem.Messages.EV_DirectoryStream.DirectoryEntry.UpdateType;
import org.s7s.plugin.filesystem.Messages.RQ_DirectoryStream;

/**
 * This class provides a convenient handle on the local filesystem.
 */
public class DirectoryStreamSource extends StreamSource<EV_DirectoryStream> {

	private static final Logger log = LoggerFactory.getLogger(DirectoryStreamSource.class);

	/**
	 * The maximum number of entries in an event.
	 */
	private static final int MAX_EVENT_ENTRIES = 45;

	/**
	 * Whether modification timestamps should be included in file listings.
	 */
	private final boolean list_mtimes;

	/**
	 * Whether access timestamps should be included in file listings.
	 */
	private final boolean list_atimes;

	/**
	 * Whether creation timestamps should be included in file listings.
	 */
	private final boolean list_ctimes;

	/**
	 * Whether file sizes should be included in file listings.
	 */
	private final boolean list_sizes;

	/**
	 * Whether MIME types should be included in file listings.
	 */
	private final boolean list_mimes;

	/**
	 * The current reference directory. Also known as the PWD or working directory.
	 */
	private Path ref;

	private final Path initial;

	/**
	 * A {@link WatchKey} for the reference directory.
	 */
	private WatchKey refWatchKey;

	/**
	 * A watch service for the reference directory.
	 */
	private WatchService refService;

	/**
	 * The resident filesystem.
	 */
	private final FileSystem filesystem;

	/**
	 * A thread that waits for events from the watch service.
	 */
	private final Thread processEvents = new Thread(() -> {
		try {
			while (!Thread.interrupted()) {
				WatchKey watchKey = refService.poll();
				if (watchKey != null) {

					var events = watchKey.pollEvents();
					for (int i = 0; i < events.size();) {
						var ev = EV_DirectoryStream.newBuilder();
						for (int j = 0; j < MAX_EVENT_ENTRIES && i < events.size(); j++, i++) {
							ev.addEntry(buildEntryFromEvent(events.get(i)));
						}

						submit(ev.build());
					}

					watchKey.reset();
				}
			}
		} catch (IOException e) {
			return;
		} catch (ClosedWatchServiceException e) {
			// TODO ensure the entire handle is closing
			return;
		}
	});

	public DirectoryStreamSource(RQ_DirectoryStream rq) {
		this(FileSystems.getDefault(), rq);
	}

	/**
	 * Construct a new handle on the given directory.
	 *
	 * @param fs The {@link FileSystem}
	 * @param rq The handle's immutable options
	 */
	public DirectoryStreamSource(FileSystem fs, RQ_DirectoryStream rq) {
		this.filesystem = Objects.requireNonNull(fs);

		initial = Paths.get(rq.getPath());
		if (!Files.isDirectory(initial))
			throw new IllegalArgumentException();

		this.list_mtimes = rq.getIncludeModifyTimestamps();
		this.list_atimes = rq.getIncludeAccessTimestamps();
		this.list_ctimes = rq.getIncludeCreateTimestamps();
		this.list_sizes = rq.getIncludeSizes();
		this.list_mimes = rq.getIncludeMimeTypes();

	}

	@Override
	public void start() {
		try {
			refService = filesystem.newWatchService();
			processEvents.start();
		} catch (IOException e) {
			log.warn("Failed to create watch service", e);
		}

		try {
			movePath(initial);
		} catch (IOException e) {
			log.warn("Failed to list directory", e);
		}
	}

	/**
	 * Get the path of the working directory.
	 *
	 * @return The absolute path of the working directory
	 */
	public String pwd() {
		return ref.toString();
	}

	/**
	 * Move the working directory up a single level.
	 *
	 * @return True if the working directory has been changed, false otherwise
	 */
	public boolean up() throws IOException {
		Path potential = ref.getParent();
		if (potential != null) {
			movePath(potential);
			return true;
		}
		return false;
	}

	/**
	 * Move the working directory down into the specified directory.
	 *
	 * @param directory The desired directory relative to the current working
	 *                  directory
	 * @return True if the working directory has been changed, false otherwise
	 */
	public boolean down(String directory) throws IOException {
		if (directory == null)
			throw new IllegalArgumentException();

		Path potential = ref.resolve(directory);
		if (Files.isDirectory(potential) && Files.exists(potential)) {
			movePath(potential);
			return true;
		}
		return false;
	}

	/**
	 * Move the working directory to the specified path.
	 *
	 * @param path The absolute path which will become the new working directory
	 * @return True if the working directory has been changed, false otherwise
	 */
	public boolean setPath(String path) throws IOException {
		if (path == null)
			throw new IllegalArgumentException();

		Path potential = Paths.get(path);
		if (Files.isDirectory(potential) && Files.exists(potential)) {
			movePath(potential);
			return true;
		}
		return false;
	}

	@Override
	public void close() {

		processEvents.interrupt();

		try {
			refService.close();
		} catch (Exception ignore) {
		} finally {
			refService = null;
		}
	}

	/**
	 * Change the current reference path to the given path.
	 *
	 * @param potential The new reference path
	 */
	private synchronized void movePath(Path potential) throws IOException {

		if (refWatchKey != null) {
			refWatchKey.cancel();
			refWatchKey = null;
		}

		var files = Files.list(potential).collect(Collectors.toList());
		for (int i = 0; i < files.size();) {
			var ev = EV_DirectoryStream.newBuilder().setPath(potential.toString());
			for (int j = 0; j < MAX_EVENT_ENTRIES && i < files.size(); j++, i++) {
				ev.addEntry(buildNewEntry(files.get(i)));
			}

			submit(ev.build());
		}

		ref = potential;
		try {
			refWatchKey = ref.register(refService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		} catch (IOException e) {
			log.error("Failed to register present working directory", e);
		}
	}

	/**
	 * Build a new {@link FileListlet} out of a {@link WatchEvent}.
	 *
	 * @param event The watch event
	 * @return A new {@link FileListlet} update
	 */
	private DirectoryEntry buildEntryFromEvent(@SuppressWarnings("rawtypes") WatchEvent event) throws IOException {
		Path path = (Path) event.context();
		Object kind = event.kind();

		if (kind == ENTRY_CREATE)
			return buildNewEntry(path);
		else if (kind == ENTRY_MODIFY)
			return buildModifiedEntry(path);
		else if (kind == ENTRY_DELETE)
			return buildDeletedEntry(path);
		else
			return DirectoryEntry.newBuilder().setUpdateType(UpdateType.OVERFLOW).build();

	}

	/**
	 * Build a new {@link DirectoryEntry} out of a {@link Path}.
	 *
	 * @param path The path
	 * @return A new {@link DirectoryEntry}
	 */
	private DirectoryEntry buildNewEntry(Path path) throws IOException {
		DirectoryEntry.Builder entry = DirectoryEntry.newBuilder();
		entry.setName(path.getFileName().toString());
		entry.setUpdateType(UpdateType.ENTRY_CREATE);

		if (Files.isRegularFile(path)) {
			entry.setType(Type.REGULAR_FILE);
		} else if (Files.isDirectory(path)) {
			entry.setType(Type.DIRECTORY);
		} else if (Files.isSymbolicLink(path)) {
			entry.setType(Type.SYMLINK);
		}

		if (list_mtimes) {
			entry.setModifyTimestamp(Files.getLastModifiedTime(path).toMillis());
		}
		if (list_atimes) {
			entry.setAccessTimestamp(((FileTime) Files.getAttribute(path, "lastAccessTime")).toMillis());
		}
		if (list_ctimes) {
			entry.setCreateTimestamp(((FileTime) Files.getAttribute(path, "creationTime")).toMillis());
		}

		if (list_sizes) {
			entry.setSize(entry.getType() == Type.DIRECTORY ? Files.list(path).count() : Files.size(path));
		}

		if (list_mimes) {
			entry.setMimeType(Files.probeContentType(path));
		}

		return entry.build();
	}

	/**
	 * Build a new {@link DirectoryEntry} out of a deleted {@link Path}.
	 *
	 * @param path The path
	 * @return A new {@link DirectoryEntry}
	 */
	private DirectoryEntry buildDeletedEntry(Path path) {
		DirectoryEntry.Builder entry = DirectoryEntry.newBuilder();
		entry.setName(path.getFileName().toString());
		entry.setUpdateType(UpdateType.ENTRY_DELETE);

		return entry.build();
	}

	/**
	 * Build a new {@link DirectoryEntry} out of a modified {@link Path}.
	 *
	 * @param path The path
	 * @return A new {@link DirectoryEntry}
	 */
	private DirectoryEntry buildModifiedEntry(Path path) throws IOException {
		DirectoryEntry.Builder entry = DirectoryEntry.newBuilder();
		entry.setName(path.getFileName().toString());
		entry.setUpdateType(UpdateType.ENTRY_MODIFY);

		if (list_mtimes) {
			entry.setModifyTimestamp(Files.getLastModifiedTime(path).toMillis());
		}
		if (list_sizes) {
			entry.setSize(entry.getType() == Type.DIRECTORY ? path.toFile().list().length : Files.size(path));
		}

		return entry.build();
	}
}
