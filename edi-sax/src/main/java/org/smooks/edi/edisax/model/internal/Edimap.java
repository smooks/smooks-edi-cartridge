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

import org.smooks.edi.edisax.util.EdimapWriter;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Edimap {

    private URI src;
    private List<Import> imports;
    private Description description;
    private Delimiters delimiters;
    private SegmentGroup segments;
    private Boolean ignoreUnmappedSegments;
    private List<Component> simpleDataElements;
    private List<Field> compositeDataElements;

    public Edimap() {
    }

    public Edimap(URI src) {
        this.src = src;
    }

    public URI getSrc() {
        return src;
    }

    public List<Import> getImports() {
        if (imports == null) {
            imports = new ArrayList<Import>();
        }
        return this.imports;
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description value) {
        this.description = value;
    }

    public Delimiters getDelimiters() {
        return delimiters;
    }

    public void setDelimiters(Delimiters value) {
        this.delimiters = value;
    }

    public void setIgnoreUnmappedSegments(Boolean value) {
        this.ignoreUnmappedSegments = value;
    }

    public boolean isIgnoreUnmappedSegments() {
        return ignoreUnmappedSegments != null && ignoreUnmappedSegments;
    }

    public SegmentGroup getSegments() {
        return segments;
    }

    public void setSegments(SegmentGroup value) {
        this.segments = value;
    }

    public void write(Writer writer) throws IOException {
        EdimapWriter.write(this, writer);
    }

    public List<Component> getSimpleDataElements() {
        return simpleDataElements;
    }

    public void setSimpleDataElements(List<Component> simpleDataElements) {
        this.simpleDataElements = simpleDataElements;
    }

    public List<Field> getCompositeDataElements() {
        return compositeDataElements;
    }

    public void setCompositeDataElements(List<Field> compositeDataElements) {
        this.compositeDataElements = compositeDataElements;
    }
}
