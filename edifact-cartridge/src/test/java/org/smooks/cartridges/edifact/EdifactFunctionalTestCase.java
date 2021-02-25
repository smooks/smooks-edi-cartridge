/*-
 * ========================LICENSE_START=================================
 * smooks-edifact-cartridge
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
package org.smooks.cartridges.edifact;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.smooks.Smooks;
import org.smooks.io.StreamUtils;
import org.xmlunit.builder.DiffBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.smooks.support.SmooksUtil.filterAndSerialize;

public class EdifactFunctionalTestCase {

    private Smooks smooks;

    @BeforeEach
    public void beforeEach() {
        smooks = new Smooks();
    }

    @AfterEach
    public void afterEach() {
        smooks.close();
    }

    @ParameterizedTest
    @CsvSource({"/data/INVOIC_D.03B_Interchange_with_UNA.txt, /data/INVOIC_D.03B_Interchange_with_UNA.xml", "/data/ORDERS_D.03B_Interchange.txt, /data/ORDERS_D.03B_Interchange.xml"})
    public void testSmooksConfigGivenParser(String fileName, String expectedResult) throws Exception {
        smooks.addConfigurations("/smooks-parser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream(fileName), smooks);

        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream(expectedResult)).ignoreWhitespace().withTest(result).build().hasDifferences());
    }

    @ParameterizedTest
    @CsvSource({"/data/INVOIC_D.03B_Interchange_with_UNA.xml, /data/INVOIC_D.03B_Interchange_with_UNA.txt", "/data/ORDERS_D.03B_Interchange.xml, /data/ORDERS_D.03B_Interchange.txt"})
    public void testSmooksConfigGivenUnparser(String fileName, String expectedResult) throws Exception {
        smooks.addConfigurations("/smooks-unparser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream(fileName), smooks);

        assertEquals(StreamUtils.readStreamAsString(getClass().getResourceAsStream(expectedResult), "UTF-8").replaceAll("\\n", "\r\n"), result);
    }
}
