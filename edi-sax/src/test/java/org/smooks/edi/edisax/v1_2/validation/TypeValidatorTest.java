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
package org.smooks.edi.edisax.v1_2.validation;

/**
 * Tests validation of type in ValueNode.
 * @author bardl 
 */
public class TypeValidatorTest {

    public void test() {

    }

//    public void test_type_String_valid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.String;
//
//        String value = "testing123";
//        assertTrue("The value [" + value + "] should be a valid String.", ediType.validateType(value, null));
//    }
//
//    public void test_type_String_invalid() throws IOException {
//        //Can't think of any invalid cases.
//    }
//
//    public void test_type_Numeric_valid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Numeric;
//
//        String value = "123";
//        assertTrue("The value [" + value + "] should be a valid Numeric.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("fomat", "#0.00"))));
//
//        value = "123.00";
//        assertTrue("The value [" + value + "] should be a valid Numeric.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("fomat", "#0.00"))));
//    }
//
//    public void test_type_Numeric_invalid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Numeric;
//
//        String value = "12A3";
//        assertFalse("The value [" + value + "] should not be a valid Numeric.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("fomat", "#0.00"))));
//    }
//
//    public void test_type_Decimal_valid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Decimal;
//
//        String value = "123";
//        assertTrue("The value [" + value + "] should be a valid Numeric.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("fomat", "#0.00"))));
//
//        value = "123.00";
//        assertTrue("The value [" + value + "] should be a valid Numeric.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("format", "#0.00"))));
//    }
//
//    public void test_type_Decimal_invalid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Date;
//
//        String value = "12A3";
//        assertFalse("The value [" + value + "] should not be a valid Numeric.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("format", "#0.00"))));
//    }
//
//    public void test_type_Date_valid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Date;
//
//        String value = "20090401";
//        String format = "yyyyMMdd";
//        assertTrue("The value [" + value + "] with format [" + format + "] should be a valid Date.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("format", format))));
//    }
//
//    public void test_type_Date_invalid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Date;
//
//        String value = "200908bb";
//        String format = "yyyyMMdd";
//        assertFalse("The value [" + value + "] with format [" + format + "] should not be a valid Date.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("format", format))));
//    }
//
//    public void test_type_Time_valid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Time;
//
//        String value = "2251";
//        String format = "HHmm";
//        assertTrue("The value [" + value + "] with format [" + format + "] should be a valid Time.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("format", format))));
//    }
//
//    public void test_type_Time_invalid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Time;
//
//        String value = "22s";
//        String format = "HHmm";
//        assertFalse("The value [" + value + "] with format [" + format + "] should not be a valid Time.", ediType.validateType(value, buildParameterList(new ParamEntry<String,String>("format", format))));
//    }
//
//    public void test_type_Binary_valid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Binary;
//
//        String value = "0101010101111000";
//        assertTrue("The value [" + value + "] should be a valid binary sequence.", ediType.validateType(value, null));
//    }
//
//    public void test_type_Binary_invalid() throws IOException {
//        EDITypeEnum ediType = EDITypeEnum.Binary;
//
//        String value = "0101001200";
//        assertFalse("The value [" + value + "] should not be a valid binary sequence.", ediType.validateType(value, null));
//    }
//
//    private List<Map.Entry<String, String>> buildParameterList(Map.Entry<String, String>... entries) {
//        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
//        for (Map.Entry<String, String> entry : entries) {
//            list.add(entry);
//        }
//        return list;
//    }
}
