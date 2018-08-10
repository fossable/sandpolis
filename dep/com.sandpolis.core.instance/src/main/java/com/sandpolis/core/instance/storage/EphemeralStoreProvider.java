/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.instance.storage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import javax.persistence.Id;

/**
 * A superclass for {@link StoreProvider}s that only store data temporarily.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class EphemeralStoreProvider<E> {

	/**
	 * The type managed by the store.
	 */
	protected final Class<E> cls;

	/**
	 * A handle on the getter for the primary ID.
	 */
	protected MethodHandle getId;

	protected EphemeralStoreProvider(Class<E> cls) {
		this.cls = cls;

		try {
			for (Field field : cls.getDeclaredFields()) {
				if (field.getAnnotation(Id.class) != null) {
					getId = MethodHandles.lookup().findGetter(cls, field.getName(), field.getType());
					break;
				}
			}
		} catch (SecurityException | NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Get a field getter for {@link #cls}.
	 * 
	 * @param field The field name
	 * @return A handle on the field getter
	 */
	protected MethodHandle fieldGetter(String field)
			throws NoSuchFieldException, IllegalAccessException, SecurityException {
		return MethodHandles.lookup().findGetter(cls, field, cls.getField(field).getType());
	}

}
