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
package org.smooks.edi.edisax;

import org.smooks.resource.URIResourceLocator;
import org.smooks.support.StreamUtils;
import org.smooks.tck.TextUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public abstract class AbstractEDIParserTestCase {

    protected void test(String testpack) throws IOException {
        String packageName = getClass().getPackage().getName().replace('.', '/');
        String mappingModel = "/" + packageName + "/" + testpack + "/edi-to-xml-mapping.xml";
        InputStream input = new ByteArrayInputStream(StreamUtils.readStream(getClass().getResourceAsStream(testpack + "/edi-input.txt")));
        String expected = new String(StreamUtils.readStream(getClass().getResourceAsStream(testpack + "/expected.xml"))).trim();
        MockContentHandler contentHandler = new MockContentHandler();

        expected = removeCRLF(expected);
        try {
            EDIParser parser = new EDIParser();
            String mappingResult = null;

            parser.setContentHandler(contentHandler);
            parser.setMappingModel(EDIParser.parseMappingModel(mappingModel, URIResourceLocator.extractBaseURI(mappingModel)));
            parser.setFeature(EDIParser.FEATURE_VALIDATE, true);
            parser.parse(new InputSource(input));

            mappingResult = contentHandler.xmlMapping.toString().trim();
            mappingResult = removeCRLF(mappingResult);
            if (!mappingResult.equals(expected)) {
                System.out.println("Expected: \n[" + expected + "]");
                System.out.println("Actual: \n[" + mappingResult + "]");
                assertEquals(expected, mappingResult, "Testpack [" + testpack + "] failed.");
            }
        } catch (SAXException e) {
            String exceptionMessage = e.getClass().getName() + ":" + e.getMessage();

            exceptionMessage = removeCRLF(exceptionMessage);
            if (!exceptionMessage.equals(expected)) {
                assertEquals(expected, exceptionMessage, "Unexpected exception on testpack [" + testpack + "].  ");
            }
        } catch (EDIConfigurationException e) {
            assert false : e;
        }
    }

    protected void testEDIParseException(String testpack, String segmentNodeName, int segmentNumber) throws IOException, EDIParseException {
        InputStream input = new ByteArrayInputStream(StreamUtils.readStream(getClass().getResourceAsStream(testpack + "/edi-input.txt")));
        InputStream mapping = new ByteArrayInputStream(StreamUtils.readStream(getClass().getResourceAsStream(testpack + "/edi-to-xml-mapping.xml")));

        MockContentHandler contentHandler = new MockContentHandler();

        try {
            EDIParser parser = new EDIParser();

            parser.setContentHandler(contentHandler);
            parser.setMappingModel(EDIParser.parseMappingModel(mapping));
            parser.setFeature(EDIParser.FEATURE_VALIDATE, true);
            parser.parse(new InputSource(input));

            fail("Test case should thow an EdiParseException.");

        } catch (EDIParseException e) {
            if (segmentNodeName == null) {
                assertNull(e.getErrorNode(), "EDIParseException should contain no MappingNode.");
            } else if (e.getErrorNode() == null) {
                throw e;
            } else {
                assertEquals(e.getErrorNode().getXmltag(), segmentNodeName, "EDIParseException should contain the MappingNode with xmltag [" + segmentNodeName + "]. Instead it contains [" + e.getErrorNode().getXmltag() + "].");
            }
            assertEquals(e.getSegmentNumber(), segmentNumber, "EDIParseException should contain the segmentNumber [" + segmentNumber + "]. Instead it contains [" + e.getSegmentNumber() + "].");

        } catch (Exception e) {
            assert false : e;
        }
    }


    private String removeCRLF(String string) throws IOException {
        return TextUtils.trimLines(new StringReader(string)).toString();
    }
}
