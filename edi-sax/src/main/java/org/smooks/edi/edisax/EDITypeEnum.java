/*-
 * ========================LICENSE_START=================================
 * smooks-edi-sax
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.edi.edisax;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public enum EDITypeEnum {
    String("String", String.class),
    Numeric("Double", String.class),
    Decimal("Double", Double.class),
    Date("Date", Date.class),
    Time("Date", Date.class),
    Binary("Binary", String.class),
    Custom(null, null);

    public final static String CUSTOM_NAME = "Custom";

    private String typeAlias;
    private Class javaClass;

    EDITypeEnum(String typeAlias, Class javaClass) {
        this.typeAlias = typeAlias;
        this.javaClass = javaClass;
    }

    public String getTypeAlias() {
        return typeAlias;
    }

    public Class getJavaClass() {
        return javaClass;
    }

    private Properties getProperties(List<Map.Entry<String, String>> parameters) {
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : parameters) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        return properties;
    }

}
