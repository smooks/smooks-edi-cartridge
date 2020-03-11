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
package org.milyn.edi.edisax.unedifact.with_ung;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.milyn.edi.edisax.model.EdifactModel;
import org.milyn.edi.edisax.registry.DefaultMappingsRegistry;
import org.milyn.edi.edisax.unedifact.UNEdifactInterchangeParser;
import org.milyn.edi.edisax.EDIConfigurationException;
import org.milyn.edi.edisax.EDIParseException;
import org.milyn.edi.edisax.EDIParser;
import org.milyn.edi.edisax.MockContentHandler;
import org.milyn.edi.edisax.MockContentHandlerNS;
import org.milyn.namespace.NamespaceAwareHandler;
import org.milyn.namespace.NamespaceDeclarationStack;
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
