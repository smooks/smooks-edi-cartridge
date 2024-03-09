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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.model.internal.CodeList;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.support.StreamUtils;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class D94ADirectoryParserTest {

    private DirectoryParser d94AReader;

    @BeforeEach
    public void init() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/d94a.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        d94AReader = new D94ADirectoryParser(zipInputStream, false, false);
    }

    @Test
    @DisplayName("should extract legacy D94A code list values properly")
    public void checkThatD94ACodeListsAreReadCorrectlyForLegacyD94A() {
        Map<String, byte[]> definitionFiles = d94AReader.getDefinitionFiles();
        assertFalse(definitionFiles.get("uncl") == null || definitionFiles.get("uncl").length == 0);
    }

    @Test
    @DisplayName("should parse ALI segment correctly")
    public void checkCorrectParsingOfD94AALISegment() throws IOException, JDOMException {

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

        Edimap edimap = UnEdifactDefinitionReader.parse(d94AReader);

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

        for (final Field field : aliSegment.getFields()) {
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
                        .withTest(new XMLOutputter(Format.getPrettyFormat()).outputString(node))
                        .ignoreComments()
                        .ignoreWhitespace()
                        .build()
                        .hasDifferences(),
                "Failed to compare XMLs for " + segmentCode);
    }
}