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

import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.support.StreamUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class AbstractDirectoryParserTest {

    protected String getEdiMessageAsString(DirectoryParser directoryParser, String messageType) throws IOException {
        Edimap edimap = directoryParser.getMappingModel(messageType, UnEdifactDefinitionReader.parse(directoryParser));
        StringWriter sw = new StringWriter();
        edimap.write(sw);
        return sw.toString();
    }

    protected void testPackage(String packageName, String mappingModel) throws IOException, SAXException, EDIConfigurationException {
        InputStream testFileInputStream = getClass().getResourceAsStream("/testfiles/" + packageName + "/input.edi");

        MockContentHandler contentHandler = new MockContentHandler();
        EDIParser parser = new EDIParser();
        parser.setContentHandler(contentHandler);
        parser.setMappingModel(EDIParser.parseMappingModel(new StringReader(mappingModel)));
        parser.parse(new InputSource(testFileInputStream));

        String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("/testfiles/" + packageName + "/expected-result.xml"))).trim();
        String actual = contentHandler.xmlMapping.toString();

        assertFalse(DiffBuilder.compare(expected).withTest(actual).ignoreComments().ignoreWhitespace().build().hasDifferences());
    }

    /************************************************************************
     * Private class MockContentHandler                                     *
     ************************************************************************/
    private static class MockContentHandler extends DefaultHandler {

        protected StringBuffer xmlMapping = new StringBuffer();

        public void startDocument() {
            xmlMapping.setLength(0);
        }

        public void characters(char[] ch, int start, int length) {
            xmlMapping.append(ch, start, length);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            xmlMapping.append("<").append(localName).append(">");
        }

        public void endElement(String namespaceURI, String localName, String qName) {
            xmlMapping.append("</").append(localName).append(">");
        }
    }
}