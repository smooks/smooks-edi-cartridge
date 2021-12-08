/*-
 * ========================LICENSE_START=================================
 * smooks-edg
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
package org.smooks.edi.edg;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.io.FileUtils;
import org.apache.daffodil.tdml.Runner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scala.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EdifactDfdlSchemaGeneratorTestCase {

    @BeforeEach
    public void beforeEach() throws IOException {
        FileUtils.deleteQuietly(new File("target/generated-test-resources"));
    }

    @Test
    public void testMain() throws Throwable {
        assertFalse(new File("target/generated-test-resources/d03b/EDIFACT-Interchange.dfdl.xsd").exists());
        assertFalse(new File("target/generated-test-resources/d03b/EDIFACT-Messages.dfdl.xsd").exists());
        assertFalse(new File("target/generated-test-resources/d03b/EDIFACT-Segments.dfdl.xsd").exists());

        EdifactDfdlSchemaGenerator.main(new String[]{"/d03b.zip,org.smooks.edi.ect.formats.unedifact.parser.UnEdifactDirectoryParser", "target/generated-test-resources"});

        assertTrue(new File("target/generated-test-resources/d03b/EDIFACT-Interchange.dfdl.xsd").exists());
        assertTrue(new File("target/generated-test-resources/d03b/EDIFACT-Messages.dfdl.xsd").exists());
        assertTrue(new File("target/generated-test-resources/d03b/EDIFACT-Segments.dfdl.xsd").exists());
    }

    @Test
    public void testDfdlSchema() throws Throwable {
        EdifactDfdlSchemaGenerator.main(new String[]{"/d03b.zip,org.smooks.edi.ect.formats.unedifact.parser.UnEdifactDirectoryParser", "target/generated-test-resources"});

        File generatedSchema = new File("target/generated-test-resources/d03b/EDIFACT-Interchange.dfdl.xsd");
        Mustache mustache = new DefaultMustacheFactory().compile("EDIFACT-Common/EDIFACT-Interchange.dfdl.xsd.mustache");
        try (FileWriter fileWriter = new FileWriter(generatedSchema)) {
            mustache.execute(fileWriter, new HashMap<String, Object>() {{
                this.put("schemaLocation", "EDIFACT-Messages.dfdl.xsd");
                this.put("messageTypes", Arrays.asList("INVOIC", "PAXLST"));
                this.put("version", "D03B");
            }});
        }

        Runner runner = Runner.apply("", "parse.tdml", true, true, false, Runner.defaultRoundTripDefaultDefault(), Runner.defaultValidationDefaultDefault(), Runner.defaultImplementationsDefaultDefault());
        runner.runOneTest("PAXLST", true);
        runner.runOneTest("INVOIC", true);
    }
}
