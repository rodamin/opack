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

package com.realtimetech.opack.value;

import com.realtimetech.opack.util.ReflectionUtil;

public interface OpackValue {
    /**
     * Assert the specific class is allowed type in opack value.
     *
     * @param type the target class
     * @throws IllegalArgumentException if the class is not allowed type
     */
    public static void assertAllowType(Class<?> type) {
        if (!OpackValue.isAllowType(type)) {
            throw new IllegalArgumentException(type.getName() + " is not allowed type, allow only primitive type or String or OpackValues or null");
        }
    }

    /**
     * Return whether the specific class is allowed type in opack value.
     *
     * @param type the target class
     * @return whether class is allowed type
     */
    public static boolean isAllowType(Class<?> type) {
        return ReflectionUtil.isWrapperType(type) ||
                ReflectionUtil.isPrimitiveType(type) ||
                (type == String.class) ||
                (AbstractOpackValue.class.isAssignableFrom(type));
    }

    /**
     * Clone this opack value.
     *
     * @return cloned opack value
     */
    public OpackValue clone();
}