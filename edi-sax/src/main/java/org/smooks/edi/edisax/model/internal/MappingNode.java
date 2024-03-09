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

import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.edi.edisax.util.IllegalNameException;

public class MappingNode {

    private String name;
    private String namespace;
    private String documentation;

    public static final String INDEXED_NODE_SEPARATOR = "_-_-";

    private String xmltag;
    private String nodeTypeRef;
    private MappingNode parent;

    public MappingNode() {
    }

    public MappingNode(String xmltag, String namespace) {
        this.xmltag = xmltag;
        this.namespace = namespace;
    }

    public String getXmltag() {
        return xmltag;
    }

    public void setXmltag(String value) {
        this.xmltag = value;
    }

    public String getNodeTypeRef() {
        return nodeTypeRef;
    }

    public void setNodeTypeRef(String nodeTypeRef) {
        this.nodeTypeRef = nodeTypeRef;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public MappingNode getParent() {
        return parent;
    }

    public void setParent(MappingNode parent) {
        this.parent = parent;
    }

    public String getJavaName() throws IllegalNameException {
        String javaName = xmltag.replace(INDEXED_NODE_SEPARATOR, "_");

        if (name != null && name.trim().length() > 0) {
            javaName += "_" + EDIUtils.encodeClassName(name);
        }

        return javaName;
    }

    public String getXmlName() throws IllegalNameException {
        return getJavaName().replace("_", "-");
    }

    public void setName(String name) {
        if (name != null) {
            name = name.trim();
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
