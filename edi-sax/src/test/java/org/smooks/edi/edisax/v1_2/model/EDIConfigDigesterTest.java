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
package org.smooks.edi.edisax.v1_2.model;

import org.junit.jupiter.api.Test;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.converter.factory.system.StringToDateConverterFactory;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.EDIConfigDigester;
import org.smooks.edi.edisax.model.internal.*;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.smooks.io.StreamUtils.readStream;

/**
 * This testcase tests that all new elements introduced in version 1.2 is digested from
 * configurationfile.
 *
 * @author bardl
 */
public class EDIConfigDigesterTest {

    /**
     * This testcase tests that parent MappingNode is connected to the correct MappingNode.
     *
     * @throws EDIConfigurationException is thrown when error occurs during config-digestion.
     * @throws IOException               is thrown when unable to read edi-config in testcase.
     * @throws SAXException              is thrown when error occurs during config-digestion.
     */
    @Test
    public void testParentMappingNodes() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-all-new-elements.xml")));
        Edimap edimap = EDIConfigDigester.digestConfig(input);

        //SegmentGroup
        SegmentGroup rootSegmentGroup = edimap.getSegments();
        assertNull(rootSegmentGroup.getParent(), "Root segmentGroup should have no parent");

        SegmentGroup segmentGroup = edimap.getSegments().getSegments().get(0);
        assertEquals(segmentGroup.getParent(), rootSegmentGroup, "SegmentGroup[" + segmentGroup.getXmltag() + "] should have the root SegmentGroup[" + rootSegmentGroup.getXmltag() + "] as parent but had parent[" + segmentGroup.getParent().getXmltag() + "]");

        //Segment
        Segment segment = (Segment) segmentGroup.getSegments().get(0);
        assertEquals(segment.getParent(), segmentGroup, "Segment[" + segment.getXmltag() + "] should have the SegmentGroup[" + segmentGroup.getXmltag() + "] as parent");

        //Fields
        for (Field field : segment.getFields()) {
            assertEquals(field.getParent(), segment, "Field[" + field.getXmltag() + "] should have the Segment[" + segment.getXmltag() + "] as parent");
            for (Component component : field.getComponents()) {
                assertEquals(component.getParent(), field, "Component[" + component.getXmltag() + "] should have the Field[" + field.getXmltag() + "] as parent");
                for (SubComponent subComponent : component.getSubComponents()) {
                    assertEquals(subComponent.getParent(), component, "SubComponent[" + subComponent.getXmltag() + "] should have the Component[" + component.getXmltag() + "] as parent");
                }
            }
        }
    }

    /**
     * This testcase tests that all values are read from ValueNode.
     *
     * @throws EDIConfigurationException is thrown when error occurs during config-digestion.
     * @throws IOException               is thrown when unable to read edi-config in testcase.
     * @throws SAXException              is thrown when error occurs during config-digestion.
     */
    @Test
    public void testReadValueNodes() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-all-new-elements.xml")));
        Edimap edimap = EDIConfigDigester.digestConfig(input);

        //SegmentGroup
        SegmentGroup segmentGroup = edimap.getSegments().getSegments().get(0);
        assertEquals(segmentGroup.getDocumentation(), "segmentGroup-documentation", "Failed to digest documentation for SegmentGroup");

        Segment segment = (Segment) segmentGroup.getSegments().get(0);
        assertEquals(segment.getDocumentation(), "segment-documentation", "Failed to digest documentation for Segment");
        List<Field> fields = segment.getFields();

        // Assert field is read correctly.
        // <medi:field xmltag="aTime" type="Time" format="HHmm" minLength="0" maxLength="4"/>
        assertEquals(fields.get(0).getDataType(), "Date", "Failed to digest type-attribute for Field");
        assertEquals(fields.get(0).getTypeParameters().get(0).getKey(), "format", "Failed to digest parameters-attribute for Field");
        assertEquals(fields.get(0).getTypeParameters().get(0).getValue(), "HHmm", "Failed to digest parameters-attribute for Field");
        assertEquals(fields.get(0).getMinLength(), new Integer(0), "Failed to digest minLength-attribute for Field");
        assertEquals(fields.get(0).getMaxLength(), new Integer(4), "Failed to digest maxLength-attribute for Field");
        assertEquals(fields.get(0).getDocumentation(), "field1-documentation", "Failed to digest documentation for Field");

        // Assert Component is read correctly.
        // <medi:component xmltag="aBinary" required="true" type="Binary" minLength="0" maxLength="8"/>
        Component component = fields.get(1).getComponents().get(0);
        assertEquals(component.getDataType(), "Binary", "Failed to digest type-attribute for Component");
        assertNull(component.getTypeParameters(), "Parameters-attribute should be null in Component");
        assertEquals(component.getMinLength(), new Integer(0), "Failed to digest minLength-attribute for Component");
        assertEquals(component.getMaxLength(), new Integer(8), "Failed to digest maxLength-attribute for Component");
        assertEquals(component.getDocumentation(), "component-documentation", "Failed to digest documentation for Component");

        // Assert SubComponent is read correctly.
        // <medi:sub-component xmltag="aNumeric" type="Numeric" format="#0.00" minLength="1" maxLength="4"/>
        SubComponent subcomponent = fields.get(1).getComponents().get(1).getSubComponents().get(0);
        assertEquals(subcomponent.getDataType(), "Double", "Failed to digest type-attribute for SubComponent");
        assertEquals(subcomponent.getTypeParameters().get(0).getKey(), "format", "Failed to digest parameters-attribute for SubComponent");
        assertEquals(subcomponent.getTypeParameters().get(0).getValue(), "#0.00", "Failed to digest format-attribute for SubComponent");
        assertEquals(subcomponent.getMinLength(), new Integer(1), "Failed to digest minLength-attribute for SubComponent");
        assertEquals(subcomponent.getMaxLength(), new Integer(4), "Failed to digest maxLength-attribute for SubComponent");
        assertEquals(subcomponent.getDocumentation(), "subcomponent-documentation", "Failed to digest documentation for SubComponent");
    }

    /**
     * This testcase tests that description attribute is read from Segment.
     *
     * @throws EDIConfigurationException is thrown when error occurs during config-digestion.
     * @throws IOException               is thrown when unable to read edi-config in testcase.
     * @throws SAXException              is thrown when error occurs during config-digestion.
     */
    @Test
    public void testReadSegmentDescription() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-all-new-elements.xml")));
        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment) edimap.getSegments().getSegments().get(0).getSegments().get(0);
        String expected = "This segment is used for testing all new elements in v.1.2";
        assertEquals(segment.getDescription(), expected, "Description in segment [" + segment.getDescription() + "] doesn't match expected value [" + expected + "].");
    }

    @Test
    public void testCorrectParametersNoCustomType() throws IOException, SAXException, EDIConfigurationException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-correct-no-custom-parameter.xml")));

        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment) edimap.getSegments().getSegments().get(0);
        Field field = segment.getFields().get(0);

        assertEquals(field.getTypeParameters().size(), 2, "Number of parameters in list [" + field.getTypeParameters().size() + "] doesn't match expected value [2].");

        String expected = "format";
        String value = field.getTypeParameters().get(0).getKey();
        assertEquals(value, expected, "Key in parameters [" + value + "] doesn't match expected value [" + expected + "].");

        expected = "yyyyMMdd";
        value = field.getTypeParameters().get(0).getValue();
        assertEquals(value, expected, "Value in parameters [" + value + "] doesn't match expected value [" + expected + "].");

        expected = "param2";
        value = field.getTypeParameters().get(1).getKey();
        assertEquals(value, expected, "Key in parameters [" + value + "] doesn't match expected value [" + expected + "].");

        expected = "value2";
        value = field.getTypeParameters().get(1).getValue();
        assertEquals(value, expected, "Value in parameters [" + value + "] doesn't match expected value [" + expected + "].");

    }

    @Test
    public void testIncorrectParametersNoCustomType() throws IOException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-incorrect-no-custom-parameter.xml")));

        try {
            EDIConfigDigester.digestConfig(input);
            assertTrue(false, "EDIConfigDigester should fail for test configuration.");
        } catch (EDIConfigurationException e) {
            String expected = "Invalid use of paramaters in ValueNode. A parameter-entry should consist of a key-value-pair separated with the '='-character. Example: [parameters=\"key1=value1;key2=value2\"]";
            assertEquals(e.getMessage(), expected, "Message in exception [" + e.getMessage() + "] doesn't match expected value [" + expected + "].");
        }
    }

    @Test
    public void testIncorrectParametersNoCustomType_ClassName() throws IOException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-incorrect-no-custom-parameter2.xml")));

        try {
            EDIConfigDigester.digestConfig(input);
            assertTrue(false, "EDIConfigDigester should fail for test configuration.");
        } catch (EDIConfigurationException e) {
            String expected = "When first parameter in list of parameters is not a key-value-pair the type of the ValueNode should be Custom.";
            assertEquals(e.getMessage(), expected, "Message in exception [" + e.getMessage() + "] doesn't match expected value [" + expected + "].");
        }
    }

    @Test
    public void testCorrectParametersCustomType() throws IOException, SAXException, EDIConfigurationException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-correct-custom-parameter.xml")));

        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment) edimap.getSegments().getSegments().get(0);
        Field field = segment.getFields().get(0);

        assertEquals(field.getTypeParameters().size(), 2, "Number of parameters in list [" + field.getTypeParameters().size() + "] doesn't match expected value [2].");

        String expected = "CustomClass";
        String value = field.getTypeParameters().get(0).getValue();
        assertEquals(StringToDateConverterFactory.class.getName(), value, "Value in parameters [" + value + "] doesn't match expected value [" + expected + "].");

        expected = "param1";
        value = field.getTypeParameters().get(1).getKey();
        assertEquals(value, expected, "Key in parameters [" + value + "] doesn't match expected value [" + expected + "].");

        expected = "value1";
        value = field.getTypeParameters().get(1).getValue();
        assertEquals(value, expected, "Value in parameters [" + value + "] doesn't match expected value [" + expected + "].");

    }

    @Test
    public void testIncorrectParametersCustomType_NoClassName() throws IOException, SAXException, EDIConfigurationException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-incorrect-custom-parameter.xml")));

        try {
            EDIConfigDigester.digestConfig(input);
            fail("Expected SmooksConfigurationException");
        } catch (SmooksConfigurationException e) {
            assertEquals("Mandatory property 'typeConverterClass' not specified.", e.getMessage());
        }
    }
}
