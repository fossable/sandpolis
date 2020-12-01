package com.sandpolis.plugin.sysinfo.agent.vanilla.osquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.StoreBase;

public class OQStore extends StoreBase {

	private static final Logger log = LoggerFactory.getLogger(OQStore.class);

	protected OQStore() {
		super(log);
	}

}
