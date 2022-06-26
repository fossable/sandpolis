//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.st;

import static org.s7s.core.protocol.Stream.EV_STStreamData.newBuilder;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.protobuf.UnsafeByteOperations;
import org.s7s.core.foundation.Platform.OsType;
import org.s7s.core.foundation.S7SCertificate;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.protocol.Stream.EV_STStreamData;
import org.s7s.core.protocol.Stream.EV_STStreamData.ValueType;

public class EphemeralAttribute extends AbstractSTObject implements STAttribute {

	public static enum AttributeType {
		BOOLEAN( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getBoolean()), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.BOOLEAN) //
						.setBoolean((boolean) value.value())), //
		BOOLEAN_ARRAY( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getBooleanArrayList()), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.BOOLEAN_ARRAY) //
						.addAllBooleanArray(() -> Arrays.stream((Boolean[]) value.value()).iterator())), //
		INT_ARRAY( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getIntegerArrayList()), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.INTEGER_ARRAY) //
						.addAllIntegerArray(() -> Arrays.stream((int[]) value.value()).iterator())), //
		BYTES( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getBytes().toByteArray()), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp())//
						.setValueType(ValueType.BYTES) //
						.setBytes(UnsafeByteOperations.unsafeWrap((byte[]) value.value()))), //
		INSTANCE_FLAVOR( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						InstanceFlavor.forNumber(proto.getInteger())), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.INSTANCE_FLAVOR) //
						.setInteger(((InstanceFlavor) value.value()).getNumber())), //
		INSTANCE_TYPE( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						InstanceType.forNumber(proto.getInteger())), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.INSTANCE_TYPE) //
						.setInteger(((InstanceType) value.value()).getNumber())), //
		INTEGER( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getInteger()),
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.INTEGER) //
						.setInteger((Integer) value.value())), //
		LONG( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getLong()), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.LONG) //
						.setLong((Long) value.value())), //
		OS_TYPE( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						OsType.forNumber(proto.getInteger())), //
				value -> newBuilder()//
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.OS_TYPE) //
						.setInteger(((OsType) value.value()).getNumber())), //
		STRING( //
				proto -> new EphemeralAttributeValue(proto.getTimestamp(), //
						proto.getString()), //
				value -> newBuilder() //
						.setTimestamp(value.timestamp()) //
						.setValueType(ValueType.STRING) //
						.setString((String) value.value())), //
		X509CERTIFICATE( //
				proto -> {
					try {
						return new EphemeralAttributeValue(proto.getTimestamp(), //
								S7SCertificate.of(proto.getBytes().toByteArray()).certificate());
					} catch (CertificateException e) {
						return null;
					}
				}, //
				value -> {
					try {
						return newBuilder().setTimestamp(value.timestamp()) //
								.setBytes(UnsafeByteOperations
										.unsafeWrap(((X509Certificate) value.value()).getEncoded()));
					} catch (CertificateEncodingException e) {
						return null;
					}
				});

		public final Function<EphemeralAttributeValue, EV_STStreamData.Builder> pack;

		public final Function<EV_STStreamData, EphemeralAttributeValue> unpack;

		private AttributeType(Function<EV_STStreamData, EphemeralAttributeValue> unpack,
				Function<EphemeralAttributeValue, EV_STStreamData.Builder> pack) {
			this.unpack = unpack;
			this.pack = pack;
		}
	}

	public static record EphemeralAttributeValue(long timestamp, Object value) {
	}

	/**
	 * The current value of the attribute.
	 */
	protected EphemeralAttributeValue current;

	/**
	 * Historical values.
	 */
	protected List<EphemeralAttributeValue> history;

	/**
	 * A strategy that determines what happens to old values.
	 */
	protected RetentionPolicy retention;

	/**
	 * A quantifier for the retention policy.
	 */
	protected long retentionLimit;

	/**
	 * An optional supplier that overrides the current value.
	 */
	protected Supplier<?> source;

	protected AttributeType type;

	public EphemeralAttribute(STDocument parent, String id) {
		super(parent, id);
	}

	/**
	 * Check the retention condition and remove all violating elements.
	 */
	private void checkRetention() {
		if (retention == null)
			return;

		if (history == null)
			history = new ArrayList<>();

		switch (retention) {
		case ITEM_LIMITED:
			while (history.size() > retentionLimit) {
				history.remove(0);
			}
			break;
		case TIME_LIMITED:
			while (history.size() > 0 && history.get(0).timestamp() > (current.timestamp() - retentionLimit)) {
				history.remove(0);
			}
			break;
		case UNLIMITED:
			// Do nothing
			break;
		}
	}

	private AttributeType findType(Object value) {
		if (value instanceof String) {
			return AttributeType.STRING;
		}
		if (value instanceof Boolean) {
			return AttributeType.BOOLEAN;
		}
		if (value instanceof Long) {
			return AttributeType.LONG;
		}
		if (value instanceof Integer) {
			return AttributeType.INTEGER;
		}
		if (value instanceof X509Certificate) {
			return AttributeType.X509CERTIFICATE;
		}
		if (value instanceof boolean[] || value instanceof Boolean[]) {
			return AttributeType.BOOLEAN_ARRAY;
		}
		if (value instanceof int[] || value instanceof Integer[]) {
			return AttributeType.INT_ARRAY;
		}
		if (value instanceof byte[] || value instanceof Byte[]) {
			return AttributeType.BYTES;
		}
		if (value instanceof InstanceType) {
			return AttributeType.INSTANCE_TYPE;
		}
		if (value instanceof InstanceFlavor) {
			return AttributeType.INSTANCE_FLAVOR;
		}
		if (value instanceof OsType) {
			return AttributeType.OS_TYPE;
		}
		throw new IllegalArgumentException("Unknown attribute value type: " + value.getClass());
	}

	@Override
	public synchronized Object get() {
		if (source != null)
			return source.get();
		if (current == null)
			return null;

		return current.value();
	}

	@Override
	public synchronized List<EphemeralAttributeValue> history() {
		if (history == null)
			return List.of();

		return Collections.unmodifiableList(history);
	}

	AttributeType findType(EV_STStreamData.ValueType type) {
		switch (type) {
		case STRING:
			return AttributeType.STRING;
		case BOOLEAN:
			return AttributeType.BOOLEAN;
		case LONG:
			return AttributeType.LONG;
		case INTEGER:
			return AttributeType.INTEGER;
		case BOOLEAN_ARRAY:
			return AttributeType.BOOLEAN_ARRAY;
		case INTEGER_ARRAY:
			return AttributeType.INT_ARRAY;
		case BYTES:
			return AttributeType.BYTES;
		case INSTANCE_TYPE:
			return AttributeType.INSTANCE_TYPE;
		case INSTANCE_FLAVOR:
			return AttributeType.INSTANCE_FLAVOR;
		case OS_TYPE:
			return AttributeType.OS_TYPE;
		default:
			throw new IllegalArgumentException("Unknown attribute value type: " + type);
		}
	}

	@Override
	public synchronized void merge(EV_STStreamData snapshot) {

		// Set type if necessary
		if (type == null) {
			type = findType(snapshot.getValueType());
		}

		// TODO check for historical value

		var old = current;
		current = type.unpack.apply(snapshot);
		fireAttributeValueChangedEvent(this, old, current);
	}

	@Override
	public synchronized void set(Object value) {

		// Save the old value for inclusion in the event
		var old = current;

		if (value == null) {
			current = null;

			fireAttributeValueChangedEvent(this, old, null);
			return;
		}

		if (type == null) {
			// Determine type experimentally
			type = findType(value);
		} else {
			// Assert the type has not changed
			if (type != findType(value)) {
				throw new IllegalArgumentException();
			}
		}

		// If retention is not enabled, then overwrite the old value
		if (retention == null) {
			current = new EphemeralAttributeValue(System.currentTimeMillis(), value);
		}

		// Retention is enabled
		else {
			// Move current value into history
			history.add(current);

			// Set current value
			current = new EphemeralAttributeValue(System.currentTimeMillis(), value);

			// Take action on the old values if necessary
			checkRetention();
		}

		fireAttributeValueChangedEvent(this, old, current);
	}

	public synchronized void setRetention(RetentionPolicy retention) {
		this.retention = retention;
		checkRetention();
	}

	public synchronized void setRetention(RetentionPolicy retention, int limit) {
		this.retention = retention;
		this.retentionLimit = limit;
		checkRetention();
	}

	@Override
	public synchronized Stream<EV_STStreamData> snapshot(STSnapshotStruct config) {

		if (!isPresent())
			// Empty attribute shortcut
			return Stream.empty();

		// Check the retention condition before serializing
		checkRetention();

		// Determine relative OID
		var relative_oid = Arrays.stream(oid().path()).skip(config.oid.path().length).collect(Collectors.joining("/"));

		// TODO historical values

		// Request for current value only
		if (source != null) {
			var value = source.get();
			if (type == null) {
				// Determine type experimentally
				type = findType(value);
			}

			return Stream.of(type.pack.apply(new EphemeralAttributeValue(System.currentTimeMillis(), value))
					.setOid(relative_oid).build());
		} else {
			return Stream.of(type.pack.apply(current).setOid(relative_oid).build());
		}
	}

	@Override
	public synchronized void source(Supplier<?> source) {
		this.source = source;
	}

	/**
	 * @return The timestamp associated with the current value
	 */
	public synchronized long timestamp() {
		if (source != null)
			return 0;
		if (current == null)
			return 0;

		return current.timestamp();
	}

	@Override
	public String toString() {
		if (current != null)
			return current.toString();
		return null;
	}
}
