//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.profile.attribute;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.storage.database.converter.OsTypeConverter;
import com.sandpolis.core.proto.pojo.Attribute.ProtoAttribute;
import com.sandpolis.core.proto.util.Platform.OsType;
import com.sandpolis.core.proto.util.Result.ErrorCode;

/**
 * An {@link Attribute} contains a datum of one of the following types:
 * <ul>
 * <li>byte[]</li>
 * <li>String</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Double</li>
 * <li>Boolean</li>
 * <li>OsType</li>
 * </ul>
 *
 * @param <E> The type the {@link Attribute} stores
 * @author cilki
 * @since 4.0.0
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Attribute<E> implements ProtoType<ProtoAttribute> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected int db_id;

	/**
	 * The timestamp of the most recent update.
	 */
	@Column
	protected long timestamp;

	/**
	 * Get the current value of this {@link Attribute}.
	 *
	 * @return The current value
	 */
	public abstract E get();

	/**
	 * Set the current value of this {@link Attribute}.
	 *
	 * @param value The new value
	 */
	public void set(E value) {
		set(value, System.currentTimeMillis());
	}

	/**
	 * Set the current value of this {@link Attribute} with an arbitrary timestamp.
	 *
	 * @param value The new value
	 * @param time  The new value's timestamp
	 */
	public abstract void set(E value, long time);

	@Override
	public ErrorCode merge(ProtoAttribute delta) throws Exception {
		// TODO
		return ErrorCode.OK;
	}

	@Override
	public ProtoAttribute extract() {
		// TODO
		return null;
	}

	@Entity
	public static final class StringAttribute extends Attribute<String> {
		@Column
		private String value;

		@Override
		public String get() {
			return value;
		}

		@Override
		public void set(String value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	@Entity
	public static final class IntegerAttribute extends Attribute<Integer> {
		@Column
		private Integer value;

		@Override
		public Integer get() {
			return value;
		}

		@Override
		public void set(Integer value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	@Entity
	public static final class LongAttribute extends Attribute<Long> {
		@Column
		private Long value;

		@Override
		public Long get() {
			return value;
		}

		@Override
		public void set(Long value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	@Entity
	public static final class BooleanAttribute extends Attribute<Boolean> {
		@Column
		private Boolean value;

		@Override
		public Boolean get() {
			return value;
		}

		@Override
		public void set(Boolean value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	@Entity
	public static final class DoubleAttribute extends Attribute<Double> {
		@Column
		private Double value;

		@Override
		public Double get() {
			return value;
		}

		@Override
		public void set(Double value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	@Entity
	public static final class ByteAttribute extends Attribute<byte[]> {
		@Column
		private byte[] value;

		@Override
		public byte[] get() {
			return value;
		}

		@Override
		public void set(byte[] value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	@Entity
	public static final class OsTypeAttribute extends Attribute<OsType> {
		@Column
		@Convert(converter = OsTypeConverter.class)
		private OsType value;

		@Override
		public OsType get() {
			return value;
		}

		@Override
		public void set(OsType value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}
}
