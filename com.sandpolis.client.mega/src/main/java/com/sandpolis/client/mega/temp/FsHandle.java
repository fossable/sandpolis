/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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

package com.sandpolis.client.mega.temp;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.Closeable;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sandpolis.core.proto.net.MCTemp.EV_FileListing;
import com.sandpolis.core.proto.net.MCTemp.FileListlet;
import com.sandpolis.core.proto.net.MCTemp.FileListlet.UpdateType;
import com.sandpolis.core.proto.net.MCTemp.FsHandleOptions;
import com.sandpolis.core.util.IDUtil;

/**
 * This class provides a convenient handle on the local filesystem.
 * 
 * @author cilki
 * @since 1.0.0
 */
public class FsHandle implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(FsHandle.class);

	/**
	 * The number of recent paths to cache.
	 */
	private static final int CACHE_SIZE = 4;

	/**
	 * The number of seconds to cache each path.
	 */
	private static final int CACHE_EXPIRE = 60;

	/**
	 * The handle ID.
	 */
	private int id;

	/**
	 * Whether modification timestamps should be included in file listings.
	 */
	private boolean list_mtimes;

	/**
	 * Whether access timestamps should be included in file listings.
	 */
	private boolean list_atimes;

	/**
	 * Whether creation timestamps should be included in file listings.
	 */
	private boolean list_ctimes;

	/**
	 * Whether file sizes should be included in file listings.
	 */
	private boolean list_sizes;

	/**
	 * Whether MIME types should be included in file listings.
	 */
	private boolean list_mimes;

	/**
	 * The current reference directory. Also known as the PWD or working directory.
	 */
	private Path ref;

	/**
	 * A {@link WatchKey} for the reference directory.
	 */
	private WatchKey refWatchKey;

	/**
	 * A cached listing for the reference
	 */
	private List<FileListlet> refListing;

	/**
	 * A watch service for the reference directory.
	 */
	private WatchService refService;

	/**
	 * A shared watch service for cached directories.
	 */
	private WatchService cacheService;

	/**
	 * The directory cache.
	 */
	private Cache<Path, CachedPath> pathCache;

	/**
	 * A list of callbacks which will be notified of changes to the reference
	 * directory.
	 */
	private List<Consumer<EV_FileListing>> callbacks = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Construct a new handle on the user's home directory.
	 */
	public FsHandle() {
		this(System.getProperty("user.home"));
	}

	/**
	 * Construct a new handle on the given directory.
	 * 
	 * @param start The start directory's path
	 */
	public FsHandle(String start) {
		this(start, FsHandleOptions.newBuilder().build());
	}

	/**
	 * Construct a new handle on the given directory.
	 * 
	 * @param start   The initial working directory
	 * @param options The handle's immutable options
	 */
	public FsHandle(String start, FsHandleOptions options) {
		this(FileSystems.getDefault(), start, options);
	}

	/**
	 * Construct a new handle on the given directory.
	 * 
	 * @param fs      The {@link FileSystem}
	 * @param start   The initial working directory
	 * @param options The handle's immutable options
	 */
	public FsHandle(FileSystem fs, String start, FsHandleOptions options) {
		if (fs == null)
			throw new IllegalArgumentException();
		if (start == null)
			throw new IllegalArgumentException();
		if (options == null)
			throw new IllegalArgumentException();

		Path startPath = Paths.get(start);
		if (!Files.isDirectory(startPath))
			throw new IllegalArgumentException();

		this.list_mtimes = options.getMtime();
		this.list_atimes = options.getAtime();
		this.list_ctimes = options.getCtime();
		this.list_sizes = options.getSize();
		this.list_mimes = options.getMime();

		try {
			refService = fs.newWatchService();
			cacheService = fs.newWatchService();

			pathCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE)
					.expireAfterAccess(CACHE_EXPIRE, TimeUnit.SECONDS).removalListener(notification -> {
						((CachedPath) notification.getValue()).watchKey.cancel();
					}).build();
			startPwdService();
		} catch (IOException e) {
			log.warn("Cache disabled due to watch service exception", e);
			pathCache = null;
		}

		movePath(startPath);
		id = IDUtil.fm();
	}

	/**
	 * Get the unique handle ID.
	 * 
	 * @return The ID associated with this handle
	 */
	public int getId() {
		return id;
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
	 * Add a new callback function.
	 * 
	 * @param callback A function that will receive file updates from the working
	 *                 directory
	 */
	public void addUpdateCallback(Consumer<EV_FileListing> callback) {
		if (callback == null)
			throw new IllegalArgumentException();

		callbacks.add(callback);
	}

	/**
	 * Fire each callback with the given update.
	 * 
	 * @param update The update event
	 */
	private void fireCallbacks(EV_FileListing update) {
		synchronized (callbacks) {
			callbacks.stream().forEach(c -> c.accept(update));
		}
	}

	/**
	 * Move the working directory up a single level.
	 * 
	 * @return True if the working directory has been changed, false otherwise
	 */
	public boolean up() {
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
	public boolean down(String directory) {
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
	public boolean setPath(String path) {
		if (path == null)
			throw new IllegalArgumentException();

		Path potential = Paths.get(path);
		if (Files.isDirectory(potential) && Files.exists(potential)) {
			movePath(potential);
			return true;
		}
		return false;
	}

	/**
	 * Get a new listing of the present working directory according to the internal
	 * option values.
	 * 
	 * @return A list of the files and directories in the working directory
	 * @throws IOException
	 */
	public List<FileListlet> list() throws IOException {
		// TODO add update callback in constructor to update refListing
		if (refListing == null) {
			try (Stream<Path> paths = Files.list(ref)) {
				refListing = paths.map(this::buildNewListlet).collect(Collectors.toList());
			}
		}

		return refListing;
	}

	@Override
	public void close() {
		try {
			refService.close();
		} catch (Exception ignore) {
		} finally {
			refService = null;
		}

		try {
			cacheService.close();
		} catch (Exception ignore) {
		} finally {
			cacheService = null;
		}
	}

	/**
	 * Change the current reference path to the given path.
	 * 
	 * @param potential The new reference path
	 */
	private void movePath(Path potential) {

		if (refWatchKey != null) {
			refWatchKey.cancel();
			refWatchKey = null;
		}

		if (pathCache != null && refListing != null) {
			// Cache current path
			try {
				pathCache.put(ref, new CachedPath(ref, refListing));
			} catch (IOException e) {
				log.error("Failed to cache current path", e);
			} finally {
				refListing = null;
			}
		}

		if (pathCache != null) {
			CachedPath potentialCached = pathCache.asMap().remove(potential);
			if (potentialCached != null) {
				refListing = potentialCached.getListing();
			}
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
	private FileListlet buildListletFromEvent(@SuppressWarnings("rawtypes") WatchEvent event) {
		Path path = (Path) event.context();
		Object kind = event.kind();

		if (kind == ENTRY_CREATE)
			return buildNewListlet(path);
		else if (kind == ENTRY_MODIFY)
			return buildModifiedListlet(path);
		else if (kind == ENTRY_DELETE)
			return buildDeletedListlet(path);
		else
			return FileListlet.newBuilder().setUpdateType(UpdateType.OVERFLOW).build();

	}

	/**
	 * Build a new {@link FileListlet} out of a {@link Path}.
	 * 
	 * @param path The path
	 * @return A new {@link FileListlet}
	 */
	private FileListlet buildNewListlet(Path path) {
		FileListlet.Builder listlet = FileListlet.newBuilder();
		listlet.setName(path.getFileName().toString());
		listlet.setDirectory(Files.isDirectory(path));
		listlet.setUpdateType(UpdateType.ENTRY_CREATE);

		if (list_mtimes) {
			try {
				listlet.setMtime(Files.getLastModifiedTime(path).toMillis());
			} catch (IOException e) {
				listlet.clearMtime();
			}
		}
		if (list_atimes) {
			try {
				listlet.setAtime(((FileTime) Files.getAttribute(path, "lastAccessTime")).toMillis());
			} catch (IOException e) {
				listlet.clearAtime();
			}
		}
		if (list_ctimes) {
			try {
				listlet.setCtime(((FileTime) Files.getAttribute(path, "creationTime")).toMillis());
			} catch (IOException e) {
				listlet.clearCtime();
			}
		}
		if (list_sizes) {
			try {
				listlet.setSize(listlet.getDirectory() ? Files.list(path).count() : Files.size(path));
			} catch (AccessDeniedException e) {
				listlet.clearSize();
			} catch (IOException e) {
				listlet.clearSize();
			}
		}
		if (list_mimes) {
			try {
				listlet.setMime(Files.probeContentType(path));
			} catch (IOException e) {
				listlet.clearMime();
			}
		}

		return listlet.build();
	}

	/**
	 * Build a new {@link FileListlet} out of a deleted {@link Path}.
	 * 
	 * @param path The path
	 * @return A new {@link FileListlet}
	 */
	private FileListlet buildDeletedListlet(Path path) {
		FileListlet.Builder listlet = FileListlet.newBuilder();
		listlet.setName(path.getFileName().toString());
		listlet.setUpdateType(UpdateType.ENTRY_DELETE);

		return listlet.build();
	}

	/**
	 * Build a new {@link FileListlet} out of a modified {@link Path}.
	 * 
	 * @param path The path
	 * @return A new {@link FileListlet}
	 */
	private FileListlet buildModifiedListlet(Path path) {
		FileListlet.Builder listlet = FileListlet.newBuilder();
		listlet.setName(path.getFileName().toString());
		listlet.setUpdateType(UpdateType.ENTRY_MODIFY);

		if (list_mtimes) {
			try {
				listlet.setMtime(Files.getLastModifiedTime(path).toMillis());
			} catch (IOException e) {
				listlet.clearMtime();
			}
		}
		if (list_sizes) {
			try {
				listlet.setSize(listlet.getDirectory() ? path.toFile().list().length : Files.size(path));
			} catch (IOException e) {
				listlet.clearSize();
			}
		}

		return listlet.build();
	}

	/**
	 * Start the thread that watches the PWD and dispatches updates to the
	 * callbacks.
	 */
	private void startPwdService() {
		new Thread(() -> {
			try {
				while (!Thread.interrupted()) {
					WatchKey watchKey = refService.poll(10, TimeUnit.MINUTES);
					if (watchKey != null) {
						Stream<FileListlet> stream = watchKey.pollEvents().stream().map(this::buildListletFromEvent);
						fireCallbacks(EV_FileListing.newBuilder().addAllListing(() -> stream.iterator()).build());

						watchKey.reset();
					}
				}
			} catch (InterruptedException e) {
				return;
			} catch (ClosedWatchServiceException e) {
				// TODO ensure the entire handle is closing
				return;
			}
		}).start();
	}

	/**
	 * Merge updates into a list of listlets.
	 */
	private static void merge(List<FileListlet> listing, List<FileListlet> updates) {
		for (FileListlet update : updates) {
			switch (update.getUpdateType()) {
			case ENTRY_CREATE:
				listing.add(update);
				break;
			case ENTRY_DELETE:
				listing.removeIf(file -> file.getName().equals(update.getName()));
				break;
			case ENTRY_MODIFY:
				// TODO
				break;
			case OVERFLOW:
				// TODO
				break;
			default:
				throw new IllegalArgumentException("Update listlet has no update type");
			}
		}
	}

	private class CachedPath {

		private List<FileListlet> listing;

		private WatchKey watchKey;

		public CachedPath(Path path, List<FileListlet> listing) throws IOException {
			if (path == null)
				throw new IllegalArgumentException();
			if (listing == null)
				throw new IllegalArgumentException();

			this.listing = listing;
			this.watchKey = path.register(cacheService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		}

		/**
		 * Get the path's current listing.
		 * 
		 * @return The updated listing.
		 */
		public List<FileListlet> getListing() {
			FsHandle.merge(listing, watchKey.pollEvents().stream().map(FsHandle.this::buildListletFromEvent)
					.collect(Collectors.toList()));
			return listing;
		}

	}
}
