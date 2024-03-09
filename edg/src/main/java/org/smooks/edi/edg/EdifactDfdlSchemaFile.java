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

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class EdifactDfdlSchemaFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdifactDfdlSchemaFile.class);
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private final String versionOutputDirectory;
    private String segmentsSchema;
    private String messagesSchema;
    private String interchangeSchema;

    public EdifactDfdlSchemaFile(String versionOutputDirectory) {
        this.versionOutputDirectory = versionOutputDirectory;
    }

    public void withSegmentsSchema(String segmentsSchema) {
        this.segmentsSchema = segmentsSchema;
    }

    public void withMessagesSchema(String messagesSchema) {
        this.messagesSchema = messagesSchema;
    }

    public void withInterchangeSchema(String interchangeSchema) {
        this.interchangeSchema = interchangeSchema;
    }

    public void write() throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        new File(versionOutputDirectory).mkdirs();

        if (segmentsSchema != null) {
            write(segmentsSchema, new File(versionOutputDirectory + "/EDIFACT-Segments.dfdl.xsd"));
        }

        if (messagesSchema != null) {
            final File messageSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Messages.dfdl.xsd");
            write(messagesSchema, messageSchemaFile);
            LOGGER.info("Validating schema {}...", messageSchemaFile.getPath());
            compileSchema(messageSchemaFile);
        }

        if (interchangeSchema != null) {
            write(interchangeSchema, new File(versionOutputDirectory + "/EDIFACT-Interchange.dfdl.xsd"));
        }
    }

    protected void write(final String xml, final File file) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
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

    protected void stripWhitespaceTextNodes(final Document document) throws XPathExpressionException {
        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
        final NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(document, XPathConstants.NODESET);

        for (int i = 0; i < emptyTextNodes.getLength(); i++) {
            final Node emptyTextNode = emptyTextNodes.item(i);
            emptyTextNode.getParentNode().removeChild(emptyTextNode);
        }
    }

    protected void compileSchema(final File messageSchemaFile) throws IOException {
        final ProcessorFactory processorFactory = Daffodil.compiler().compileFile(messageSchemaFile);
        for (Diagnostic diagnostic : processorFactory.getDiagnostics()) {
            if (diagnostic.isError()) {
                throw new EdifactDfdlSchemaGeneratorException(diagnostic.getSomeCause());
            }
        }
    }
}
