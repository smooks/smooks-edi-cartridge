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
package org.smooks.edi.ect.formats.unedifact.parser;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.internal.Component;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.io.StreamUtils;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UnEdifactSpecificationReaderTest.
 *
 * @author bardl
 */
@DisplayName("UnEdifactDirectoryParserTest")
public class UnEdifactDirectoryParserTest extends AbstractDirectoryParserTest {

    private static final XMLOutputter XML_OUTPUTTER = new XMLOutputter(Format.getPrettyFormat());

    @Nested
    @DisplayName("for D08A message directory")
    class D08A {
        private DirectoryParser d08AReader_longnames;
        private DirectoryParser d08AReader_shortnames;

        @BeforeEach
        public void parseD08A() throws Exception {
            InputStream inputStream = getClass().getResourceAsStream("/d08a.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            d08AReader_longnames = new UnEdifactDirectoryParser(zipInputStream, false, false);

            inputStream = getClass().getResourceAsStream("/d08a.zip");
            zipInputStream = new ZipInputStream(inputStream);
            d08AReader_shortnames = new UnEdifactDirectoryParser(zipInputStream, false, true);
        }

        public void _disabled_test_D08A_Messages() throws IOException, EdiParseException {
            test("BANSTA", d08AReader_longnames);
            test("CASRES", d08AReader_longnames);
            test("INVOIC", d08AReader_longnames);
            test("PAYMUL", d08AReader_longnames);
            test("TPFREP", d08AReader_longnames);
        }

        @Test
        @DisplayName("should support long messages names")
        public void test_getMessagesLongName() throws IOException {
            Edimap edimap = UnEdifactDefinitionReader.parse(d08AReader_longnames);
            Set<String> messages = d08AReader_longnames.getMessageNames(edimap);
            for (String message : messages) {
                Edimap model = d08AReader_longnames.getMappingModel(message, edimap);
                StringWriter writer = new StringWriter();
                model.write(writer);
            }
        }

        @Test
        @DisplayName("should support short messages names")
        public void test_getMessagesShortName() throws IOException {
            Edimap edimap = UnEdifactDefinitionReader.parse(d08AReader_shortnames);
            Set<String> messages = d08AReader_shortnames.getMessageNames(edimap);
            for (String message : messages) {
                Edimap model = d08AReader_shortnames.getMappingModel(message, edimap);
                StringWriter writer = new StringWriter();
                model.write(writer);
            }
        }

        @Test
        @DisplayName("should support long segment names")
        public void test_D08A_SegmentsLongName()
            throws IOException, EdiParseException, SAXException, JDOMException {

            Edimap edimap = UnEdifactDefinitionReader.parse(d08AReader_longnames);

            StringWriter stringWriter = new StringWriter();
            edimap.write(stringWriter);

            Document document = new SAXBuilder().build(new StringReader(stringWriter.toString()));

            testSegment("BGM", document, false);
            testSegment("DTM", document, false);
            testSegment("NAD", document, false);
            testSegment("PRI", document, false);
        }

        @Test
        @DisplayName("should support short segment names")
        public void test_D08A_Segments_ShortName()
            throws IOException, EdiParseException, SAXException, JDOMException {
            
            Edimap edimap = UnEdifactDefinitionReader.parse(d08AReader_shortnames);

            StringWriter stringWriter = new StringWriter();
            edimap.write(stringWriter);

            Document document = new SAXBuilder().build(new StringReader(stringWriter.toString()));

            testSegment("BGM", document, true);
            testSegment("DTM", document, true);
            testSegment("NAD", document, true);
            testSegment("PRI", document, true);
        }

        @Test
        @DisplayName("should handle real-life D96A input file with support for long names")
        public void testRealLifeInputFilesD96ALongName()
            throws IOException, EDIConfigurationException, SAXException {
            //Test INVOIC
            String mappingModel = getEdiMessageAsString(d08AReader_longnames, "INVOIC");
            testPackage("d96a-invoic-1", mappingModel);
        }

        @Test
        @DisplayName("should handle real-life D96A input file with support for short names")
        public void testRealLifeInputFilesD96AShortName()
            throws IOException, EDIConfigurationException, SAXException {
            //Test INVOIC
            String mappingModel = getEdiMessageAsString(d08AReader_shortnames, "INVOIC");
            testPackage("d96a-invoic-shortname", mappingModel);
        }
        
        private void testSegment(final String segmentCode, Document doc, boolean useShortName) throws IOException, JDOMException {
            String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("/testfiles/d08a/segment/expected-" + (useShortName ? "shortname-" : "") + segmentCode.toLowerCase() + ".xml"))).trim();
            XPath lookup = XPath.newInstance("//medi:segment[@segcode='" + segmentCode + "']");
            lookup.addNamespace("medi", "http://www.milyn.org/schema/edi-message-mapping-1.5.xsd");
            Element node = (Element) lookup.selectSingleNode(doc);
            assertNotNull(node, "Node with segment code " + segmentCode + " wasn't found");

            assertFalse(DiffBuilder.compare(expected).withTest(XML_OUTPUTTER.outputString(node)).ignoreComments().ignoreWhitespace().build().hasDifferences(), "Failed to compare XMLs for " + segmentCode);
        }

        private void test(String messageName, DirectoryParser ediSpecificationReader) throws IOException {
            Edimap edimap = ediSpecificationReader.getMappingModel(messageName, new Edimap());

            StringWriter stringWriter = new StringWriter();
            edimap.write(stringWriter);
//		String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("d08a/message/expected-" + messageName.toLowerCase() + ".xml"))).trim();
//
//        String result = removeCRLF(stringWriter.toString());
//		expected = removeCRLF(expected);
//
//        if(!result.equals(expected)) {
//            System.out.println("Expected: \n[" + expected + "]");
//            System.out.println("Actual: \n[" + result + "]");
//            assertEquals("Message [" + messageName + "] failed.", expected, result);
//        }

            StringWriter result = new StringWriter();
            edimap.write(result);
            String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("d08a/message/expected-" + messageName.toLowerCase() + ".xml"))).trim();

            assertFalse(DiffBuilder.compare(expected).withTest(result.toString()).ignoreWhitespace().build().hasDifferences());
        }
    }

    @Nested
    @DisplayName("for D00A message directory")
    class D00A {

        private DirectoryParser d00AReader;

        @BeforeEach
        public void init() throws Exception {
            InputStream inputStream = getClass().getResourceAsStream("/d00a.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            d00AReader = new UnEdifactDirectoryParser(zipInputStream, false, false);
        }

        @Test
        @DisplayName("should not contain wrong or duplicate code list value")
        public void shouldNotContainDuplicateCodeListEntries() throws IOException {
            // code list values for component 1131 on item 36 and 38 did contain both changes to
            // their names as well as changes to their text descriptions, thus the RegEx did not
            // capture these value properly but instead lead to an invalid A entries that occurred
            // multiple times and therefore raised a failure in the DFDL XML schemata
            final List<Component> simpleDataElements = UnEdifactDefinitionReader.parse(d00AReader).getSimpleDataElements();

            final Component e1131 = simpleDataElements.get(23);
            assertTrue(e1131.getCodeList().getCodes().containsAll(Arrays.asList("35", "36", "37", "38", "39")));
            assertFalse(e1131.getCodeList().getCodes().contains("A"));
        }
    }
}