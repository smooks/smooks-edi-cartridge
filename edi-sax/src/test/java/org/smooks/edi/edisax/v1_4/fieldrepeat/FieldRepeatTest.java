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
package org.smooks.edi.edisax.v1_4.fieldrepeat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.MockContentHandler;
import org.smooks.edi.edisax.model.EdifactModel;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FieldRepeatTest {

    @Test
    public void test() throws IOException, SAXException, EDIConfigurationException {
        MockContentHandler handler;
        EdifactModel msg1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("edi-to-xml-mapping.xml"));

        EDIParser parser = new EDIParser();
        parser.setMappingModel(msg1);

        handler = new MockContentHandler();
        parser.setContentHandler(handler);

        parser.parse(new InputSource(getClass().getResourceAsStream("edi-input.txt")));

		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("expected.xml")).ignoreWhitespace().withTest(handler.xmlMapping.toString()).build().hasDifferences());
    }
}
