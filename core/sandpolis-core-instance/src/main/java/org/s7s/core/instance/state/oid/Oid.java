//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.oid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.s7s.core.instance.state.oid.Oid.PathComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Range;

/**
 * <p>
 * An {@link Oid} corresponds to one or more objects in a real or virtual state
 * tree. OIDs locate objects in the state tree with an immutable sequence of
 * strings called the "path".
 *
 * <p>
 * When represented as a String, OID components are joined with "/" and prefixed
 * with the namespace followed by a ":".
 *
 * <h3>Concrete/Generic</h3>
 * <p>
 * An OID is either "concrete", meaning that it corresponds to exactly one
 * virtual object, or "generic" which means the OID corresponds to multiple
 * objects of the same type.
 */
public record Oid(

		/**
		 * The namespace identifier which is the module in which the Oid belongs.
		 */
		String namespace,

		/**
		 *
		 */
		PathComponent[] path,

		/**
		 *
		 */
		Range<Integer> indexSelection,

		/**
		 *
		 */
		Range<Long> timestampSelection) {

	public static record PathComponent(String element, String id, boolean hasId) {

		public PathComponent(String element, String id, boolean hasId) {
			if (!PATH_VALIDATOR.test(element)) {
				throw new IllegalArgumentException();
			}

			this.element = element;
			this.id = id;
			this.hasId = hasId;
		}

		public static PathComponent of(String component) {
			if (component.endsWith("()")) {
				return new PathComponent(component.substring(0, component.indexOf('(')), null, true);
			} else if (component.endsWith(")") && component.contains("(")) {
				return new PathComponent(component.substring(0, component.indexOf('(')),
						component.substring(component.indexOf('(') + 1, component.indexOf(')')), true);
			} else {
				return new PathComponent(component, null, false);
			}
		}

		public PathComponent resolve(String id) {
			if (!hasId) {
				throw new IllegalStateException();
			}

			return new PathComponent(element, id, true);
		}
	}

	private static final String DEFAULT_NAMESPACE = "org.s7s.core.instance";

	private static final Logger log = LoggerFactory.getLogger(Oid.class);

	private static final Predicate<String> NAMESPACE_VALIDATOR = Pattern.compile("^[a-z\\.]+$").asMatchPredicate();

	private static final Predicate<String> PATH_VALIDATOR = Pattern.compile("^[a-z0-9_\\-*]+$").asMatchPredicate();

	private static boolean checkRelationship(String[] ancestor, String[] descendant) {

		// The descendant cannot be shorter than the ancestor
		if (descendant.length < ancestor.length) {
			return false;
		}

		descendant = descendant.clone();
		ancestor = ancestor.clone();

		// Make any generic entries in ancestor also generic in the descendant
		for (int i = 0; i < ancestor.length; i++) {
			if (ancestor[i].equals("*")) {
				descendant[i] = "*";
			}
		}

		// Make any generic entries in descendant also generic in the ancestor
		for (int i = 0; i < ancestor.length; i++) {
			if (descendant[i].equals("*")) {
				ancestor[i] = "*";
			}
		}

		int index = Arrays.mismatch(descendant, ancestor);
		return index == -1 || index == ancestor.length;
	}

	public static Oid of(String oid, String... resolutions) {
		Objects.requireNonNull(oid);

		String namespace;
		List<PathComponent> path = new ArrayList<>();
		Range<Integer> indexSelection = null;
		Range<Long> timestampSelection = null;

		// Determine namespace
		var components = oid.split(":");
		if (components.length == 1) {
			namespace = DEFAULT_NAMESPACE;
		} else if (components.length == 2) {
			namespace = components[0];

			// Remove namespace
			oid = components[1];
		} else {
			throw new IllegalArgumentException("Invalid namespace");
		}

		// Validate namespace
		if (namespace == null || !NAMESPACE_VALIDATOR.test(namespace)) {
			throw new IllegalArgumentException("Illegal namespace: " + namespace);
		}

		for (var element : oid.split("/")) {
			path.add(PathComponent.of(element));
		}

		// Perform resolutions
		int i = 0;
		for (var r : resolutions) {
			for (; i < path.size(); i++) {
				if (path.get(i).hasId()) {
					path.set(i, path.get(i).resolve(r));
					break;
				}
			}
		}

		// Parse selector at end
		if (path.length > 0) {
			String last = path[path.length - 1];
			if (last.endsWith("]")) {
				int s = last.lastIndexOf('[');
				if (s == -1) {
					throw new IllegalArgumentException("Expected range selector '['");
				}
				var range = last.substring(s);
				if (range.isBlank()) {
					throw new IllegalArgumentException("Empty range selector");
				}

				// Remove selector from path
				path[path.length - 1] = last.substring(0, s);

				if (range.contains(",")) {
					var parts = range.split(",");
					if (parts.length != 2) {
						throw new IllegalArgumentException();
					}

					Integer left;
					Integer right;

					if (parts[0].equals("[")) {
						left = Integer.parseInt(parts[0].substring(0, parts[0].length() - 1));
					}
					if (parts[1].equals("]")) {
						right = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1));
					}

					if (left == null && right == null) {
						indexSelection = Range.all();
					} else if (left != null) {
						indexSelection = Range.atLeast(left);
					} else if (right != null) {
						indexSelection = Range.atMost(left);
					} else {
						indexSelection = Range.closed(left, right);
					}

				} else if (range.contains("-")) {
					var parts = range.split("-");
					if (parts.length != 2) {
						throw new IllegalArgumentException();
					}

					Long left;
					Long right;

					if (parts[0].equals("[")) {
						left = Long.parseLong(parts[0].substring(0, parts[0].length() - 1));
					}
					if (parts[1].equals("]")) {
						right = Long.parseLong(parts[1].substring(0, parts[1].length() - 1));
					}

					if (left == null && right == null) {
						timestampSelection = Range.all();
					} else if (left != null) {
						timestampSelection = Range.atLeast(left);
					} else if (right != null) {
						timestampSelection = Range.atMost(left);
					} else {
						timestampSelection = Range.closed(left, right);
					}

				} else {
					throw new IllegalArgumentException();
				}
			}
		}

		return new Oid(namespace, path.toArray(PathComponent[]::new), indexSelection, timestampSelection);
	}

	public Oid child(String id) {
		String[] childPath = Arrays.copyOf(path, path.length + 1);
		childPath[childPath.length - 1] = id;
		return new Oid(namespace, childPath, indexSelection, timestampSelection);
	}

	public String first() {
		if (path.length > 0) {
			return path[0].element();
		} else {
			return null;
		}
	}

	/**
	 * Determine whether this OID is an ancestor of the given OID.
	 *
	 * @param descendant The descendant OID
	 * @return Whether this OID is an ancestor
	 */
	public boolean isAncestorOf(Oid descendant) {
		Objects.requireNonNull(descendant);

		return isAncestorOf(descendant.path);
	}

	/**
	 * Determine whether this OID is an ancestor of the given OID.
	 *
	 * @param oid The descendant OID
	 * @return Whether this OID is an ancestor
	 */
	public boolean isAncestorOf(String[] descendant) {
		Objects.requireNonNull(descendant);

		return checkRelationship(this.path, descendant);
	}

	/**
	 * Determine whether the OID corresponds to exactly one entity (concrete) or
	 * multiple entities (generic). The OID is generic if it contains at least one
	 * empty component.
	 *
	 * @return Whether the OID is concrete
	 */
	public boolean isConcrete() {
		return !Arrays.stream(path()).anyMatch(c -> c.hasId());
	}

	/**
	 * Determine whether this OID is a descendant of the given OID.
	 *
	 * @param ancestor The ancestor OID
	 * @return Whether this OID is a descendant
	 */
	public boolean isDescendantOf(Oid ancestor) {
		Objects.requireNonNull(ancestor);

		return isDescendantOf(ancestor.path);
	}

	/**
	 * Determine whether this OID is a descendant of the given OID.
	 *
	 * @param ancestor The ancestor OID
	 * @return Whether this OID is a descendant
	 */
	public boolean isDescendantOf(String[] ancestor) {
		Objects.requireNonNull(ancestor);

		return checkRelationship(ancestor, this.path);
	}

	public String last() {
		if (path.length > 0) {
			return path[path.length - 1].element();
		} else {
			return null;
		}
	}

	public String pathString() {
		return Arrays.stream(path).map(PathComponent::element).collect(Collectors.joining("/"));
	}

	public Oid relative(String path) {
		return new Oid(namespace, ObjectArrays.concat(this.path, path.split("/"), String.class), indexSelection,
				timestampSelection);
	}

	public Oid resolve(String... resolutions) {

		var path = this.path.clone();

		int i = 0;
		for (var r : resolutions) {
			for (; i < path.length; i++) {
				if (path[i].hasId() && path[i].id() == null) {
					path[i++] = new PathComponent(path[i].element(), r, true);
					break;
				}
			}
		}

		return new Oid(namespace, path, indexSelection, timestampSelection);
	}

	public Oid resolveLast(String... resolutions) {

		var path = this.path.clone();

		int i = path.length - 1;
		for (var r : Lists.reverse(Arrays.asList(resolutions))) {
			for (; i > 0; i--) {
				if (path[i].hasId() && path[i].id() == null) {
					path[i++] = new PathComponent(path[i].element(), r, true);
					break;
				}
			}
		}

		return new Oid(namespace, path, indexSelection, timestampSelection);
	}

	@Override
	public String toString() {
		var string = new StringBuilder(namespace);
		string.append(":");

		for (int i = 0; i < path.length; i++) {
			string.append("/");
			string.append(path[i].element());
			if (path[i].hasId()) {
				string.append("(");
				string.append(path[i].id());
				string.append(")");
			}
		}

		return string.toString();
	}
}
