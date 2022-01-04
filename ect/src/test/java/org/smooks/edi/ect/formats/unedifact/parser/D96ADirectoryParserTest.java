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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.formats.unedifact.parser.D96ADirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class D96ADirectoryParserTest {
    private DirectoryParser d96AReader;

    @BeforeEach
    public void init() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/d96a.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        d96AReader = new D96ADirectoryParser(zipInputStream, false, false);
    }
    @Test
    @DisplayName("should extract legacy D96A code list values correctly")
    public void checkThatD96ACodeListsAreReadCorrectlyForLegacyD96A() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/d96a.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        DirectoryParser d96AReader = new D96ADirectoryParser(zipInputStream, false, false);

        Map<String, byte[]> definitionFiles = d96AReader.getDefinitionFiles();
        assertFalse(
                definitionFiles.get("uncl") == null || definitionFiles.get("uncl").length == 0);
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

    @Test
    public void checkCorrectParsingOfCodesD96A() throws IOException{
        Edimap edimap = UnEdifactDefinitionReader.parse(d96AReader);

        StringWriter stringWriter = new StringWriter();
        edimap.write(stringWriter);

        edimap.getSimpleDataElements().forEach(a -> {
            if (a.getCodeList() != null) {
                assertTrue(a.getCodeList().getCodes().size() > 0);
            }
            if (a.getNodeTypeRef().equals("1153")) {
                assertTrue(a.getCodeList().getCodes().contains("AAA"));
                assertTrue(a.getCodeList().getCodes().contains("AEK"));
                assertTrue(a.getCodeList().getCodes().contains("AEJ"));
                assertTrue(a.getCodeList().getCodes().contains("VN"));
                assertTrue(a.getCodeList().getCodes().contains("ON"));
                assertTrue(a.getCodeList().getCodes().contains("BT"));
                assertTrue(a.getCodeList().getCodes().contains("VA"));
            }
            if (a.getNodeTypeRef().equals("3139")) {
                assertTrue(a.getCodeList().getCodes().contains("IC"));
            }
            if (a.getNodeTypeRef().equals("3155")) {
                assertTrue(a.getCodeList().getCodes().contains("TE"));
            }
            if (a.getNodeTypeRef().equals("6311")) {
                assertTrue(a.getCodeList().getCodes().contains("AAY"));
            }
            if (a.getNodeTypeRef().equals("6313")) {
                assertTrue(a.getCodeList().getCodes().contains("HM"));
                assertTrue(a.getCodeList().getCodes().contains("LM"));
                assertTrue(a.getCodeList().getCodes().contains("WM"));
                assertTrue(a.getCodeList().getCodes().contains("GW"));
                assertTrue(a.getCodeList().getCodes().contains("LM"));
            }
            if (a.getNodeTypeRef().equals("7143")) {
                assertTrue(a.getCodeList().getCodes().contains("BP"));
                assertTrue(a.getCodeList().getCodes().contains("VP"));
            }
            if (a.getNodeTypeRef().equals("7077")) {
                assertTrue(a.getCodeList().getCodes().contains("F"));
            }
            if (a.getNodeTypeRef().equals("3035")) {
                assertTrue(a.getCodeList().getCodes().contains("BY"));
                assertTrue(a.getCodeList().getCodes().contains("SU"));
            }
            if (a.getNodeTypeRef().equals("5125")) {
                assertTrue(a.getCodeList().getCodes().contains("INF"));
            }
            if (a.getNodeTypeRef().equals("5387")) {
                assertTrue(a.getCodeList().getCodes().contains("NTP"));
            }
        });
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
}