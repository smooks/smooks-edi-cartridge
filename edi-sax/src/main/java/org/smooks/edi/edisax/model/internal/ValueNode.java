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

import org.smooks.config.Configurable;
import org.smooks.converter.TypeConverter;
import org.smooks.converter.TypeConverterException;
import org.smooks.converter.TypeConverterFactoryLoader;
import org.smooks.converter.factory.TypeConverterFactory;
import org.smooks.converter.factory.system.StringConverterFactory;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ValueNode.
 *
 * @author bardl
 */
public class ValueNode extends MappingNode {

    private static final Set<TypeConverterFactory<?, ?>> TYPE_CONVERTER_FACTORIES = new TypeConverterFactoryLoader().load();

    private String dataType;
    private List<Map.Entry<String,String>> parameters;
    private Integer minLength;
    private Integer maxLength;
    private Class<?> typeClass;
    private Properties decodeParams;
    private TypeConverter<String, ?> typeConverter;

    public ValueNode() {
	}
    
	public ValueNode(String xmltag, String namespace) {
		super(xmltag, namespace);
		minLength = 0;
		maxLength = 1;
	}

	public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
        if (dataType != null) {
            if (dataType.equals("Custom")) {
                typeConverter = new CustomTypeConverter();
            } else {
                Set<TypeConverterFactory<?, ?>> typeConverterFactories = TYPE_CONVERTER_FACTORIES.stream().filter(i -> i.getClass().isAnnotationPresent(Resource.class) && dataType.equals(i.getClass().getAnnotation(Resource.class).name())).collect(Collectors.toSet());
                if (typeConverterFactories.isEmpty()) {
                    typeConverter = new StringConverterFactory().createTypeConverter();
                    typeClass = String.class;
                } else {
                    TypeConverterFactory typeConverterFactory = typeConverterFactories.toArray(new TypeConverterFactory[]{})[0];
                    typeConverter = (TypeConverter<String, ?>) typeConverterFactory.createTypeConverter();
                    typeClass = (Class<?>) typeConverterFactory.getTypeConverterDescriptor().getTargetType();
                }
            }
        }
    }

    public TypeConverter<String, ?> getTypeConverter() {
        return typeConverter;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    public List<Map.Entry<String,String>> getTypeParameters() {
        return parameters;
    }

    public void setDataTypeParameters(List<Map.Entry<String,String>> parameters) {
        this.parameters = parameters;

        if (typeConverter instanceof Configurable) {
            if(typeConverter  == null) {
                throw new IllegalStateException("Illegal call to set parameters before 'dataType' has been configured on the " + getClass().getName());
            }

            decodeParams = new Properties();
            if(parameters != null) {
                for (Map.Entry<String,String> entry : parameters) {
                    decodeParams.setProperty(entry.getKey(), entry.getValue());
                }
            }
            ((Configurable) typeConverter).setConfiguration(decodeParams);
        }
    }

    public String getDataTypeParametersString() {
        if(parameters == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for(Map.Entry<String,String> parameter : parameters) {
            if(builder.length() > 0) {
                builder.append(";");
            }
            builder.append(parameter.getKey());
            builder.append("=");
            builder.append(parameter.getValue());
        }

        return builder.toString();
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public void isValidForType(String value) throws TypeConverterException {
        typeConverter.convert(value);
    }
}
