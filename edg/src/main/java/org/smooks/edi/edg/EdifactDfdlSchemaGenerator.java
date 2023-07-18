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

import org.apache.daffodil.io.processors.charset.CharsetUtils;
import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ProcessorFactory;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edg.template.InterchangeTemplate;
import org.smooks.edi.edg.template.MessagesTemplate;
import org.smooks.edi.edg.template.SegmentsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

public final class EdifactDfdlSchemaGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdifactDfdlSchemaGenerator.class);
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();


    private EdifactDfdlSchemaGenerator() {

    }

    public static void main(final String[] args) {
        //FIXME: https://issues.apache.org/jira/browse/DAFFODIL-2827
        CharsetUtils.supportedEncodingsString();
        Arrays.stream(Arrays.copyOfRange(args, 0, args.length - 1)).parallel().forEach(a -> {
            final String[] argAsArray = a.split(",");
            final String directoryPath = argAsArray[0];
            final String directoryParserImpl = argAsArray[1];

            LOGGER.info("Generating schema from {}...", directoryPath);
            try {
                generateDfdlSchemas(directoryPath, directoryParserImpl, args[args.length - 1]);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void generateDfdlSchemas(final String directoryPath, final String directoryParserImpl, final String outputDirectory) throws Throwable {
        final InputStream resourceAsStream = EdifactDfdlSchemaGenerator.class.getResourceAsStream(directoryPath);
        final Constructor<?> directoryParserClassConstructor = Class.forName(directoryParserImpl).getConstructor(ZipInputStream.class, boolean.class, boolean.class);
        final DirectoryParser directoryParser = (DirectoryParser) directoryParserClassConstructor.newInstance(new ZipInputStream(resourceAsStream), true, true);

        final Edimap edimap = UnEdifactDefinitionReader.parse(directoryParser);
        final String[] namespace = edimap.getDescription().getNamespace().split(":");
        final String version = namespace[3].replace("-", "").toUpperCase();
        final String versionOutputDirectory = outputDirectory + "/" + version.toLowerCase();

        new File(versionOutputDirectory).mkdirs();

        final File segmentSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Segments.dfdl.xsd");
        if (segmentSchemaFile.exists()) {
            LOGGER.info("Skipping existing schema " + segmentSchemaFile.getAbsolutePath());
        } else {
            final String segmentsSchema = new SegmentsTemplate(version, edimap).materialise();
            write(segmentsSchema, segmentSchemaFile);
        }

        final MessagesTemplate messagesTemplate = new MessagesTemplate(version, directoryParser, edimap);
        final File messageSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Messages.dfdl.xsd");
        if (messageSchemaFile.exists()) {
            LOGGER.info("Skipping existing schema " + messageSchemaFile.getAbsolutePath());
        } else {
            final String messagesSchema = messagesTemplate.materialise();
            write(messagesSchema, messageSchemaFile);

            LOGGER.info("Validating schema {}...", messageSchemaFile.getPath());
            final ProcessorFactory processorFactory = Daffodil.compiler().compileFile(messageSchemaFile);
            for (Diagnostic diagnostic : processorFactory.getDiagnostics()) {
                if (diagnostic.isError()) {
                    throw diagnostic.getSomeCause();
                }
            }
        }

        final File interchangeSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Interchange.dfdl.xsd");
        if (interchangeSchemaFile.exists()) {
            LOGGER.info("Skipping existing schema " + interchangeSchemaFile.getAbsolutePath());
        } else {
            String interchangeSchema = new InterchangeTemplate(version, messagesTemplate.getMessageTypes()).materialise();
            write(interchangeSchema, interchangeSchemaFile);
        }
    }

    private static void write(final String xml, final File file) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        final DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        final Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
        stripWhitespaceTextNodes(document);

        final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");

        try (FileWriter fileWriter = new FileWriter(file)) {
            transformer.transform(new DOMSource(document), new StreamResult(fileWriter));
        }
    }

    private static void stripWhitespaceTextNodes(final Document document) throws XPathExpressionException {
        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
        final NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(document, XPathConstants.NODESET);

        for (int i = 0; i < emptyTextNodes.getLength(); i++) {
            final Node emptyTextNode = emptyTextNodes.item(i);
            emptyTextNode.getParentNode().removeChild(emptyTextNode);
        }
    }
}
