package com.sandpolis.core.instance.state;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Function;

import com.google.protobuf.ByteString;
import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.State.ProtoAttributeValue;
import com.sandpolis.core.instance.state.converter.InstanceFlavorConverter;
import com.sandpolis.core.instance.state.converter.InstanceTypeConverter;
import com.sandpolis.core.instance.state.converter.OsTypeConverter;
import com.sandpolis.core.instance.state.converter.X509CertificateConverter;

@SuppressWarnings("rawtypes")
public interface STAttributeValue<T> {

	// Boolean list
	static final Function<ProtoAttributeValue, List<Boolean>> BOOLEAN_LIST_DESERIALIZER //
			= proto -> proto.getBooleanList();

	static final Function<List<Boolean>, ProtoAttributeValue.Builder> BOOLEAN_LIST_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addAllBoolean(value);

	static final Function[] BOOLEAN_LIST = { BOOLEAN_LIST_SERIALIZER, BOOLEAN_LIST_DESERIALIZER };

	// Boolean
	static final Function<ProtoAttributeValue, Boolean> BOOLEAN_DESERIALIZER //
			= proto -> proto.getBoolean(0);

	static final Function<Boolean, ProtoAttributeValue.Builder> BOOLEAN_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addBoolean(value);

	static final Function[] BOOLEAN = { BOOLEAN_SERIALIZER, BOOLEAN_DESERIALIZER };

	// Byte array
	static final Function<ProtoAttributeValue, byte[]> BYTES_DESERIALIZER //
			= proto -> proto.getBytes(0).toByteArray();

	static final Function<byte[], ProtoAttributeValue.Builder> BYTES_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addBytes(ByteString.copyFrom(value));

	static final Function[] BYTES = { BYTES_SERIALIZER, BYTES_DESERIALIZER };

	// Double list
	static final Function<ProtoAttributeValue, List<Double>> DOUBLE_LIST_DESERIALIZER //
			= proto -> proto.getDoubleList();

	static final Function<List<Double>, ProtoAttributeValue.Builder> DOUBLE_LIST_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addAllDouble(value);

	static final Function[] DOUBLE_LIST = { DOUBLE_LIST_SERIALIZER, DOUBLE_LIST_DESERIALIZER };

	// Double
	static final Function<ProtoAttributeValue, Double> DOUBLE_DESERIALIZER //
			= proto -> proto.getDouble(0);

	static final Function<Double, ProtoAttributeValue.Builder> DOUBLE_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addDouble(value);

	static final Function[] DOUBLE = { DOUBLE_SERIALIZER, DOUBLE_DESERIALIZER };

	// Integer list
	static final Function<ProtoAttributeValue, List<Integer>> INTEGER_LIST_DESERIALIZER //
			= proto -> proto.getIntegerList();

	static final Function<List<Integer>, ProtoAttributeValue.Builder> INTEGER_LIST_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addAllInteger(value);

	static final Function[] INTEGER_LIST = { INTEGER_LIST_SERIALIZER, INTEGER_LIST_DESERIALIZER };

	// Integer
	static final Function<ProtoAttributeValue, Integer> INTEGER_DESERIALIZER //
			= proto -> proto.getInteger(0);

	static final Function<Integer, ProtoAttributeValue.Builder> INTEGER_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addInteger(value);

	static final Function[] INTEGER = { INTEGER_SERIALIZER, INTEGER_DESERIALIZER };

	// InstanceFlavor
	static final Function<ProtoAttributeValue, InstanceFlavor> INSTANCEFLAVOR_DESERIALIZER //
			= INTEGER_DESERIALIZER.andThen(InstanceFlavorConverter.DESERIALIZER);

	static final Function<InstanceFlavor, ProtoAttributeValue.Builder> INSTANCEFLAVOR_SERIALIZER //
			= InstanceFlavorConverter.SERIALIZER.andThen(INTEGER_SERIALIZER);

	static final Function[] INSTANCEFLAVOR = { INSTANCEFLAVOR_SERIALIZER, INSTANCEFLAVOR_DESERIALIZER };

	// InstanceType
	static final Function<ProtoAttributeValue, InstanceType> INSTANCETYPE_DESERIALIZER //
			= INTEGER_DESERIALIZER.andThen(InstanceTypeConverter.DESERIALIZER);

	static final Function<InstanceType, ProtoAttributeValue.Builder> INSTANCETYPE_SERIALIZER //
			= InstanceTypeConverter.SERIALIZER.andThen(INTEGER_SERIALIZER);

	static final Function[] INSTANCETYPE = { INSTANCETYPE_SERIALIZER, INSTANCETYPE_DESERIALIZER };

	// Long list
	static final Function<ProtoAttributeValue, List<Long>> LONG_LIST_DESERIALIZER //
			= proto -> proto.getLongList();

	static final Function<List<Long>, ProtoAttributeValue.Builder> LONG_LIST_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addAllLong(value);

	static final Function[] LONG_LIST = { LONG_LIST_SERIALIZER, LONG_LIST_DESERIALIZER };

	// Long
	static final Function<ProtoAttributeValue, Long> LONG_DESERIALIZER //
			= proto -> proto.getLong(0);

	static final Function<Long, ProtoAttributeValue.Builder> LONG_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addLong(value);

	static final Function[] LONG = { LONG_SERIALIZER, LONG_DESERIALIZER };

	// OsType
	static final Function<ProtoAttributeValue, OsType> OSTYPE_DESERIALIZER //
			= INTEGER_DESERIALIZER.andThen(OsTypeConverter.DESERIALIZER);

	static final Function<OsType, ProtoAttributeValue.Builder> OSTYPE_SERIALIZER //
			= OsTypeConverter.SERIALIZER.andThen(INTEGER_SERIALIZER);

	static final Function[] OSTYPE = { OSTYPE_SERIALIZER, OSTYPE_DESERIALIZER };

	// String list
	static final Function<ProtoAttributeValue, List<String>> STRING_LIST_DESERIALIZER //
			= proto -> proto.getStringList();

	static final Function<List<String>, ProtoAttributeValue.Builder> STRING_LIST_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addAllString(value);

	static final Function[] STRING_LIST = { STRING_LIST_SERIALIZER, STRING_LIST_DESERIALIZER };

	// String
	static final Function<ProtoAttributeValue, String> STRING_DESERIALIZER //
			= proto -> proto.getString(0);

	static final Function<String, ProtoAttributeValue.Builder> STRING_SERIALIZER //
			= value -> ProtoAttributeValue.newBuilder().addString(value);

	static final Function[] STRING = { STRING_SERIALIZER, STRING_DESERIALIZER };

	// X509Certificate
	static final Function<ProtoAttributeValue, X509Certificate> X509CERTIFICATE_DESERIALIZER //
			= BYTES_DESERIALIZER.andThen(X509CertificateConverter.DESERIALIZER);

	static final Function<X509Certificate, ProtoAttributeValue.Builder> X509CERTIFICATE_SERIALIZER //
			= X509CertificateConverter.SERIALIZER.andThen(BYTES_SERIALIZER);

	static final Function[] X509CERTIFICATE = { X509CERTIFICATE_SERIALIZER, X509CERTIFICATE_DESERIALIZER };

	static Function[] determine(ProtoAttributeValue value) {
		if (value.getStringCount() > 0) {
			return STRING;
		}

		else if (value.getIntegerCount() > 0) {
			return INTEGER;
		}

		else if (value.getBytesCount() > 0) {
			return BYTES;
		}
		throw new IllegalArgumentException("Unknown value type");
	}

	static Function[] determine(Object value) {

		if (value instanceof String) {
			return STRING;
		}

		else if (value instanceof Boolean) {
			return BOOLEAN;
		}

		else if (value instanceof Integer) {
			return INTEGER;
		}

		else if (value instanceof byte[]) {
			return BYTES;
		}

		else if (value instanceof Long) {
			return LONG;
		}

		else if (value instanceof Double) {
			return DOUBLE;
		}

		else if (value instanceof X509Certificate) {
			return X509CERTIFICATE;
		}

		else if (value instanceof InstanceType) {
			return INSTANCETYPE;
		}

		else if (value instanceof InstanceFlavor) {
			return INSTANCEFLAVOR;
		}

		else if (value instanceof OsType) {
			return OSTYPE;
		}

		else if (value instanceof List) {
			var list = ((List<?>) value);
			if (list.size() == 0)
				throw new IllegalArgumentException("List cannot be empty");

			// Take an element from the list for type discovery
			var item = list.get(0);

			if (item instanceof String) {
				return STRING_LIST;
			}

			else if (item instanceof Boolean) {
				return BOOLEAN_LIST;
			}

			else if (item instanceof Long) {
				return LONG_LIST;
			}

			else if (item instanceof Double) {
				return DOUBLE_LIST;
			}

			else if (item instanceof Integer) {
				return INTEGER_LIST;
			}

			else {
				throw new IllegalArgumentException("Unknown list type: " + item.toString());
			}
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.toString());
		}
	}

	/**
	 * Get the value.
	 *
	 * @return The value
	 */
	public T get();

	/**
	 * Get the value's timestamp.
	 *
	 * @return The timestamp
	 */
	public long timestamp();

}
