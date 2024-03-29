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
package org.smooks.edi.edisax.unedifact;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;

import org.smooks.assertion.AssertArgument;
import org.smooks.edi.edisax.BufferedSegmentReader;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.interchange.ControlBlockHandler;
import org.smooks.edi.edisax.interchange.ControlBlockHandlerFactory;
import org.smooks.edi.edisax.interchange.InterchangeContext;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.smooks.edi.edisax.registry.LazyMappingsRegistry;
import org.smooks.edi.edisax.registry.MappingsRegistry;
import org.smooks.edi.edisax.unedifact.handlers.r41.UNEdifact41ControlBlockHandlerFactory;
import org.smooks.namespace.NamespaceDeclarationStack;
import org.smooks.namespace.NamespaceDeclarationStackAware;
import org.smooks.xml.hierarchy.HierarchyChangeListener;
import org.smooks.xml.hierarchy.HierarchyChangeReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 * UN/EDIFACT Interchange Envelope parser.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNEdifactInterchangeParser implements XMLReader, NamespaceDeclarationStackAware, HierarchyChangeReader {

    private final Map<String, Boolean> features = new HashMap<String, Boolean>();

    public static final Delimiters defaultUNEdifactDelimiters = new Delimiters().setSegment("'").setField("+").setComponent(":").setEscape("?").setDecimalSeparator(".");

    /**
     * By default we are using {@link LazyMappingsRegistry} instance
     */
    protected MappingsRegistry registry = new LazyMappingsRegistry();
    private ContentHandler contentHandler;
    private HierarchyChangeListener hierarchyChangeListener;
    private InterchangeContext interchangeContext;
    private NamespaceDeclarationStack namespaceDeclarationStack;

    public void parse(InputSource unedifactInterchange) throws IOException, SAXException {
        AssertArgument.isNotNull(unedifactInterchange, "unedifactInterchange");

        if (contentHandler == null) {
            throw new IllegalStateException("'contentHandler' not set.  Cannot parse EDI stream.");
        }

        if (registry == null) {
            throw new IllegalStateException("'mappingsRegistry' not set.  Cannot parse EDI stream.");
        }

        boolean endDocument = false;
        try {
            ControlBlockHandlerFactory handlerFactory = new UNEdifact41ControlBlockHandlerFactory(hierarchyChangeListener);
            BufferedSegmentReader segmentReader = new BufferedSegmentReader(unedifactInterchange, defaultUNEdifactDelimiters);
            boolean validate = getFeature(EDIParser.FEATURE_VALIDATE);
            String segCode;

            segmentReader.mark();
            segmentReader.setIgnoreNewLines(getFeature(EDIParser.FEATURE_IGNORE_NEWLINES));

            contentHandler.startDocument();
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, ControlBlockHandlerFactory.ENVELOPE_PREFIX, XMLConstants.XMLNS_ATTRIBUTE + ":" + ControlBlockHandlerFactory.ENVELOPE_PREFIX, "CDATA", handlerFactory.getNamespace());
            String envElementQName = ControlBlockHandlerFactory.ENVELOPE_PREFIX + ":unEdifact";
            contentHandler.startElement(handlerFactory.getNamespace(), "unEdifact", envElementQName, attrs);

            while (true) {
                segCode = segmentReader.peek(3, true);
                if (segCode.length() == 3) {
                    interchangeContext = createInterchangeContext(segmentReader, validate, handlerFactory, namespaceDeclarationStack);
                    namespaceDeclarationStack = interchangeContext.getNamespaceDeclarationStack();

                    if (hierarchyChangeListener != null) {
                        hierarchyChangeListener.attachXMLReader(interchangeContext.getControlSegmentParser());
                    } else if (!interchangeContext.isContainerManagedNamespaceStack()) {
                        interchangeContext.getNamespaceDeclarationStack().pushReader(interchangeContext.getControlSegmentParser());
                    }

                    // Add the UN/EDIFACT namespace to the namespace stack...
                    namespaceDeclarationStack.pushNamespaces(envElementQName, handlerFactory.getNamespace(), attrs);

                    ControlBlockHandler handler = interchangeContext.getControlBlockHandler(segCode);

                    interchangeContext.indentDepth++;
                    handler.process(interchangeContext);
                    interchangeContext.indentDepth--;
                } else {
                    break;
                }
            }

            contentHandler.characters(new char[]{'\n'}, 0, 1);
            contentHandler.endElement(handlerFactory.getNamespace(), "unEdifact", envElementQName);
            endDocument = true;
        } finally {
            if (namespaceDeclarationStack != null) {
                namespaceDeclarationStack.popNamespaces();
                if (hierarchyChangeListener != null) {
                    hierarchyChangeListener.detachXMLReader();
                } else if (!interchangeContext.isContainerManagedNamespaceStack()) {
                    interchangeContext.getNamespaceDeclarationStack().popReader();
                }
            }
            if (endDocument) {
                contentHandler.endDocument();
            }
            contentHandler = null;
        }
    }

    protected InterchangeContext createInterchangeContext(BufferedSegmentReader segmentReader, boolean validate, ControlBlockHandlerFactory controlBlockHandlerFactory, NamespaceDeclarationStack namespaceDeclarationStack) {
        return new InterchangeContext(segmentReader, registry, contentHandler, getFeatures(), controlBlockHandlerFactory, namespaceDeclarationStack, validate);
    }

    public InterchangeContext getInterchangeContext() {
        return interchangeContext;
    }

    /**
     * Set the EDI mapping model to be used in all subsequent parse operations.
     * <br><br>
     * The model can be generated through a call to the {@link EDIParser}.
     *
     * @param registry The mapping model registry.
     * @return This parser instance.
     */
    public UNEdifactInterchangeParser setMappingsRegistry(MappingsRegistry registry) {
        AssertArgument.isNotNull(registry, "mappingsRegistry");
        this.registry = registry;
        return this;
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public void ignoreNewLines(boolean ignoreNewLines) {
        setFeature(EDIParser.FEATURE_IGNORE_NEWLINES, ignoreNewLines);
    }

    public void ignoreEmptyNodes(boolean ignoreEmptyNodes) {
        setFeature(EDIParser.FEATURE_IGNORE_EMPTY_NODES, ignoreEmptyNodes);
    }

    public void validate(boolean validate) {
        setFeature(EDIParser.FEATURE_VALIDATE, validate);
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public void setFeature(String name, boolean value) {
        features.put(name, value);
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        Boolean feature = features.get(name);
        if (feature == null) {
            return false;
        }
        return feature;
    }

    public void setNamespaceDeclarationStack(NamespaceDeclarationStack namespaceDeclarationStack) {
        this.namespaceDeclarationStack = namespaceDeclarationStack;
    }

    public void setHierarchyChangeListener(HierarchyChangeListener listener) {
        this.hierarchyChangeListener = listener;
    }

    /****************************************************************************
     *
     * The following methods are currently unimplemnted...
     *
     ****************************************************************************/

    public void parse(String systemId) throws IOException, SAXException {
        throw new UnsupportedOperationException("Operation not supports by this reader.");
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public void setDTDHandler(DTDHandler arg0) {
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public void setEntityResolver(EntityResolver arg0) {
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setErrorHandler(ErrorHandler arg0) {
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
    }
}
