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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ValidationMode;
import org.apache.daffodil.util.Misc;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cartridges.edi.EdiDataProcessorFactory;
import org.smooks.api.resource.config.Parameter;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EdifactDataProcessorFactory extends EdiDataProcessorFactory {

    private static final Mustache MUSTACHE;

    static {
        try {
            MUSTACHE = new DefaultMustacheFactory().compile("EDIFACT-Common/EDIFACT-Interchange.dfdl.xsd.mustache");
        } catch (Throwable t) {
            throw new SmooksConfigException(t);
        }
    }

    @Inject
    protected ApplicationContext applicationContext;

    @Override
    public DataProcessor doCreateDataProcessor(final Map<String, String> variables) {
        try {
            final Parameter<String> schemaUriParameter = resourceConfig.getParameter("schemaURI", String.class);
            final String version = readVersion(schemaUriParameter);
            final URI entrySchemaUri;

            final List<Parameter<?>> messageTypeParameters = resourceConfig.getParameters("messageType");
            if (messageTypeParameters == null || messageTypeParameters.isEmpty()) {
                entrySchemaUri = new URI(version.toLowerCase() + "/EDIFACT-Interchange.dfdl.xsd");
            } else {
                final List<String> messageTypes = (List) messageTypeParameters.stream().map(Parameter::getValue).collect(Collectors.toList());
                entrySchemaUri = materialiseEntrySchema(schemaUriParameter.getValue(), messageTypes, version);
            }

            final DfdlSchema dfdlSchema = new DfdlSchema(entrySchemaUri, variables, ValidationMode.valueOf(resourceConfig.getParameterValue("validationMode", String.class, "Off")), Boolean.parseBoolean(resourceConfig.getParameterValue("cacheOnDisk", String.class, "false")), Boolean.parseBoolean(resourceConfig.getParameterValue("debugging", String.class, "false")), null) {
                @Override
                public String getName() {
                    return schemaUriParameter.getValue() + ":" + getValidationMode() + ":" + isCacheOnDisk() + ":" + isDebugging() + ":" + variables.toString();
                }
            };

            return compileOrGet(dfdlSchema);
        } catch (Throwable t) {
            throw new SmooksConfigException(t);
        }
    }
    
    protected URI materialiseEntrySchema(final String schemaUri, final List<String> messageTypes, final String version) throws IOException {
        final File generatedEntrySchemaDir = Files.createTempDirectory(null).toFile();
        generatedEntrySchemaDir.deleteOnExit();
        final File generatedEntrySchema = new File(generatedEntrySchemaDir + "/EDIFACT-Interchange-" + UUID.nameUUIDFromBytes(String.join(":", messageTypes).getBytes()) + ".dfdl.xsd");
        try (Writer fileWriter = new OutputStreamWriter(new FileOutputStream(generatedEntrySchema), StandardCharsets.UTF_8)) {
            MUSTACHE.execute(fileWriter, new HashMap<String, Object>() {{
                this.put("schemaLocation", schemaUri);
                this.put("messageTypes", messageTypes);
                this.put("version", version);
            }});
        }
        
        return generatedEntrySchema.toURI();
    }

    protected String readVersion(final Parameter<String> schemaURIParameter) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = documentBuilder.parse(Misc.getRequiredResource(schemaURIParameter.getValue()).toString());

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();

        return (String) xpath.compile("/schema/annotation/appinfo[@source='http://www.ibm.com/dfdl/edi/un/edifact']/text()").evaluate(document, XPathConstants.STRING);
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
