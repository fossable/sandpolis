/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.instance;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Implementations of this class are "constant trees". They contain type chains
 * which resolve to a unique key. These trees should always have private
 * constructors and therefore never be instantiated. The root class of a
 * constant tree should conform to typical Java naming conventions, but all
 * nested classes should consist of lowercase letters only.<br>
 * 
 * Constant trees are commonly used to organize storage keys and
 * permissions.<br>
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class ConstantTree<E> {

	/**
	 * This optional annotation specifies that a field is a key in a
	 * {@link ConstantTree} and allows keys to clarify their associated type.
	 * 
	 * All fields in the hierarchy of a {@link ConstantTree} are keys whether this
	 * annotation is present or not.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	@Target({ FIELD })
	@Retention(value = RUNTIME)
	public @interface TreeKey {

		/**
		 * The field's associated type if it exists.
		 */
		public Class<?> type();
	}

	/**
	 * Lookup a tree constant by value.
	 * 
	 * @param _class
	 *            The {@link ConstantTree} subclass to search
	 * @param value
	 *            The value to search
	 * @return The constant's {@link String} representation
	 */
	protected static String lookup(Class<?> _class, Object value) {

		try {
			for (Field field : _class.getDeclaredFields()) {
				if (value.equals(field.get(null))) {
					return getFieldString(field);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// If unit tests are correct, this will never run
			throw new RuntimeException(e);
		}

		for (Class<?> sub : _class.getDeclaredClasses()) {
			String ret = lookup(sub, value);
			if (ret != null)
				return ret;
		}

		return null;
	}

	/**
	 * Get the {@link String} representation of a {@link Field} (key) in the tree.
	 * 
	 * @param field
	 *            The tree constant
	 * @return The constant's {@link String} representation
	 */
	public static String getFieldString(Field field) {
		if (field == null)
			throw new IllegalArgumentException();

		StringBuffer buffer = new StringBuffer(field.getName());

		Class<?> _class = field.getDeclaringClass();
		while (!ConstantTree.class.equals(_class.getSuperclass())) {
			buffer.insert(0, '.');
			buffer.insert(0, _class.getSimpleName());

			_class = _class.getDeclaringClass();
		}
		return buffer.toString();
	}

}
