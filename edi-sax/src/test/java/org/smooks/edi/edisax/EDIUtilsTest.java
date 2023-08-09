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

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.model.internal.DelimiterType;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.smooks.edi.edisax.unedifact.UNEdifactInterchangeParser;
import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.edi.edisax.util.IllegalNameException;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bardl
 */
public class EDIUtilsTest {

    @Test
    public void test_with_escape() throws IOException, SAXException {
        String[] test = EDIUtils.split("first?::second??:third", ":", "?");
        String[] expected = new String[]{"first:", "second??", "third"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        test = EDIUtils.split("ATS+hep:iee+hai??+kai=haikai+slut", "+", "?");
        expected = new String[]{"ATS", "hep:iee", "hai??", "kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        test = EDIUtils.split("ATS+hep:iee+hai?#?#+kai=haikai+slut", "+", "?#");
        expected = new String[]{"ATS", "hep:iee", "hai?#?#", "kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        test = EDIUtils.split("ATS+#hep:iee+#hai?#?#+#kai=haikai+#slut", "+#", "?#");
        expected = new String[]{"ATS", "hep:iee", "hai?#?#", "kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        test = EDIUtils.split("ATS+#hep:iee+#hai??+#kai=haikai+#slut", "+#", "?");
        expected = new String[]{"ATS", "hep:iee", "hai??", "kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        test = EDIUtils.split("ATS+#hep:iee+#hai??+#kai=haikai+#slut", "+#", null);
        expected = new String[]{"ATS", "hep:iee", "hai??", "kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        // Test restarting escape sequence within escape sequence.
        test = EDIUtils.split("ATS+hep:iee+hai??#+kai=haikai+slut", "+", "?#");
        expected = new String[]{"ATS", "hep:iee", "hai?+kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

        // Test restarting delimiter sequence within delimiter sequence.
        test = EDIUtils.split("ATS++#hep:iee+#hai?+#kai=haikai+#slut", "+#", "?");
        expected = new String[]{"ATS+", "hep:iee", "hai+#kai=haikai", "slut"};
        assertTrue(equal(test, expected), "Result is [" + output(test) + "] should be [" + output(expected) + "] ");

    }

    @Test
    public void test_without_escape() {
        String[] result = EDIUtils.split(null, "*", null);
        assertTrue(result == null, "Result is [" + output(result) + "] should be [null] ");

        result = EDIUtils.split("", null, null);
        String[] expected = new String[0];
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("abc def", null, null);
        expected = new String[]{"abc", "def"};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("abc def", " ", null);
        expected = new String[]{"abc", "def"};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("abc  def", " ", null);
        expected = new String[]{"abc", "", "def"};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("ab:cd:ef", ":", null);
        expected = new String[]{"ab", "cd", "ef"};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("ab:cd:ef:", ":", null);
        expected = new String[]{"ab", "cd", "ef", ""};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("ab:cd:ef::", ":", null);
        expected = new String[]{"ab", "cd", "ef", "", ""};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split(":cd:ef", ":", null);
        expected = new String[]{"", "cd", "ef"};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split("::cd:ef", ":", null);
        expected = new String[]{"", "", "cd", "ef"};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");

        result = EDIUtils.split(":cd:ef:", ":", null);
        expected = new String[]{"", "cd", "ef", ""};
        assertTrue(equal(result, expected), "Result is [" + output(result) + "] should be [" + output(expected) + "] ");


    }

    @Test
    public void test_concatAndTruncate() {
        Delimiters delims = UNEdifactInterchangeParser.defaultUNEdifactDelimiters;

        assertEquals("ab", EDIUtils.concatAndTruncate(Stream.of("a", "b", "+:+").collect(Collectors.toList()), DelimiterType.SEGMENT, delims));
        assertEquals("a+:+b", EDIUtils.concatAndTruncate(Stream.of("a", "+:+", "b", "+:+").collect(Collectors.toList()), DelimiterType.SEGMENT, delims));
        assertEquals("a+:+bc+:+", EDIUtils.concatAndTruncate(Stream.of("a", "+:+", "b", "c+:+").collect(Collectors.toList()), DelimiterType.SEGMENT, delims));

        assertEquals("ab", EDIUtils.concatAndTruncate(Stream.of("a", "b", "+:+").collect(Collectors.toList()), DelimiterType.FIELD, delims));
        assertEquals("ab+:+'", EDIUtils.concatAndTruncate(Stream.of("a", "b", "+:+'").collect(Collectors.toList()), DelimiterType.FIELD, delims));
        assertEquals("ab+:+", EDIUtils.concatAndTruncate(Stream.of("a", "b", "+:+").collect(Collectors.toList()), DelimiterType.COMPONENT, delims));
    }

    @Test
    public void testEncodeClassName() throws IllegalNameException {
        assertEquals("Address", EDIUtils.encodeClassName("ADDRESS"));
        assertEquals("CustomerAddress", EDIUtils.encodeClassName("CUSTOMER_ADDRESS"));
        assertEquals("CustomerADDRESS", EDIUtils.encodeClassName("Customer_ADDRESS"));
        assertEquals("CustomerAddress", EDIUtils.encodeClassName("Customer_address"));
        assertEquals("Default", EDIUtils.encodeClassName("default"));
        assertEquals("_1CustomerAddressPOBox", EDIUtils.encodeClassName("1CustomerAddressP.O.Box"));
    }

    @Test
    public void testEncodeAttribute() throws IllegalNameException {
        assertEquals("address", EDIUtils.encodeAttributeName("ADDRESS"));
        assertEquals("addRESS", EDIUtils.encodeAttributeName("addRESS"));
        assertEquals("addRESS", EDIUtils.encodeAttributeName("AddRESS"));
        assertEquals("orderId", EDIUtils.encodeAttributeName("orderId"));
        assertEquals("orderId", EDIUtils.encodeAttributeName("order_id"));
        assertEquals("_default", EDIUtils.encodeAttributeName("default"));
        assertEquals("_package", EDIUtils.encodeAttributeName("package"));
        assertEquals("_package", EDIUtils.encodeAttributeName("Package"));
        assertEquals("_1address", EDIUtils.encodeAttributeName("1ADDRESS"));
        assertEquals("_1addressPOBox", EDIUtils.encodeAttributeName("_1addressP.O.Box"));
    }

    private String output(String[] value) {
        if (value == null) {
            return null;
        }

        String result = "{";
        String str;
        for (int i = 0; i < value.length; i++) {
            str = value[i];
            result += "\"" + str + "\"";
            if (i != value.length - 1) {
                result += ", ";
            }
        }
        result += "}";
        return result;
    }

    private static boolean equal(String[] test, String[] expected) {
        if (test.length != expected.length) {
            return false;
        }

        for (int i = 0; i < test.length; i++) {
            if (!test[i].equals(expected[i])) {
                return false;
            }
        }
        return true;
    }
}
