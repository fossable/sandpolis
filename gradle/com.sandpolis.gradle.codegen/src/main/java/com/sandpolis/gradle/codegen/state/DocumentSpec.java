package com.sandpolis.gradle.codegen.state;

import java.util.TreeMap;

public class DocumentSpec {

	/**
	 * The fully qualified document name.
	 */
	public String name;

	/**
	 * The OID of the parent document.
	 */
	public String parent;

	/**
	 * The document's sub-documents sorted by tag.
	 */
	public TreeMap<Integer, String> documents;

	/**
	 * The document's sub-collections sorted by tag.
	 */
	public TreeMap<Integer, String> collections;

	/**
	 * The document's attributes sorted by tag.
	 */
	public TreeMap<Integer, AttributeSpec> attributes;

	/**
	 * The document's relations sorted by tag.
	 */
	public TreeMap<Integer, RelationSpec> relations;

	public String shortName() {
		return name.replaceAll(".*\\.", "");
	}
}
