package com.sandpolis.gradle.codegen.state;

public class RelationSpec {

	public String name;

	public String type;

	public boolean collection;

	public String simpleName() {
		return type.replaceAll(".*\\.", "");
	}
}
