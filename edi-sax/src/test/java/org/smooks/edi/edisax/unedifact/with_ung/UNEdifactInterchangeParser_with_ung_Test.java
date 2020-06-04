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
/*
	Milyn - Copyright (C) 2006 - 2010

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.smooks.edi.edisax.unedifact.with_ung;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.registry.DefaultMappingsRegistry;
import org.smooks.edi.edisax.unedifact.UNEdifactInterchangeParser;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.EDIParseException;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.MockContentHandler;
import org.smooks.edi.edisax.MockContentHandlerNS;
import org.smooks.namespace.NamespaceAwareHandler;
import org.smooks.namespace.NamespaceDeclarationStack;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNEdifactInterchangeParser_with_ung_Test {

    @Test
    public void test_with_groupref() throws IOException, SAXException, EDIConfigurationException {
        EdifactModel model1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG1-model.xml"));
        EdifactModel model2 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG2-model.xml"));

        UNEdifactInterchangeParser parser = new UNEdifactInterchangeParser();
        parser.setMappingsRegistry(new DefaultMappingsRegistry(model1, model2));
        parser.ignoreNewLines(true);

        MockContentHandlerNS handler;

        // Test message 01 - no UNA segment...
        handler = new MockContentHandlerNS();
        NamespaceDeclarationStack namespaceDeclarationStack = new NamespaceDeclarationStack(parser);

        parser.setContentHandler(new NamespaceAwareHandler(handler, namespaceDeclarationStack));
        parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-01.edi")));
        //System.out.println(handler.xmlMapping);
		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("unedifact-msg-expected-01.xml")).ignoreWhitespace().withTest(handler.xmlMapping.toString()).build().hasDifferences());

        // Test message 01 - has a UNA segment...
        handler = new MockContentHandlerNS();
        parser.setContentHandler(handler);
        parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-02.edi")));

//		System.out.println(handler.xmlMapping);
		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("unedifact-msg-expected-01.xml")).ignoreWhitespace().withTest(handler.xmlMapping.toString()).build().hasDifferences());
    }

    @Test
    public void test_without_groupref() throws IOException, SAXException, EDIConfigurationException {
        EdifactModel model1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG1-model.xml"));
        EdifactModel model2 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG2-model.xml"));

        UNEdifactInterchangeParser parser = new UNEdifactInterchangeParser();
        parser.setMappingsRegistry(new DefaultMappingsRegistry(model1, model2));
        parser.ignoreNewLines(true);

        MockContentHandler handler;

        handler = new MockContentHandler();
        NamespaceDeclarationStack namespaceDeclarationStack = new NamespaceDeclarationStack(parser);
        parser.setContentHandler(new NamespaceAwareHandler(handler, namespaceDeclarationStack));

        try {
            parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-03.edi")));
            fail("Expected EDIParseException");
        } catch (EDIParseException e) {
            assertEquals("EDI message processing failed [EDI Message Interchange Control Model][N/A].  Segment [UNG], field 5 (groupRef) expected to contain a value.  Currently at segment number 2.", e.getMessage());
        }
    }

    @Test
    public void test_full_group_header() throws IOException, SAXException, EDIConfigurationException {
        EdifactModel model1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG1-model.xml"));
        EdifactModel model2 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG2-model.xml"));

        UNEdifactInterchangeParser parser = new UNEdifactInterchangeParser();
        parser.setMappingsRegistry(new DefaultMappingsRegistry(model1, model2));
        parser.ignoreNewLines(true);

        MockContentHandlerNS handler;

        // Test message 01 - no UNA segment...
        handler = new MockContentHandlerNS();
        NamespaceDeclarationStack namespaceDeclarationStack = new NamespaceDeclarationStack(parser);
        parser.setContentHandler(new NamespaceAwareHandler(handler, namespaceDeclarationStack));
        parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-04.edi")));
        //System.out.println(handler.xmlMapping);
		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("unedifact-msg-expected-02.xml")).ignoreWhitespace().withTest(handler.xmlMapping.toString()).build().hasDifferences());
    }

    @Test
    public void test_with_unknown_ucd_segment() throws IOException, SAXException, EDIConfigurationException {
        EdifactModel model1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG1-model.xml"));
        EdifactModel model2 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG2-model.xml"));

        UNEdifactInterchangeParser parser = new UNEdifactInterchangeParser();
        parser.setMappingsRegistry(new DefaultMappingsRegistry(model1, model2));
        parser.ignoreNewLines(true);

        MockContentHandler handler;

        // Test message 01 - no UNA segment...
        handler = new MockContentHandler();
        NamespaceDeclarationStack namespaceDeclarationStack = new NamespaceDeclarationStack(parser);
        parser.setContentHandler(new NamespaceAwareHandler(handler, namespaceDeclarationStack));
        parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-05.edi")));
//		System.out.println(handler.xmlMapping);
		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("unedifact-msg-expected-03.xml")).ignoreWhitespace().withTest(handler.xmlMapping.toString()).build().hasDifferences());
    }
}
