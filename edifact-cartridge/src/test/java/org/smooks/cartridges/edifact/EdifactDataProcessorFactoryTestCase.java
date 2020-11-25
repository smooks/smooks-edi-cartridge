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

import org.apache.daffodil.japi.DataProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cdr.ResourceConfig;
import org.smooks.container.standalone.DefaultApplicationContextBuilder;
import org.smooks.io.FileUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class EdifactDataProcessorFactoryTestCase {
    
    @BeforeEach
    public void beforeEach() {
        File workingDir = new File(DfdlSchema.WORKING_DIRECTORY);
        if (workingDir.exists()) {
            FileUtils.deleteDir(workingDir);
        }
    }

    @AfterEach
    public void afterEach() {
        File workingDir = new File(DfdlSchema.WORKING_DIRECTORY);
        if (workingDir.exists()) {
            FileUtils.deleteDir(workingDir);
        }
    }
    
    @Test
    public void testDoCreateDataProcessorGivenNonUtf8FileEncodingDoesNotThrowMalformedInputException() throws NoSuchFieldException, IllegalAccessException {
        EdifactDataProcessorFactory edifactDataProcessorFactory = new EdifactDataProcessorFactory();
        edifactDataProcessorFactory.setResourceConfig(new ResourceConfig());
        edifactDataProcessorFactory.setApplicationContext(new DefaultApplicationContextBuilder().build());
        edifactDataProcessorFactory.getResourceConfig().setParameter("schemaURI", "/d03b/EDIFACT-Messages.dfdl.xsd");
        edifactDataProcessorFactory.getResourceConfig().setParameter("messageType", "ORDERS");

        String originalFileEncoding = System.getProperty("file.encoding");
        System.setProperty("file.encoding", "iso-8859-1");
        Field defaultCharsetField = Charset.class.getDeclaredField("defaultCharset");
        defaultCharsetField.setAccessible(true);
        defaultCharsetField.set(null, null);

        assertEquals(StandardCharsets.ISO_8859_1, Charset.defaultCharset());
        DataProcessor dataProcessor = edifactDataProcessorFactory.doCreateDataProcessor(new HashMap<>());
        assertNotNull(dataProcessor);

        System.setProperty("file.encoding", originalFileEncoding);
        defaultCharsetField.set(null, null);
    }

    @Test
    public void testNextDoCreateDataProcessorIsCacheHitGivenIdenticalMessageType() {
        EdifactDataProcessorFactory edifactDataProcessorFactory = new EdifactDataProcessorFactory();
        edifactDataProcessorFactory.setResourceConfig(new ResourceConfig());
        edifactDataProcessorFactory.setApplicationContext(new DefaultApplicationContextBuilder().build());
        edifactDataProcessorFactory.getResourceConfig().setParameter("cacheOnDisk", "true");
        edifactDataProcessorFactory.getResourceConfig().setParameter("schemaURI", "/d03b/EDIFACT-Messages.dfdl.xsd");
        edifactDataProcessorFactory.getResourceConfig().setParameter("messageType", "ORDERS");
        
        assertFalse(new File(DfdlSchema.WORKING_DIRECTORY).exists());
        edifactDataProcessorFactory.doCreateDataProcessor(new HashMap<>());
        assertEquals(1, new File(DfdlSchema.WORKING_DIRECTORY).listFiles().length);
        
        EdifactDataProcessorFactory cachedEdifactDataProcessorFactory = new EdifactDataProcessorFactory();
        cachedEdifactDataProcessorFactory.setResourceConfig(new ResourceConfig());
        cachedEdifactDataProcessorFactory.setApplicationContext(new DefaultApplicationContextBuilder().build());
        cachedEdifactDataProcessorFactory.getResourceConfig().setParameter("cacheOnDisk", "true");
        cachedEdifactDataProcessorFactory.getResourceConfig().setParameter("schemaURI", "/d03b/EDIFACT-Messages.dfdl.xsd");
        cachedEdifactDataProcessorFactory.getResourceConfig().setParameter("messageType", "ORDERS");

        assertEquals(1, new File(DfdlSchema.WORKING_DIRECTORY).listFiles().length);
        cachedEdifactDataProcessorFactory.doCreateDataProcessor(new HashMap<>());
        assertEquals(1, new File(DfdlSchema.WORKING_DIRECTORY).listFiles().length);
    }

    @Test
    public void testNextDoCreateDataProcessorIsCacheMissGivenDifferentMessageType() {
        EdifactDataProcessorFactory edifactDataProcessorFactory = new EdifactDataProcessorFactory();
        edifactDataProcessorFactory.setResourceConfig(new ResourceConfig());
        edifactDataProcessorFactory.setApplicationContext(new DefaultApplicationContextBuilder().build());
        edifactDataProcessorFactory.getResourceConfig().setParameter("cacheOnDisk", "true");
        edifactDataProcessorFactory.getResourceConfig().setParameter("schemaURI", "/d03b/EDIFACT-Messages.dfdl.xsd");
        edifactDataProcessorFactory.getResourceConfig().setParameter("messageType", "ORDERS");

        assertFalse(new File(DfdlSchema.WORKING_DIRECTORY).exists());
        edifactDataProcessorFactory.doCreateDataProcessor(new HashMap<>());
        assertEquals(1, new File(DfdlSchema.WORKING_DIRECTORY).listFiles().length);

        EdifactDataProcessorFactory cachedEdifactDataProcessorFactory = new EdifactDataProcessorFactory();
        cachedEdifactDataProcessorFactory.setResourceConfig(new ResourceConfig());
        cachedEdifactDataProcessorFactory.setApplicationContext(new DefaultApplicationContextBuilder().build());
        cachedEdifactDataProcessorFactory.getResourceConfig().setParameter("cacheOnDisk", "true");
        cachedEdifactDataProcessorFactory.getResourceConfig().setParameter("schemaURI", "/d03b/EDIFACT-Messages.dfdl.xsd");
        cachedEdifactDataProcessorFactory.getResourceConfig().setParameter("messageType", "INVOIC");

        assertEquals(1, new File(DfdlSchema.WORKING_DIRECTORY).listFiles().length);
        cachedEdifactDataProcessorFactory.doCreateDataProcessor(new HashMap<>());
        assertEquals(2, new File(DfdlSchema.WORKING_DIRECTORY).listFiles().length);
    }
}