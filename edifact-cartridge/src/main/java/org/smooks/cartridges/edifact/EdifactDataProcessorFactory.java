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

import com.ctc.wstx.util.StringUtil;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ValidationMode;
import org.apache.daffodil.util.Misc;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cartridges.edi.EdiDataProcessorFactory;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.container.ApplicationContext;
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class EdifactDataProcessorFactory extends EdiDataProcessorFactory {

    private static final Mustache MUSTACHE;

    static {
        try {
            MUSTACHE = new DefaultMustacheFactory().compile("EDIFACT-Common/EDIFACT-Interchange.dfdl.xsd.mustache");
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    @Inject
    protected ApplicationContext applicationContext;

    @Override
    public DataProcessor doCreateDataProcessor(final Map<String, String> variables) {
        try {
            final Parameter<String> schemaURIParameter = smooksResourceConfiguration.getParameter("schemaURI", String.class);
            final String version = readVersion(schemaURIParameter);
            final URI schema;

            final List<Parameter> messageTypeParameters = smooksResourceConfiguration.getParameters("messageType");
            if (messageTypeParameters == null || messageTypeParameters.isEmpty()) {
                schema = new URI(version.toLowerCase() + "/EDIFACT-Interchange.dfdl.xsd");
            } else {
                final List<String> messageTypes = (List) messageTypeParameters.stream().map(m -> m.getValue()).collect(Collectors.toList());
                
                StringBuilder aSB = new StringBuilder ();
                for (String s : messageTypes)
                  aSB.append (s);
                final byte[] md = MessageDigest.getInstance ("MD5").digest (aSB.toString ().getBytes (StandardCharsets.UTF_8));
                aSB = new StringBuilder (md.length * 2);
                for (byte b : md)
                  aSB.append (Character.forDigit ((b & 0xf0) >> 4, 16)).append (Character.forDigit (b & 0x0f, 16));
                
                // TODO find correct directory
                final File generatedSchema = new File (version.toLowerCase() + "/EDIFACT-Interchange-"+aSB.toString ().toLowerCase (Locale.ROOT)+".dfdl.xsd");
                generatedSchema.getParentFile ().mkdirs ();

                try (FileWriter fileWriter = new FileWriter(generatedSchema)) {
                    MUSTACHE.execute(fileWriter, new HashMap<String, Object>() {{
                        this.put("schemaLocation", schemaURIParameter.getValue());
                        this.put("messageTypes", messageTypes);
                        this.put("version", version);
                    }});
                }

                schema = generatedSchema.toURI();
            }

            final DfdlSchema dfdlSchema = new DfdlSchema(schema, variables, ValidationMode.valueOf(smooksResourceConfiguration.getParameterValue("validationMode", String.class, "Off")), Boolean.parseBoolean(smooksResourceConfiguration.getParameterValue("cacheOnDisk", String.class, "false")), Boolean.parseBoolean(smooksResourceConfiguration.getParameterValue("debugging", String.class, "false"))) {
                @Override
                public String getName() {
                    return schemaURIParameter.getValue() + ":" + getValidationMode() + ":" + isCacheOnDisk() + ":" + isDebugging() + ":" + variables.toString();
                }
            };

            return compileOrGet(dfdlSchema);
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
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
