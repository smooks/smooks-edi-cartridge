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
package org.milyn.edifact.edisax.v1_4.fieldrepeat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.milyn.edifact.edisax.EDIConfigurationException;
import org.milyn.edifact.edisax.EDIParser;
import org.milyn.edifact.edisax.MockContentHandler;
import org.milyn.edifact.edisax.model.EdifactModel;
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
