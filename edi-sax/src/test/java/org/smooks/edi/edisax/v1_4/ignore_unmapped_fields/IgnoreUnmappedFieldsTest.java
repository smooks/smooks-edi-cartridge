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
package org.smooks.edi.edisax.v1_4.ignore_unmapped_fields;

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
public class IgnoreUnmappedFieldsTest {

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
