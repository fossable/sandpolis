package com.sandpolis.core.instance.state.oid;

public final class OidData<T> {
	public static final OidData<Class<?>> TYPE = new OidData<>();
	public static final OidData<Boolean> SINGULARITY = new OidData<>();
	public static final OidData<Boolean> READ_ONLY = new OidData<>();
}
