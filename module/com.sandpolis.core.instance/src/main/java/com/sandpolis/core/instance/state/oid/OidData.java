package com.sandpolis.core.instance.state.oid;

/**
 * {@link OidData} allows auxiliary data to be associated with an {@link Oid}.
 * 
 * @since 7.0.0
 *
 * @param <T> The data type
 */
public final class OidData<T> {

	/**
	 * The class of the corresponding attribute's value.
	 */
	public static final OidData<Class<?>> TYPE = new OidData<>();

	/**
	 * Whether the corresponding attribute is singular.
	 */
	public static final OidData<Boolean> SINGULARITY = new OidData<>();

	/**
	 * Whether the corresponding attribute is immutable.
	 */
	public static final OidData<Boolean> IMMUTABLE = new OidData<>();
}
