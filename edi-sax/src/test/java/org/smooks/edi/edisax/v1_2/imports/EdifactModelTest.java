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
package org.smooks.edi.edisax.v1_2.imports;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.io.StreamUtils;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that the EdifactModel's logic works as expected.
 *
 * @author bardl
 */
public class EdifactModelTest {

    private String packageName = "/" + EdifactModelTest.class.getPackage().getName().replace('.', '/');

    @Test
    public void testImport_truncatableSegmentsExists() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(StreamUtils.readStream(getClass().getResourceAsStream("edi-config-truncatableSegmentsExists.xml")));
        EdifactModel ediModel = new EdifactModel(input);

        Assertions.assertTrue(((Segment) ediModel.getEdimap().getSegments().getSegments().get(0).getSegments().get(0)).isTruncatable(), "The truncatable attribute should have value [true] in Segment.");
    }

    @Test
    public void testImport_truncatableSegmentsExists_relative() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(StreamUtils.readStream(getClass().getResourceAsStream("edi-config-truncatableSegmentsExists-relativepath.xml")));
        EdifactModel ediModel = new EdifactModel(URI.create(packageName + "/" + "edi-config-truncatableSegmentsExists-relativepath.xml"), URI.create(packageName), input);

        assertTrue(((Segment) ediModel.getEdimap().getSegments().getSegments().get(0).getSegments().get(0)).isTruncatable(), "The truncatable attribute should have value [true] in Segment.");
    }

    @Test
    public void testImport_truncatableSegmentsNotExists() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(StreamUtils.readStream(getClass().getResourceAsStream("edi-config-truncatableSegmentsNotExists.xml")));
        EdifactModel ediModel = new EdifactModel(input);

        assertTrue(!((Segment) ediModel.getEdimap().getSegments().getSegments().get(0).getSegments().get(0)).isTruncatable(), "The truncatable attribute should have value [true] in Segment.");
    }
}
