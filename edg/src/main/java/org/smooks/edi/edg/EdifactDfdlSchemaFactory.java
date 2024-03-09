/*-
 * ========================LICENSE_START=================================
 * smooks-edg
 * %%
 * Copyright (C) 2020 - 2024 Smooks
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edg.template.InterchangeTemplate;
import org.smooks.edi.edg.template.MessagesTemplate;
import org.smooks.edi.edg.template.SegmentsTemplate;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipInputStream;

public class EdifactDfdlSchemaFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdifactDfdlSchemaFactory.class);
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    public EdifactDfdlSchemaFile create(final String directoryPath, final String directoryParserImpl, final String outputDirectory) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, IOException, EdiParseException, XPathExpressionException, ParserConfigurationException, SAXException {
        final InputStream resourceAsStream = EdifactDfdlSchemaGenerator.class.getResourceAsStream(directoryPath);
        final Constructor<?> directoryParserClassConstructor = Class.forName(directoryParserImpl).getConstructor(ZipInputStream.class, boolean.class, boolean.class);
        final DirectoryParser directoryParser = (DirectoryParser) directoryParserClassConstructor.newInstance(new ZipInputStream(resourceAsStream), true, true);

        final Edimap edimap = UnEdifactDefinitionReader.parse(directoryParser);
        final String[] namespace = edimap.getDescription().getNamespace().split(":");
        final String version = namespace[3].replace("-", "").toUpperCase();
        assertMessageReleaseNoEnum(version);
        final String versionOutputDirectory = outputDirectory + "/" + version.toLowerCase();

        final File segmentSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Segments.dfdl.xsd");
        EdifactDfdlSchemaFile edifactDfdlSchemaFile = new EdifactDfdlSchemaFile(versionOutputDirectory);
        if (segmentSchemaFile.exists()) {
            LOGGER.info("Skipping existing schema " + segmentSchemaFile.getAbsolutePath());
        } else {
            edifactDfdlSchemaFile.withSegmentsSchema(new SegmentsTemplate(version, edimap).materialise());
        }

        final MessagesTemplate messagesTemplate = new MessagesTemplate(version, directoryParser, edimap);
        final File messageSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Messages.dfdl.xsd");
        if (messageSchemaFile.exists()) {
            LOGGER.info("Skipping existing schema " + messageSchemaFile.getAbsolutePath());
        } else {
            edifactDfdlSchemaFile.withMessagesSchema(messagesTemplate.materialise());
        }

        final File interchangeSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Interchange.dfdl.xsd");
        if (interchangeSchemaFile.exists()) {
            LOGGER.info("Skipping existing schema " + interchangeSchemaFile.getAbsolutePath());
        } else {
            edifactDfdlSchemaFile.withInterchangeSchema(new InterchangeTemplate(version, messagesTemplate.getMessageTypes()).materialise());
        }

        return edifactDfdlSchemaFile;
    }

    protected void assertMessageReleaseNoEnum(final String version) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        final InputStream edifactServiceSegmentsDfdlSchema = EdifactDfdlSchemaGenerator.class.getResourceAsStream("/EDIFACT-Common/EDIFACT-Service-Segments-4.1.dfdl.xsd");
        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final String messageReleaseNo = version.substring(1);
        final XPathExpression xpathExp = xpathFactory.newXPath().compile(String.format("/schema/simpleType[@name=\"E0054-MessageReleaseNumber\"]/restriction/enumeration[@value=\"%s\"]", messageReleaseNo));
        final Node enumeration = (Node) xpathExp.evaluate(DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(edifactServiceSegmentsDfdlSchema), XPathConstants.NODE);
        if (enumeration == null) {
            throw new EdifactDfdlSchemaGeneratorException(String.format("Message release number for %s does not exist in EDIFACT-Service-Segments-4.1.dfdl.xsd", messageReleaseNo));
        }
    }
}
