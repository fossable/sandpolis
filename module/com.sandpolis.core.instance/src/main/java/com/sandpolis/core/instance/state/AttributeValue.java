package com.sandpolis.core.instance.state;

import java.util.List;

import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.State.ProtoAttributeValue;

/**
 * A wrapper for an {@link Attribute}'s value. All implementations are
 * automatically generated by the codegen plugin.
 *
 * @param <T> The type of the value
 */
abstract class AttributeValue<T> {

	/**
	 * Directly get the value.
	 * 
	 * @return The value
	 */
	public abstract T get();

	/**
	 * Directly set the value.
	 * 
	 * @param value The new value
	 */
	public abstract void set(T value);

	/**
	 * Build a new {@link AttributeValue} of the same type, but not necessarily with
	 * the same content.
	 */
	public abstract AttributeValue<T> clone();

	/**
	 * Get the value as a {@link ProtoAttributeValue}.
	 * 
	 * @return The value
	 */
	public abstract ProtoAttributeValue.Builder getProto();

	/**
	 * Set the value from a {@link ProtoAttributeValue}.
	 * 
	 * @param av The new value
	 */
	public abstract void setProto(ProtoAttributeValue av);

	/**
	 * Build a new attribute value according to the type of a test value. This is
	 * only necessary to build the very first {@link AttributeValue} for an
	 * attribute. It's more efficient to call {@link #clone} on an existing
	 * attribute value.
	 * 
	 * @param test The test value which is only relevant for its type
	 * @return A new {@link AttributeValue} of the correct type
	 */
	@SuppressWarnings("unchecked")
	static <T> AttributeValue<T> newAttributeValue(T test) {
		if (test instanceof String) {
			return (AttributeValue<T>) new StringAttributeValue();
		} else if (test instanceof Boolean) {
			return (AttributeValue<T>) new BooleanAttributeValue();
		} else if (test instanceof Integer) {
			return (AttributeValue<T>) new IntegerAttributeValue();
		} else if (test instanceof InstanceFlavor) {
			return (AttributeValue<T>) new InstanceFlavorAttributeValue();
		} else if (test instanceof InstanceType) {
			return (AttributeValue<T>) new InstanceTypeAttributeValue();
		} else if (test instanceof Long) {
			return (AttributeValue<T>) new LongAttributeValue();
		} else if (test instanceof List) {
			var list = ((List<?>) test);
			if (list.size() == 0)
				throw new IllegalArgumentException("List cannot be empty");

			// Take an element from the list for type discovery
			var item = list.get(0);
			if (item instanceof String) {
				return (AttributeValue<T>) new StringListAttributeValue();
			} else if (item instanceof Boolean) {
				return (AttributeValue<T>) new BooleanListAttributeValue();
			} else if (item instanceof Integer) {
				return (AttributeValue<T>) new IntegerListAttributeValue();
			} else if (item instanceof InstanceFlavor) {
				return (AttributeValue<T>) new InstanceFlavorListAttributeValue();
			} else if (item instanceof InstanceType) {
				return (AttributeValue<T>) new InstanceTypeListAttributeValue();
			} else if (item instanceof Long) {
				return (AttributeValue<T>) new LongListAttributeValue();
			}
		}

		throw new IllegalArgumentException("Unknown type");
	}
}