//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance;

import static org.s7s.core.instance.state.STStore.STStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.foundation.S7SEnvironmentVariable;
import org.s7s.core.foundation.S7SSystemProperty;
import org.s7s.core.instance.state.STStore;
import org.s7s.core.instance.state.oid.Oid;

public class RuntimeVariable<T> implements Supplier<T> {

	private static final Logger log = LoggerFactory.getLogger(RuntimeVariable.class);

	private RuntimeVariableConfig<T> cfg;

	private RuntimeVariable(RuntimeVariableConfig<T> cfg) {
		this.cfg = cfg;
	}

	public static class RuntimeVariableConfig<E> {
		public Class<E> type;
		public Oid primary;
		public S7SSystemProperty secondary;
		public S7SEnvironmentVariable tertiary;
		public Supplier<E> defaultValue;
		public Predicate<E> validator;

		public RuntimeVariableConfig(Consumer<RuntimeVariableConfig<E>> configurator) {
			configurator.accept(this);
		}
	}

	public static <T> RuntimeVariable<T> of(Consumer<RuntimeVariableConfig<T>> configurator) {
		return new RuntimeVariable<>(new RuntimeVariableConfig<T>(configurator));
	}

	@Override
	public T get() {

		if (cfg.primary != null) {
			var attribute = STStore.attribute(cfg.primary);
			if (attribute.isPresent()) {
				return (T) attribute.get();
			}
		}
		if (cfg.secondary != null) {
			if (cfg.secondary.value().isPresent()) {
				try {
					T v = parseValue(cfg.secondary.value().get());
					if (v != null && (cfg.validator == null || cfg.validator.test(v))) {
						return v;
					}
				} catch (Exception e) {
					log.debug("Failed to parse system property", e);
				}
			}
		}
		if (cfg.tertiary != null) {
			if (cfg.tertiary.value().isPresent()) {
				try {
					T v = parseValue(cfg.tertiary.value().get());
					if (v != null && (cfg.validator == null || cfg.validator.test(v))) {
						return v;
					}
				} catch (Exception e) {
					log.debug("Failed to parse environment variable", e);
				}
			}
		}
		if (cfg.defaultValue != null) {
			try {
				T v = cfg.defaultValue.get();
				if (v != null && (cfg.validator == null || cfg.validator.test(v))) {
					return v;
				}
			} catch (Exception e) {
				log.debug("Failed to parse default value", e);
			}
		}

		return null;
	}

	private T parseValue(String value) throws Exception {

		if (cfg.type == String.class) {
			return (T) (String) value;
		}

		else if (cfg.type == String[].class) {
			return (T) (String[]) value.split(",");
		}

		else if (cfg.type == Integer.class) {
			return (T) (Integer) Integer.parseInt(value);
		}

		else if (cfg.type == Boolean.class) {
			return (T) (Boolean) Boolean.parseBoolean(value);
		}

		else if (cfg.type == Path.class) {
			return (T) Paths.get(value);
		}

		else {
			log.error("Unknown type: {}", cfg.type);
			return null;
		}
	}
}
