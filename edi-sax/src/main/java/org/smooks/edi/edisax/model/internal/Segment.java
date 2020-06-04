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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Segment extends SegmentGroup implements ContainerNode {

    private List<Field> fields;
    private String segcode;
    private Pattern segcodePattern;
    private Boolean truncatable;
    private Boolean ignoreUnmappedFields;
    private String description;
    private String importXmlTag;

    public List<Field> getFields() {
        if (fields == null) {
            fields = new ArrayList<Field>();
        }
        return this.fields;
    }
    
    public Segment addField(Field field) {
    	getFields().add(field);
    	return this;
    }

    public String getSegcode() {
        return segcode;
    }

    public void setSegcode(String value) {
        this.segcode = value;
        segcodePattern = Pattern.compile("^" + segcode, Pattern.DOTALL);
    }

    public Pattern getSegcodePattern() {
        return segcodePattern;
    }

    public boolean isTruncatable() {
        return truncatable != null && truncatable;
    }

    public void setTruncatable(Boolean value) {
        this.truncatable = value;
    }
    
    public void setIgnoreUnmappedFields(Boolean value) {
    	this.ignoreUnmappedFields = value;
    }
    
    public boolean isIgnoreUnmappedFields() {
    	return ignoreUnmappedFields != null && ignoreUnmappedFields;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImportXmlTag() {
        return importXmlTag;
    }

    public void setImportXmlTag(String importXmlTag) {
        this.importXmlTag = importXmlTag;
    }
}
