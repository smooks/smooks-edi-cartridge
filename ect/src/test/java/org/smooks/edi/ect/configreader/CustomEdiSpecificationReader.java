/*-
 * ========================LICENSE_START=================================
 * smooks-ect
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
package org.smooks.edi.ect.configreader;

import org.smooks.edi.edisax.interchange.EdiDirectory;
import org.smooks.edi.ect.EdiSpecificationReader;
import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Description;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.smooks.edi.edisax.model.internal.SegmentGroup;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

public class CustomEdiSpecificationReader implements EdiSpecificationReader {
    public void initialize(InputStream inputStream, boolean useImport) throws IOException, EdiParseException {
    }

    public Set<String> getMessageNames() {
        return new HashSet<String>();
    }

    public Edimap getMappingModel(String messageName) throws IOException {
        return createEdimap();
    }

    public Properties getInterchangeProperties() {
        return new Properties();
    }

    public EdiDirectory getEdiDirectory(String... includeMessages) throws IOException {
        return null;
    }

    public Edimap getDefinitionModel() throws IOException {
        return createEdimap();
    }

    private Edimap createEdimap() {
        Edimap edimap = new Edimap();

        Description description = new Description();
        description.setName("Custom Config Reader");
        description.setVersion("1.0");
        edimap.setDescription(description);

        Delimiters delimiters = new Delimiters();
        delimiters.setSegment("'");
        delimiters.setField("+");
        delimiters.setComponent(":");
        delimiters.setSubComponent("^");
        delimiters.setEscape("?");
        edimap.setDelimiters(delimiters);

        SegmentGroup root = new SegmentGroup();
        root.setXmltag("Root");
        edimap.setSegments(root);

        return edimap;
    }
}
