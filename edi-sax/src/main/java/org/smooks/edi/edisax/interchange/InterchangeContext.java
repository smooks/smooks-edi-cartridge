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
package org.smooks.edi.edisax.interchange;

import org.smooks.assertion.AssertArgument;
import org.smooks.edi.edisax.BufferedSegmentReader;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.smooks.edi.edisax.model.internal.Description;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.registry.MappingsRegistry;
import org.smooks.lang.MutableInt;
import org.smooks.namespace.NamespaceDeclarationStack;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Map;

/**
 * EDI message interchange context object.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class InterchangeContext {

    public static final String INTERCHANGE_MESSAGE_BLOCK_ELEMENT_NAME = "interchangeMessage";

	private BufferedSegmentReader segmentReader;
	private ContentHandler contentHandler;
    private Map<String,Boolean> features;
    private EDIParser controlSegmentParser;
    public MutableInt indentDepth = new MutableInt(0);
    private ControlBlockHandlerFactory controlBlockHandlerFactory;
    private boolean validate;
    private MappingsRegistry registry;
    private NamespaceDeclarationStack namespaceDeclarationStack;
    private boolean containerManagedNamespaceStack = false;

    /**
	 * Public constructor.
	 *
     * @param segmentReader The interchange {@link BufferedSegmentReader} instance.
     * @param registry The {@link EdifactModel Mapping Models} registry.
     * @param contentHandler The {@link ContentHandler content handler} instance to receive the interchange events.
     * @param parserFeatures Parser features.
     * @param controlBlockHandlerFactory Control Block Handler Factory.
     * @param validate Validate the data types of the EDI message data as defined in the mapping model.
     */
	public InterchangeContext(BufferedSegmentReader segmentReader, MappingsRegistry registry, ContentHandler contentHandler, Map<String, Boolean> parserFeatures, ControlBlockHandlerFactory controlBlockHandlerFactory, NamespaceDeclarationStack namespaceDeclarationStack, boolean validate) {
		AssertArgument.isNotNull(segmentReader, "segmentReader");
		AssertArgument.isNotNull(registry, "registry");
		AssertArgument.isNotNull(contentHandler, "contentHandler");
        AssertArgument.isNotNull(controlBlockHandlerFactory, "controlBlockHandlerFactory");
		this.segmentReader = segmentReader;
		this.registry = registry;
		this.contentHandler = contentHandler;
        this.features = parserFeatures;
        this.controlBlockHandlerFactory = controlBlockHandlerFactory;
		this.validate = validate;
        this.namespaceDeclarationStack = namespaceDeclarationStack;

		controlSegmentParser = new EDIParser();
		controlSegmentParser.setBufferedSegmentReader(segmentReader);
		controlSegmentParser.setContentHandler(contentHandler);
		controlSegmentParser.setIndentDepth(indentDepth);

        if (this.namespaceDeclarationStack == null) {
            this.namespaceDeclarationStack= new NamespaceDeclarationStack();
        } else {
            this.containerManagedNamespaceStack = true;
        }
        controlSegmentParser.setNamespaceDeclarationStack(this.namespaceDeclarationStack);

        Edimap controlMap = new Edimap();
        EdifactModel controlModel = new EdifactModel(controlMap);

        controlMap.setDescription(new Description().setName("EDI Message Interchange Control Model").setVersion("N/A"));
        controlSegmentParser.setMappingModel(controlModel);
    }

    public ControlBlockHandler getControlBlockHandler(String segCode) throws SAXException {
        return controlBlockHandlerFactory.getControlBlockHandler(segCode);
    }

    public BufferedSegmentReader getSegmentReader() {
		return segmentReader;
	}

    public ContentHandler getContentHandler() {
		return contentHandler;
	}

    public boolean isValidate() {
		return validate;
	}

    public String getNamespace() {
        return controlBlockHandlerFactory.getNamespace();
    }

    public NamespaceDeclarationStack getNamespaceDeclarationStack() {
        return namespaceDeclarationStack;
    }

    public EDIParser newParser(EdifactModel mappingModel) {
		EDIParser parser = new EDIParser();

		parser.setContentHandler(contentHandler);
		parser.setMappingModel(mappingModel);
		parser.setBufferedSegmentReader(segmentReader);
		parser.setIndentDepth(indentDepth);
        parser.getFeatures().putAll(features);
		parser.setFeature(EDIParser.FEATURE_VALIDATE, validate);
        parser.setNamespaceDeclarationStack(namespaceDeclarationStack);

		return parser;
	}

    public EDIParser getControlSegmentParser() {
		return controlSegmentParser;
	}

    public void mapControlSegment(Segment controlSegment, boolean clearSegmentBuffer) throws SAXException {
		controlSegmentParser.startElement(controlSegment, true);
		controlSegmentParser.mapFields(segmentReader.getCurrentSegmentFields(), controlSegment);
		controlSegmentParser.endElement(controlSegment, true);

		// And clear the buffer... we're finished with this data...
		if(clearSegmentBuffer) {
			segmentReader.getSegmentBuffer().setLength(0);
		}
	}

    public void pushDelimiters(Delimiters delimiters) {
        segmentReader.pushDelimiters(delimiters);
    }

    public void popDelimiters() {
        segmentReader.popDelimiters();
    }

    /**
     * Returns an instance of {@link MappingsRegistry}
     *
     * @return
     */
    public MappingsRegistry getRegistry() {
		return registry;
	}

    public boolean isContainerManagedNamespaceStack() {
        return containerManagedNamespaceStack;
    }
}
