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
package org.smooks.edi.ect.ecore;

import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.formats.unedifact.parser.UnEdifactDirectoryParser;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.archive.Archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class XSDExportTest {

    @Test
    public void testSchemaExport() throws Exception {
        String directory = "d03b";
        String pluginID = "org.smooks.edi.unedifact." + directory;
        String pathPrefix = pluginID.replace('.', '/');
        InputStream inputStream = getClass().getResourceAsStream("/" + directory + ".zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        DirectoryParser directoryParser = new UnEdifactDirectoryParser(zipInputStream, false, false);
        ECoreGenerator ecoreGen = new ECoreGenerator();
        Set<EPackage> packages = ecoreGen.generatePackages(directoryParser.getEdiDirectory(UnEdifactDefinitionReader.parse(directoryParser)));
        // To make tests execution faster let us just select a small subset of packages
        Set<EPackage> smallerSet = new HashSet<EPackage>();
        for (EPackage pkg : packages) {
            if ("cuscar".equals(pkg.getName()) || "invoic".equals(pkg.getName())) {
                smallerSet.add(pkg);
            }
        }
        Archive archive = SchemaConverter.INSTANCE.createArchive(smallerSet, pluginID, pathPrefix);
        archive.toOutputStream(new ZipOutputStream(new FileOutputStream("./target/" + archive.getArchiveName())));
    }

}
