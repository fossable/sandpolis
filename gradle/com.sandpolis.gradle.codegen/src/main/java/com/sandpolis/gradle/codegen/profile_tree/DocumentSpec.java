package com.sandpolis.gradle.codegen.profile_tree;

import java.util.TreeMap;

public class DocumentSpec {

	public String name;

	public TreeMap<Integer, String> documents;

	public TreeMap<Integer, String> collections;

	public TreeMap<Integer, AttributeSpec> attributes;
}
