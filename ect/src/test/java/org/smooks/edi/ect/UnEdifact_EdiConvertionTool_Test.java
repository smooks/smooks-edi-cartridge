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
package org.smooks.edi.ect;

import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.formats.unedifact.parser.D93ADirectoryParser;
import org.smooks.edi.ect.formats.unedifact.parser.UnEdifactDirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.EDIConfigDigester;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UnEdifact_EdiConvertionTool_Test {

    @Test
    public void test_D08A_longName() throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(getClass().getResourceAsStream("/d08a.zip"));
        File modelSetFile = new File("./target/D08A-mapping-model.zip");

        modelSetFile.delete();

        EdiConvertionTool.fromUnEdifactSpec(zipInputStream, new ZipOutputStream(new FileOutputStream(modelSetFile)), "org.smooks.edi.unedifact:d08a:1.0-SNAPSHOT", false);
    }

    @Test
    public void test_MILYN_475() throws IOException, EDIConfigurationException, SAXException {
        ZipInputStream zipInputStream = new ZipInputStream(getClass().getResourceAsStream("/d08a.zip"));

        DirectoryParser directoryParser = new UnEdifactDirectoryParser(zipInputStream, false, false);
        ByteArrayOutputStream serializedMap = new ByteArrayOutputStream();

        Edimap jupreq = directoryParser.getMappingModel("JUPREQ", UnEdifactDefinitionReader.parse(directoryParser));
        Writer writer = new OutputStreamWriter(serializedMap);

        jupreq.write(writer);

        EDIConfigDigester.digestConfig(new ByteArrayInputStream(serializedMap.toByteArray()));
    }

    @Test
    public void test_MILYN_476() throws IOException, EDIConfigurationException, SAXException {
        ZipInputStream zipInputStream = new ZipInputStream(getClass().getResourceAsStream("/d93a.zip"));
        DirectoryParser directoryParser = new D93ADirectoryParser(zipInputStream, false, false);
        ByteArrayOutputStream serializedMap = new ByteArrayOutputStream();

        Edimap jupreq = directoryParser.getMappingModel("INVOIC", UnEdifactDefinitionReader.parse(directoryParser));
        Writer writer = new OutputStreamWriter(serializedMap);

        jupreq.write(writer);

        EDIConfigDigester.digestConfig(new ByteArrayInputStream(serializedMap.toByteArray()));
    }
}
