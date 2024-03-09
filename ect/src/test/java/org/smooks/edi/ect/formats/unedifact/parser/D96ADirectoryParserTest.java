/*-
 * ========================LICENSE_START=================================
 * smooks-ect
 * %%
 * Copyright (C) 2020 - 2021 Smooks
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
package org.smooks.edi.ect.formats.unedifact.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.model.internal.Component;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class D96ADirectoryParserTest {
    @Test
    @DisplayName("should extract D96A code list values correctly")
    public void checkThatD96ACodeListsAreReadCorrectly() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/d96a.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        DirectoryParser d96AReader = new D96ADirectoryParser(zipInputStream, false, false);

        Map<String, byte[]> definitionFiles = d96AReader.getDefinitionFiles();
        assertFalse(definitionFiles.get("uncl") == null || definitionFiles.get("uncl").length == 0);

        final Component component5125 = getSimpleDataElement(d96AReader, "5125");
        assertEquals(9, component5125.getCodeList().getCodes().size());

        final Component component6063 = getSimpleDataElement(d96AReader, "6063");
        assertEquals(180, component6063.getCodeList().getCodes().size());
    }

    @Test
    @DisplayName("should parse components correctly")
    public void checkThatD96AComponentsAreCorrectlyParsed() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/d96a.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        DirectoryParser d96AReader = new D96ADirectoryParser(zipInputStream, false, true);

        final Field component = getComplexElement(d96AReader, "C770");
        assertEquals("9424", component.getComponents().get(0).getNodeTypeRef());
    }

    private Field getComplexElement(final DirectoryParser reader, final String componentName) throws IOException {
        Edimap edimap = UnEdifactDefinitionReader.parse(reader);
        for (final Field field : edimap.getCompositeDataElements()) {
            if (componentName.equals(field.getNodeTypeRef())) {
                return field;
            }
        }
        return null;
    }

    private Component getSimpleDataElement(final DirectoryParser reader, final String componentName) throws IOException {
        Edimap edimap = UnEdifactDefinitionReader.parse(reader);
        for (final Component component : edimap.getSimpleDataElements()) {
            if (componentName.equals(component.getNodeTypeRef())) {
                return component;
            }
        }
        return null;
    }
}