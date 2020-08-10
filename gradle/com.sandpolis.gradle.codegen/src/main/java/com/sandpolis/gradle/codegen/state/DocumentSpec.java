package com.sandpolis.gradle.codegen.state;

import java.util.TreeMap;

public class DocumentSpec {

	/**
	 * The full document name.
	 */
	public String name;

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

	public String shortName() {
		return name.replaceAll(".*\\.", "");
	}
}
