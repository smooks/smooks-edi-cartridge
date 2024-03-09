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

import org.smooks.assertion.AssertArgument;

public class Description {

    private String name;
    private String version;
    private String namespace;

    public String getName() {
        return name;
    }

    public Description setName(String name) {
        AssertArgument.isNotNull(name, "name");
        this.name = name.trim();
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Description setVersion(String version) {
        AssertArgument.isNotNull(version, "version");
        this.version = version.trim();
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public Description setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    @Override
    public Object clone() {
        return new Description().setName(name).setVersion(version).setNamespace(namespace);
    }

    @Override
    public boolean equals(Object obj) {
        assertInitialized();

        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        if (obj instanceof Description) {
            Description description = (Description) obj;
            return description.name.equals(name) && description.version.equals(version);
        } else if (obj instanceof String) {
            // Just comparing the names and ignoring the version...
            return obj.equals(name);
        }

        return false;
    }

    @Override
    public int hashCode() {
        assertInitialized();
        return (name + "#" + version).hashCode();
    }

    private void assertInitialized() {
        if (name == null || version == null) {
            throw new IllegalStateException("Description 'name' and/or 'version' properties are not initialized.");
        }
    }
}
