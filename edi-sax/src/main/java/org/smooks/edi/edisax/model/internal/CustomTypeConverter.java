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
package org.smooks.edi.edisax.model.internal;

import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.config.Configurable;
import org.smooks.converter.TypeConverter;
import org.smooks.converter.factory.TypeConverterFactory;

import java.util.Properties;

public class CustomTypeConverter implements TypeConverter<String, Object>, Configurable {
    public static final String CLASS_PROPERTY_NAME = "typeConverterClass";

    private Properties properties;
    private TypeConverter<? super String, ?> delegateTypeConverter;

    @Override
    public Object convert(String value) {
        return delegateTypeConverter.convert(value);
    }

    @Override
    public void setConfiguration(Properties properties) throws SmooksConfigurationException {
        String className = properties.getProperty(CLASS_PROPERTY_NAME);

        if (className == null) {
            throw new SmooksConfigurationException("Mandatory property '" + CLASS_PROPERTY_NAME + "' not specified.");
        }

        try {
            Class<TypeConverterFactory<String, Object>> typeConverterFactoryClass = (Class<TypeConverterFactory<String, Object>>) Class.forName(className);
            delegateTypeConverter = typeConverterFactoryClass.newInstance().createTypeConverter();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new SmooksConfigurationException(e);
        }
        
        //Set configuration in delegateDecoder.
        if (delegateTypeConverter instanceof Configurable) {
            ((Configurable) delegateTypeConverter).setConfiguration(properties);
        }

        this.properties = properties;
    }

    @Override
    public Properties getConfiguration() {
        return properties;
    }
}
