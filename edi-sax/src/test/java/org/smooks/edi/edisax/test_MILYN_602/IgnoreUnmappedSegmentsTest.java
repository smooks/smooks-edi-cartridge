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
package org.smooks.edi.edisax.test_MILYN_602;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.MockContentHandler;
import org.smooks.edi.edisax.model.EdifactModel;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.smooks.edi.edisax.EDIParseException;
import org.xmlunit.builder.DiffBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class IgnoreUnmappedSegmentsTest {

    private EdifactModel msg1;
    private MockContentHandler handler;

    @BeforeEach
    public void setUp() throws IOException, SAXException {
        msg1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("edi-to-xml-mapping.xml"));
        handler = new MockContentHandler();
    }

    @AfterEach
    public void tearDown() {
        msg1 = null;
        handler = null;
    }

    // test cases to verify that the "ignoreUnmappedSegment" attribute is available
    @Test
    public void testIgnoreUnmappedSegmentsAttributeTrue() throws IOException, SAXException {
        assertEquals(true, msg1.getEdimap().isIgnoreUnmappedSegments(), "Attribute should be true");
    }

    @Test
    public void testIgnoreUnmappedSegmentsAttributeFalse() throws IOException, SAXException {
        msg1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("ignoreUnmappedSegments_false_mapping.xml"));
        assertFalse(msg1.getEdimap().isIgnoreUnmappedSegments(), "Attribute should be false");
    }

    @Test
    public void testIgnoreUnmappedSegmentsAttributeDefault() throws IOException, SAXException {
        msg1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("ignoreUnmappedSegments_default_value_mapping.xml"));
        assertFalse(msg1.getEdimap().isIgnoreUnmappedSegments(), "Attribute should be false");
    }

    // verify that a mapping model of only optional segments can ignore everything - basically anything goes
    @Test
    public void testIgnoreAllUnmapped() throws IOException, SAXException, EDIConfigurationException {
        msg1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("ignoreUnmappedSegments_all_optional.xml"));
        parseEdiMessage("IgnoreAllUnmapped.txt");
        String actual = handler.xmlMapping.toString();
        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("expected_all_optional.xml")).ignoreWhitespace().withTest(actual).build().hasDifferences(), "Got: " + actual);
    }

    // verify that can ignore unmapped in the beginning
    @Test
    public void testIgnoreUnmappedAtStart() throws IOException, SAXException, EDIConfigurationException {
        parseEdiMessage("IgnoreUnmappedAtStart.txt");
        verifyParsedEdiMessage();
    }

    // verify that can ignore unmapped in the middle
    @Test
    public void testIgnoreUnmappedInTheMiddle() throws IOException, SAXException, EDIConfigurationException {
        parseEdiMessage("IgnoreUnmappedInTheMiddle.txt");
        verifyParsedEdiMessage();
    }

    // verify that can ignore unmapped at the end
    @Test
    public void testIgnoreUnmappedAtEnd() throws IOException, SAXException, EDIConfigurationException {
        parseEdiMessage("IgnoreUnmappedAtEnd.txt");
        verifyParsedEdiMessage();
    }

    // verify that a message with no unmapped is valid
    @Test
    public void testNoUnmapped() throws IOException, SAXException, EDIConfigurationException {
        parseEdiMessage("NoUnmapped.txt");
        verifyParsedEdiMessage();
    }

    // ------------ IGNORED TEST CASES ---------------------
    // verify that if file only contains mapped but not valid (e.g. in wrong order) an exception is thrown
    // fails due to defect -- see below test case
    public void ignore_testNoUnmappedInvalid() throws IOException, SAXException, EDIConfigurationException {
        try {
            parseEdiMessage("NoUpmapped_invalid.txt");
            fail("Expected EDIParseException");
        } catch (EDIParseException ee) {
            // ignore verifing that the exception content is correct
        }
    }

    // verify that if strict validation is used we can still have an EDI message where the last mandatory segment is missing
    public void ignore_testDefect_StrictValidationMissingLastMandatorySegment() throws IOException, SAXException, EDIConfigurationException {
        try {
            msg1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("ignoreUnmappedSegments_false_mapping.xml"));
            parseEdiMessage("NoUpmapped_MissingRequired.txt");
            fail("Expected EDIParseException");
        } catch (EDIParseException ee) {
            // ignore verifing that the exception content is correct
        }
    }

    //-----------------------------------------------------------
    // verify that if file only contains unmapped an exception is thrown
    @Test
    public void testNoMapped() throws IOException, SAXException, EDIConfigurationException {
        try {
            parseEdiMessage("NoMapped.txt");
            fail("Expected EDIParseException");
        } catch (EDIParseException ee) {
            // ignore verifing that the exception content is correct
        }
    }

    // verify that if file contains mapped and unmapped segments but missing some mapped segments an exception is thrown
    @Test
    public void testMissingRequired() throws IOException, SAXException, EDIConfigurationException {
        try {
            parseEdiMessage("MissingRequired.txt");
            fail("Expected EDIParseException");
        } catch (EDIParseException ee) {
            // ignore verifing that the exception content is correct
        }
    }

    private void parseEdiMessage(String inputFile) throws IOException, SAXException, EDIParseException {
        EDIParser parser = new EDIParser();
        parser.setMappingModel(msg1);
        parser.setContentHandler(handler);
        parser.parse(new InputSource(getClass().getResourceAsStream(inputFile)));
    }

    private void verifyParsedEdiMessage() throws SAXException, IOException {
        String actual = handler.xmlMapping.toString();
        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("expected.xml")).ignoreWhitespace().withTest(actual).build().hasDifferences(), "Got: " + new StringReader(actual));
    }
}
