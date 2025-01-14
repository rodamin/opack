/*
 * Copyright (C) 2021 REALTIMETECH All Rights Reserved
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realtimetech.opack.bake;

import com.realtimetech.opack.transformer.Transformer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class BakedType {
    public static class Property {
        final Field field;
        final String name;
        final Class<?> type;

        final Transformer transformer;
        final Class<?> explicitType;

        public Property(@NotNull Field field, Transformer transformer, Class<?> explicitType) {
            this.field = field;
            this.name = this.field.getName();
            this.type = explicitType == null ? this.field.getType() : explicitType;

            this.transformer = transformer;
            this.explicitType = explicitType;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }

        public Field getField() {
            return field;
        }

        public Transformer getTransformer() {
            return transformer;
        }

        public Class<?> getExplicitType() {
            return explicitType;
        }

        /**
         * Sets the field of the object to a specified value.
         *
         * @param object the object whose field should be modified
         * @param value  the new value for the field of object being modified
         * @throws IllegalAccessException if this Field object is enforcing Java language access control and the underlying field is either inaccessible or final
         */
        public void set(Object object, Object value) throws IllegalAccessException {
            if (!this.field.canAccess(object)) {
                this.field.setAccessible(true);
            }
            this.field.set(object, value);
        }

        /**
         * Returns the field value extracted from the object.
         *
         * @param object the object to extract the field value
         * @return field value
         * @throws IllegalAccessException if this Field object is enforcing Java language access control and the underlying field is inaccessible.
         */
        public Object get(Object object) throws IllegalAccessException {
            if (!this.field.canAccess(object)) {
                this.field.setAccessible(true);
            }
            return this.field.get(object);
        }
    }

    final Class<?> type;
    final Transformer[] transformers;
    final Property[] fields;

    public BakedType(Class<?> type, Transformer[] transformers, Property[] fields) {
        this.type = type;
        this.transformers = transformers;
        this.fields = fields;
    }

    public Class<?> getType() {
        return type;
    }

    public Transformer[] getTransformers() {
        return transformers;
    }

    public Property[] getFields() {
        return fields;
    }
}