/*-
 * ========================LICENSE_START=================================
 * smooks-edg
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
package org.smooks.edi.edg.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.DecoratedCollection;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Template {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected final String version;
    protected final DefaultMustacheFactory mustacheFactory;
    protected Map<String, Object> scope = new HashMap<>();

    public Template(final String version) {
        mustacheFactory = new DefaultMustacheFactory() {
            @Override
            public void encode(String value, Writer writer) {
                try {
                    writer.append(value);
                } catch (IOException e) {
                    throw new MustacheException(e);
                }
            }
        };
        mustacheFactory.setObjectHandler(new ReflectionObjectHandler() {
            @Override
            public Object coerce(Object object) {
                if (object instanceof Collection) {
                    return new DecoratedCollection((Collection) object);
                }
                return super.coerce(object);
            }
        });
        this.version = version;
        scope.put("version", version);
    }

    public Map<String, Object> getScope() {
        return scope;
    }

    public abstract String getName();

    public String materialise() {
        final Mustache segmentsMustache = mustacheFactory.compile(getName());
        final StringWriter stringWriter = new StringWriter();
        segmentsMustache.execute(stringWriter, getScope());

        return stringWriter.toString();
    }
}
