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
import org.smooks.edi.ect.formats.unedifact.UnEdifactSpecificationReader;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.model.internal.CodeList;
import org.smooks.edi.edisax.model.internal.Component;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.io.StreamUtils;
import org.smooks.util.ClassUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UnEdifactSpecificationReaderTest.
 *
 * @author bardl
 */
@SuppressWarnings("deprecation")
@DisplayName("UnEdifactSpecificationReader")
public class UnEdifactSpecificationReaderTest {

    private XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());

    @Nested
    @DisplayName("for D08A message directory")
    class D08A {
        private UnEdifactSpecificationReader d08AReader_longnames;
        private UnEdifactSpecificationReader d08AReader_shortnames;

        @BeforeEach
        public void parseD08A() throws Exception {
            InputStream
                inputStream =
                UnEdifactSpecificationReaderTest.class.getResourceAsStream("D08A.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            d08AReader_longnames = new UnEdifactSpecificationReader(zipInputStream, false, false);

            inputStream = UnEdifactSpecificationReaderTest.class.getResourceAsStream("D08A.zip");
            zipInputStream = new ZipInputStream(inputStream);
            d08AReader_shortnames = new UnEdifactSpecificationReader(zipInputStream, false, true);
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
            Set<String> messages = d08AReader_longnames.getMessageNames();
            for (String message : messages) {
                Edimap model = d08AReader_longnames.getMappingModel(message);
                StringWriter writer = new StringWriter();
                model.write(writer);
            }
        }

        @Test
        @DisplayName("should support short messages names")
        public void test_getMessagesShortName() throws IOException {
            Set<String> messages = d08AReader_shortnames.getMessageNames();
            for (String message : messages) {
                Edimap model = d08AReader_shortnames.getMappingModel(message);
                StringWriter writer = new StringWriter();
                model.write(writer);
            }
        }

        @Test
        @DisplayName("should support long segment names")
        public void test_D08A_SegmentsLongName()
            throws IOException, EdiParseException, SAXException, JDOMException {

            Edimap edimap = d08AReader_longnames.getDefinitionModel();

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

            Edimap edimap = d08AReader_shortnames.getDefinitionModel();

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

        @Test
        @DisplayName("should handle real-life D93A input file with support for long names")
        public void testRealLifeInputFilesD93ALongName()
            throws IOException, EDIConfigurationException, SAXException {
            InputStream inputStream = ClassUtil.getResourceAsStream("d93a.zip", this.getClass());
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);

            EdiSpecificationReader
                ediSpecificationReader =
                new UnEdifactSpecificationReader(zipInputStream, false, false);

            //Test INVOIC
            String mappingModel = getEdiMessageAsString(ediSpecificationReader, "INVOIC");
            testPackage("d93a-invoic-1", mappingModel);
        }

        @Test
        @DisplayName("should handle real-life D93A input file with support for short names")
        public void testRealLifeInputFilesD93AShortName()
            throws IOException, EDIConfigurationException, SAXException {
            InputStream inputStream = ClassUtil.getResourceAsStream("d93a.zip", this.getClass());
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);

            EdiSpecificationReader
                ediSpecificationReader =
                new UnEdifactSpecificationReader(zipInputStream, false, true);

            //Test INVOIC
            String mappingModel = getEdiMessageAsString(ediSpecificationReader, "INVOIC");
            testPackage("d93a-invoic-shortname", mappingModel);
        }

        public void testPackage(String packageName, String mappingModel) throws IOException, SAXException, EDIConfigurationException {
            InputStream testFileInputStream = getClass().getResourceAsStream("testfiles/" + packageName + "/input.edi");

            MockContentHandler contentHandler = new MockContentHandler();
            EDIParser parser = new EDIParser();
            parser.setContentHandler(contentHandler);
            parser.setMappingModel(EDIParser.parseMappingModel(new StringReader(mappingModel)));
            parser.parse(new InputSource(testFileInputStream));

            String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("testfiles/" + packageName + "/expected-result.xml"))).trim();
            String actual = contentHandler.xmlMapping.toString();

            assertFalse(DiffBuilder.compare(expected).withTest(actual).ignoreComments().ignoreWhitespace().build().hasDifferences());
        }

        private String getEdiMessageAsString(EdiSpecificationReader ediSpecificationReader, String messageType) throws IOException {
            Edimap edimap = ediSpecificationReader.getMappingModel(messageType);
            StringWriter sw = new StringWriter();
            edimap.write(sw);
            return sw.toString();
        }

        private void testSegment(final String segmentCode, Document doc, boolean useShortName) throws IOException, SAXException, JDOMException {
            String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("d08a/segment/expected-" + (useShortName ? "shortname-" : "") + segmentCode.toLowerCase() + ".xml"))).trim();
            XPath lookup = XPath.newInstance("//medi:segment[@segcode='" + segmentCode + "']");
            lookup.addNamespace("medi", "http://www.milyn.org/schema/edi-message-mapping-1.5.xsd");
            Element node = (Element) lookup.selectSingleNode(doc);
            assertNotNull(node, "Node with segment code " + segmentCode + " wasn't found");

            assertFalse(DiffBuilder.compare(expected).withTest(out.outputString(node)).ignoreComments().ignoreWhitespace().build().hasDifferences(), "Failed to compare XMLs for " + segmentCode);
        }

        private void test(String messageName, EdiSpecificationReader ediSpecificationReader) throws IOException {
            Edimap edimap = ediSpecificationReader.getMappingModel(messageName);

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
    @DisplayName("for D96A message directory")
    class D96A {

        @Test
        @DisplayName("should extract legacy D96A code list values correctly")
        public void checkThatD96ACodeListsAreReadCorrectlyForLegacyD96A() throws Exception {
            InputStream
                inputStream =
                UnEdifactSpecificationReaderTest.class.getResourceAsStream("d96a.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            UnEdifactSpecificationReader
                d96AReader =
                new UnEdifactSpecificationReader(zipInputStream, false, false);

            Map<String, byte[]> definitionFiles = d96AReader.getDefinitionFiles();
            assertFalse(
                definitionFiles.get("uncl") == null || definitionFiles.get("uncl").length == 0);
        }

        @Test
        @DisplayName("should parse components correctly")
        public void checkThatD96AComponentsAreCorrectlyParsed() throws Exception {
            InputStream inputStream =
                UnEdifactSpecificationReaderTest.class.getResourceAsStream("d96a.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            UnEdifactSpecificationReader d96AReader =
                new UnEdifactSpecificationReader(zipInputStream, false, false);

            final Field component = getComplexElement(d96AReader, "C770");
            assertEquals("9424", component.getComponents().get(0).getNodeTypeRef());
        }

        private Field getComplexElement(final UnEdifactSpecificationReader reader, final String componentName) {
            Edimap edimap = reader.getDefinitionModel();
            for (final Field field : edimap.getCompositeDataElements()) {
                if (componentName.equals(field.getNodeTypeRef())) {
                    return field;
                }
            }
            return null;
        }
    }

    @Nested
    @DisplayName("for D94A message directory")
    class D94A {

        private UnEdifactSpecificationReader d94AReader;

        @BeforeEach
        public void init() throws Exception {
            InputStream
                inputStream =
                UnEdifactSpecificationReaderTest.class.getResourceAsStream("d94a.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            d94AReader =
                new UnEdifactSpecificationReader(zipInputStream, false, false);
        }

        @Test
        @DisplayName("should extract legacy D94A code list values properly")
        public void checkThatD94ACodeListsAreReadCorrectlyForLegacyD94A() {

            Map<String, byte[]> definitionFiles = d94AReader.getDefinitionFiles();
            assertFalse(
                definitionFiles.get("uncl") == null || definitionFiles.get("uncl").length == 0);
        }

        @Test
        @DisplayName("should parse ALI segment correctly")
        public void checkCorrectParsingOfD94AALISegment()
            throws IOException, JDOMException {

            // Schema Definition Error:
            //   Error loading schema due to org.xml.sax.SAXParseException; systemId: .../smooks-edi-cartridge/edifact-schemas/target/classes/d94a/EDIFACT-Segments.dfdl.xsd; lineNumber: 2362; columnNumber: 47;
            //     enumeration-valid-restriction: Enumeration value '3166' is not in the value space of the base type, alpha0-3.
            // Schema context: .../smooks-edi-cartridge/edifact-schemas/target/classes/d94a/EDIFACT-Messages.dfdl.xsd
            //   Location in file:.../smooks-edi-cartridge/edifact-schemas/target/classes/d94a/EDIFACT-Messages.dfdl.xsd

            // TRSD.94A
            //
            //       ALI    ADDITIONAL INFORMATION
            //
            //       Function: To indicate that special conditions due to the
            //                 origin, customs preference, fiscal or commercial
            //                 factors are applicable.
            //
            // 010   3239  COUNTRY OF ORIGIN, CODED                      C an..3
            //
            // 020   9213  TYPE OF DUTY REGIME, CODED                    C an..3
            //
            // 030   4183  SPECIAL CONDITIONS, CODED                     C an..3
            //
            // 040   4183  SPECIAL CONDITIONS, CODED                     C an..3
            //
            // 050   4183  SPECIAL CONDITIONS, CODED                     C an..3
            //
            // 060   4183  SPECIAL CONDITIONS, CODED                     C an..3
            //
            // 070   4183  SPECIAL CONDITIONS, CODED                     C an..3
            //
            // -------------------------------------------------------------------------------------
            //
            // TRED.94A
            //
            //  3239  Country of origin, coded
            //
            //  Desc: Country in which the goods have been produced or
            //        manufactured, according to criteria laid down for the
            //        purposes of application of the Customs tariff, of
            //        quantitative restrictions, or of any other measure related
            //        to trade.
            //
            //  Repr: an..3
            //
            //  Note: See section 5.2 Country and currency codes (UNTDED) or
            //        ISO 3166 two alpha country code (Code set 3207).
            //
            // -------------------------------------------------------------------------------------
            //
            // UNCL-1.94A
            //
            //   3239  Country of origin, coded
            //
            //  Desc: Country in which the goods have been produced or
            //        manufactured, according to criteria laid down for the
            //        purposes of application of the Customs tariff, of
            //        quantitative restrictions, or of any other measure related
            //        to trade.
            //
            //  Repr: an..3
            //
            //  Note: See section 5.2 Country and currency codes (UNTDED) or ISO
            //        3166 two alpha country code (Code set 3207).
            //
            //
            // 3166 is a part of the code list definition note and not the actual code list value

            Edimap edimap = d94AReader.getDefinitionModel();

            StringWriter stringWriter = new StringWriter();
            edimap.write(stringWriter);

            Document document = new SAXBuilder().build(new StringReader(stringWriter.toString()));

            testSegment("ALI", document, false);

            Segment aliSegment = (Segment) edimap.getSegments().getSegments().get(3);
            assertEquals(7, aliSegment.getFields().size());
            assertEquals("3239", aliSegment.getFields().get(0).getNodeTypeRef());
            assertEquals(Collections.emptyList(), aliSegment.getFields().get(0).getComponents());
            CodeList codeList = aliSegment.getFields().get(0).getCodeList();
            assertFalse(codeList.getCodes().contains("3166"), "CodeList contains unexpected entry 3166!");

            for (final org.smooks.edi.edisax.model.internal.Field field : aliSegment.getFields()) {
                assertFalse(field.getDocumentation().contains("3166"));
            }
        }

        private void testSegment(final String segmentCode, Document doc, boolean useShortName) throws IOException, JDOMException {
            String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream("/d94a_expected-" + (useShortName ? "shortname-" : "") + segmentCode.toLowerCase() + ".xml"))).trim();
            XPath lookup = XPath.newInstance("//medi:segment[@segcode='" + segmentCode + "']");
            lookup.addNamespace("medi", "http://www.milyn.org/schema/edi-message-mapping-1.5.xsd");
            Element node = (Element) lookup.selectSingleNode(doc);
            assertNotNull(node, "Node with segment code " + segmentCode + " wasn't found");

            assertFalse(DiffBuilder.compare(expected)
                            .withTest(out.outputString(node))
                            .ignoreComments()
                            .ignoreWhitespace()
                            .build()
                            .hasDifferences(),
                        "Failed to compare XMLs for " + segmentCode);
        }
    }

    @Nested
    @DisplayName("for D00A message directory")
    class D00A {

        private UnEdifactSpecificationReader d00AReader;

        @BeforeEach
        public void init() throws Exception {
            InputStream
                inputStream =
                UnEdifactSpecificationReaderTest.class.getResourceAsStream("d00a.zip");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            d00AReader =
                new UnEdifactSpecificationReader(zipInputStream, false, false);
        }

        @Test
        @DisplayName("should not contain wrong or duplicate code list value")
        public void shouldNotContainDuplicateCodeListEntries() {
            // code list values for component 1131 on item 36 and 38 did contain both changes to
            // their names as well as changes to their text descriptions, thus the RegEx did not
            // capture these value properly but instead lead to an invalid A entries that occurred
            // multiple times and therefore raised a failure in the DFDL XML schemata
            final List<Component> simpleDataElements =
                d00AReader.getDefinitionModel().getSimpleDataElements();

            final Component e1131 = simpleDataElements.get(23);
            assertTrue(e1131.getCodeList().getCodes().containsAll(Arrays.asList("35", "36", "37", "38", "39")));
            assertFalse(e1131.getCodeList().getCodes().contains("A"));
        }
    }

    /************************************************************************
     * Private class MockContentHandler                                     *
     ************************************************************************/
    private class MockContentHandler extends DefaultHandler {

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
