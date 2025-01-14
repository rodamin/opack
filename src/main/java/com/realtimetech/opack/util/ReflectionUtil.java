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

package com.realtimetech.opack.util;

import org.jetbrains.annotations.NotNull;

import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtil {
    private static abstract class Allocator {
        /**
         * Create new instance for specific class.
         *
         * @param typeClass the class to create
         * @return created instance
         * @throws InvocationTargetException if exception occurs in invoked underlying method
         * @throws IllegalAccessException    if this Method object is enforcing Java language access control and the underlying method is inaccessible
         */
        abstract <T> T allocate(Class<T> typeClass) throws InvocationTargetException, IllegalAccessException;
    }

    static final Map<Class<?>, Class<?>> PRIMITIVES_WRAPPERS_MAP = Map.of(
            boolean.class, Boolean.class,

            byte.class, Byte.class,
            char.class, Character.class,

            short.class, Short.class,

            int.class, Integer.class,
            float.class, Float.class,

            double.class, Double.class,
            long.class, Long.class
    );

    static final Map<Class<?>, Class<?>> WRAPPERS_PRIMITIVES_MAP = Collections.unmodifiableMap(new HashMap<>() {
        {
            for (Class<?> primitiveType : PRIMITIVES_WRAPPERS_MAP.keySet()) {
                Class<?> wrapperType = PRIMITIVES_WRAPPERS_MAP.get(primitiveType);

                put(wrapperType, primitiveType);
            }
        }
    });

    static final Allocator ALLOCATOR;

    static {
        ALLOCATOR = ReflectionUtil.createAvailableAllocator();
        if (ALLOCATOR == null) {
            throw new ExceptionInInitializerError("This virtual machine doesn't support unsafe allocator.");
        }
    }

    /**
     * Create new unsafe allocator for JVM, DalvikVM.
     *
     * @return created unsafe allocator
     */
    static Allocator createAvailableAllocator() {
        // for JVM
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);

            final Object unsafeObject = unsafeField.get(null);
            final Method allocateMethod = unsafeClass.getMethod("allocateInstance", Class.class);
            return new Allocator() {
                @Override
                public <T> T allocate(Class<T> typeClass) throws InvocationTargetException, IllegalAccessException {
                    return typeClass.cast(allocateMethod.invoke(unsafeObject, typeClass));
                }
            };
        } catch (Exception exception) {
        }

        // for DalvikVM (> 2.3)
        try {
            final Method getterMethod = ObjectStreamClass.class.getDeclaredMethod("getConstructorId", Class.class);
            getterMethod.setAccessible(true);

            final int constructorId = (Integer) getterMethod.invoke(null, Object.class);
            final Method allocateMethod = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class, int.class);
            allocateMethod.setAccessible(true);
            return new Allocator() {
                @Override
                public <T> T allocate(Class<T> typeClass) throws InvocationTargetException, IllegalAccessException {
                    return typeClass.cast(allocateMethod.invoke(null, typeClass, constructorId));
                }
            };
        } catch (Exception exception) {
        }

        // for DalvikVM (< 2.3)
        try {
            final Method allocateMethod = ObjectInputStream.class.getDeclaredMethod("newInstance", Class.class, Class.class);
            allocateMethod.setAccessible(true);
            return new Allocator() {
                @Override
                public <T> T allocate(Class<T> typeClass) throws InvocationTargetException, IllegalAccessException {
                    return typeClass.cast(allocateMethod.invoke(null, typeClass, Object.class));
                }
            };
        } catch (Exception exception) {
        }

        return null;
    }

    /**
     * Casts an object to the specific class.
     *
     * @param type   the class to cast
     * @param object the object to be cast
     * @return the object after casting
     */
    public static Object cast(Class<?> type, Object object) {
        if (ReflectionUtil.isPrimitiveType(type)) {
            type = ReflectionUtil.convertPrimitiveTypeToWrapperType(type);
        }
        if (ReflectionUtil.isWrapperType(type)) {
            Class<?> objectType = object.getClass();

            if (objectType == Long.class) {
                Long value = (Long) object;

                if (type == Integer.class) {
                    return value.intValue();
                } else if (type == Short.class) {
                    return value.shortValue();
                } else if (type == Character.class) {
                    return (char) value.intValue();
                } else if (type == Byte.class) {
                    return value.byteValue();
                }
            } else if (objectType == Integer.class) {
                Integer value = (Integer) object;

                if (type == Short.class) {
                    return value.shortValue();
                } else if (type == Character.class) {
                    return (char) value.intValue();
                } else if (type == Byte.class) {
                    return value.byteValue();
                }
            } else if (objectType == Short.class) {
                Short value = (Short) object;

                if (type == Character.class) {
                    return (char) value.intValue();
                } else if (type == Byte.class) {
                    return value.byteValue();
                }
            } else if (objectType == Character.class) {
                Character value = (Character) object;

                if (type == Byte.class) {
                    return (byte) value.charValue();
                }
            } else if (objectType == Double.class) {
                Double value = (Double) object;

                if (type == Float.class) {
                    return value.floatValue();
                }
            }

            return object;
        }

        return type.cast(object);
    }

    /**
     * Returns the value of the indexed component in the specified array object.
     *
     * @param array the array object
     * @param index the index
     * @return the value of the indexed component in the specified array
     */
    public static Object getArrayItem(Object array, int index) {
        Class<?> c = array.getClass();

        if (int[].class == c) {
            return ((int[]) array)[index];
        } else if (float[].class == c) {
            return ((float[]) array)[index];
        } else if (boolean[].class == c) {
            return ((boolean[]) array)[index];
        } else if (char[].class == c) {
            return ((char[]) array)[index];
        } else if (double[].class == c) {
            return ((double[]) array)[index];
        } else if (long[].class == c) {
            return ((long[]) array)[index];
        } else if (short[].class == c) {
            return ((short[]) array)[index];
        } else if (byte[].class == c) {
            return ((byte[]) array)[index];
        }

        return ((Object[]) array)[index];
    }

    /**
     * Sets the value of the indexed component of the specified array object to the specified new value.
     *
     * @param array the array object
     * @param index the index
     * @param value the new value of the indexed component
     */
    public static void setArrayItem(Object array, int index, Object value) {
        Class<?> c = array.getClass();

        if (int[].class == c) {
            ((int[]) array)[index] = (int) value;
        } else if (float[].class == c) {
            ((float[]) array)[index] = (float) value;
        } else if (boolean[].class == c) {
            ((boolean[]) array)[index] = (boolean) value;
        } else if (char[].class == c) {
            ((char[]) array)[index] = (char) value;
        } else if (double[].class == c) {
            ((double[]) array)[index] = (double) value;
        } else if (long[].class == c) {
            ((long[]) array)[index] = (long) value;
        } else if (short[].class == c) {
            ((short[]) array)[index] = (short) value;
        } else if (byte[].class == c) {
            ((byte[]) array)[index] = (byte) value;
        } else {
            ((Object[]) array)[index] = value;
        }
    }

    /**
     * Clones the array object.
     *
     * @param array the object to clone
     * @return cloned array object
     * @throws IllegalArgumentException if the object is not array object
     */
    public static Object cloneArray(Object array) {
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException(array + " is not array object.");
        }

        int length = Array.getLength(array);
        Object newArray = Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, newArray, 0, length);

        return newArray;
    }

    /**
     * Add accessible fields of the target Class to the field List.
     *
     * @param fieldList the field list to be added
     * @param type      the target class
     */
    static void addAccessibleFields(List<Field> fieldList, Class<?> type) {
        Class<?> superClass = type.getSuperclass();

        if (superClass != null && superClass != Object.class) {
            ReflectionUtil.addAccessibleFields(fieldList, type.getSuperclass());
        }

        for (Field field : type.getDeclaredFields()) {
            if (!fieldList.contains(field) && !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                fieldList.add(field);
            }
        }
    }

    /**
     * Returns accessible fields of target class.
     *
     * @param type the target class
     * @return accessible fields
     */
    public static Field[] getAccessibleFields(Class<?> type) {
        List<Field> fields = new LinkedList<>();
        ReflectionUtil.addAccessibleFields(fields, type);

        return fields.toArray(new Field[0]);
    }

    /**
     * Returns the dimension of the array through the class object of the array object.
     *
     * @param arrayType the class of the target array object
     * @return dimension
     * @throws IllegalArgumentException if the class is not the class of the array object
     */
    public static int getArrayDimension(Class<?> arrayType) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException(arrayType + " is not array class.");
        }

        int count = 0;

        while (true) {
            Class<?> componentClass = arrayType.getComponentType();

            if (componentClass == null) {
                break;
            } else {
                count++;
                arrayType = componentClass;
            }
        }

        return count;
    }

    /**
     * Returns the underlying component type of the array through the class object of the array object.
     *
     * @param arrayType the class of the array object
     * @return component type
     */
    public static Class<?> getArrayLastComponentType(Class<?> arrayType) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException(arrayType + " is not array class.");
        }

        Class<?> lastClass = arrayType;

        while (true) {
            Class<?> componentClass = lastClass.getComponentType();

            if (componentClass == null) {
                break;
            } else {
                lastClass = componentClass;
            }
        }

        return lastClass;
    }

    /**
     * @param type the target class
     * @return whether the target class is wrapper class
     */
    public static boolean isWrapperType(Class<?> type) {
        return WRAPPERS_PRIMITIVES_MAP.containsKey(type);
    }

    /**
     * Returns the primitive class corresponding to the wrapper class.
     *
     * @param type the wrapper class
     * @return primitive class
     * @throws IllegalArgumentException if target class is not wrapper class
     */
    public static @NotNull Class<?> convertWrapperClassToPrimitiveClass(@NotNull Class<?> type) {
        Class<?> primitiveClass = WRAPPERS_PRIMITIVES_MAP.getOrDefault(type, null);

        if (primitiveClass == null) {
            throw new IllegalArgumentException(type + " is not wrapper class.");
        }

        return primitiveClass;
    }

    /**
     * @param type the target class
     * @return whether the target class is primitive class
     */
    public static boolean isPrimitiveType(Class<?> type) {
        return PRIMITIVES_WRAPPERS_MAP.containsKey(type);
    }

    /**
     * Returns the wrapper class corresponding to the primitive class.
     *
     * @param type the primitive class
     * @return wrapper class
     * @throws IllegalArgumentException if target class is not primitive class
     */
    public static @NotNull Class<?> convertPrimitiveTypeToWrapperType(@NotNull Class<?> type) {
        Class<?> wrapperClass = PRIMITIVES_WRAPPERS_MAP.getOrDefault(type, null);

        if (wrapperClass == null) {
            throw new IllegalArgumentException(type + " is not primitive class.");
        }

        return wrapperClass;
    }

    /**
     * Returns whether casting from the source class to the destination class is possible.
     *
     * @param fromType the source class
     * @param toType   the destination class
     * @return whether casting is possible
     */
    public static boolean checkCastable(@NotNull Class<?> fromType, @NotNull Class<?> toType) {
        if (fromType.isPrimitive()) {
            fromType = ReflectionUtil.convertPrimitiveTypeToWrapperType(fromType);
        }

        if (toType.isPrimitive()) {
            toType = ReflectionUtil.convertPrimitiveTypeToWrapperType(toType);
        }

        return toType.isAssignableFrom(fromType);
    }

    /**
     * Create a new instance of the class with unsafe allocator.
     *
     * @param instanceType the class to create instance
     * @return created instance
     * @throws InvocationTargetException if exception occurs in invoked underlying method
     * @throws IllegalAccessException    if the method object is enforcing Java language access control and the underlying method is inaccessible
     * @throws InstantiationException    if the class object represents an abstract class, an interface
     */
    public static <T> @NotNull T createInstanceUnsafe(@NotNull Class<T> instanceType) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        int modifiers = instanceType.getModifiers();

        if (Modifier.isInterface(modifiers)) {
            throw new InstantiationException("Interface can't be instantiated, got " + instanceType.getSimpleName() + " interface.");
        }

        if (Modifier.isAbstract(modifiers)) {
            throw new InstantiationException("Abstract class can't be instantiated, got " + instanceType.getSimpleName() + " abstract class.");
        }

        return ReflectionUtil.ALLOCATOR.allocate(instanceType);
    }

    /**
     * Create a new instance of the class through constructor.
     *
     * @param instanceType the class to create instance
     * @param objects      the argument of the constructor
     * @return created instance
     * @throws IllegalArgumentException  if the class doesn't have matched constructor
     * @throws InvocationTargetException if exception occurs in invoked underlying method
     * @throws IllegalAccessException    if the method object is enforcing Java language access control and the underlying method is inaccessible
     * @throws InstantiationException    if the class object represents an abstract class, an interface
     */
    public static <T> @NotNull T createInstance(@NotNull Class<T> instanceType, Object @NotNull ... objects) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        int modifiers = instanceType.getModifiers();

        if (Modifier.isInterface(modifiers)) {
            throw new InstantiationException("Interface can't be instantiated, got " + instanceType.getSimpleName() + " interface.");
        }

        if (Modifier.isAbstract(modifiers)) {
            throw new InstantiationException("Abstract class can't be instantiated, got " + instanceType.getSimpleName() + " abstract class.");
        }

        Constructor<?>[] constructors = instanceType.getDeclaredConstructors();
        Class<?>[] classes = new Class[objects.length];

        for (int index = 0; index < objects.length; index++) {
            classes[index] = objects[index].getClass();
        }

        for (Constructor<?> constructor : constructors) {
            if (constructor.getDeclaringClass() == instanceType) {
                int parameterCount = constructor.getParameterCount();
                Class<?>[] parameterTypes = constructor.getParameterTypes();

                if (parameterCount == objects.length) {
                    boolean matched = true;

                    for (int index = 0; index < parameterCount; index++) {
                        if (!ReflectionUtil.checkCastable(classes[index], parameterTypes[index])) {
                            matched = false;
                            break;
                        }
                    }

                    if (matched) {
                        constructor.setAccessible(true);

                        return instanceType.cast(constructor.newInstance(objects));
                    }
                }
            }
        }

        throw new IllegalArgumentException(instanceType.getSimpleName() + " class doesn't have matched constructor.");
    }
}