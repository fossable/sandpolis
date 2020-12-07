package com.sandpolis.core.server.generator;

import com.sandpolis.core.server.group.Group;

public interface Packager {

	public byte[] generate(Group group) throws Exception;

}
