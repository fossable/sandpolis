package com.sandpolis.gradle.codegen.profile_tree;

import java.util.List;

public class CollectionSpec {

	public String name;
	
	public int tag;

	public List<AttributeSpec> attributes;

	public List<CollectionSpec> collections;

	public List<DocumentSpec> documents;
}
