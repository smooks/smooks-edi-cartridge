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
/*
	Milyn - Copyright (C) 2006 - 2010

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.smooks.edi.edisax.unedifact.handlers;

import java.io.IOException;

import org.smooks.edi.edisax.BufferedSegmentListener;
import org.smooks.edi.edisax.BufferedSegmentReader;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.interchange.ControlBlockHandler;
import org.smooks.edi.edisax.interchange.InterchangeContext;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.model.internal.Description;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.registry.MappingsRegistry;
import org.smooks.xml.hierarchy.HierarchyChangeListener;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.XMLConstants;

/**
 * UNH Segment Handler.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNHHandler implements ControlBlockHandler {

    private Segment unhSegment;
    private Segment untSegment;
    private static UNTSegmentListener untSegmentListener = new UNTSegmentListener();

    private HierarchyChangeListener hierarchyChangeListener;

    public UNHHandler(Segment unhSegment, Segment untSegment, HierarchyChangeListener hierarchyChangeListener) {
        this.unhSegment = unhSegment;
        this.untSegment = untSegment;
        this.hierarchyChangeListener = hierarchyChangeListener;
    }

    public void process(InterchangeContext interchangeContext) throws IOException, SAXException {
		BufferedSegmentReader segmentReader = interchangeContext.getSegmentReader();
		MappingsRegistry registry = interchangeContext.getRegistry();

		// Move to the end of the UNH segment and map it's fields..
		segmentReader.moveToNextSegment(false);

		// Select the mapping model to use for this message...
		String[] fields = segmentReader.getCurrentSegmentFields();
		String messageName = fields[2];
		EdifactModel mappingModel = registry.getMappingModel(messageName, segmentReader.getDelimiters());
        Edimap ediMap = mappingModel.getEdimap();

        Description description = ediMap.getDescription();
        AttributesImpl attrs = new AttributesImpl();
        String namespace = description.getNamespace();
        String commonNS = null;
        String messageNSPrefix = null;
        if(namespace != null && !namespace.equals(XMLConstants.NULL_NS_URI)) {
            int nameComponentIndex = namespace.lastIndexOf(":");
            if(nameComponentIndex != -1) {
                commonNS = namespace.substring(0, nameComponentIndex) + ":common";
                messageNSPrefix = description.getName().toLowerCase();
                attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "c", "xmlns:c", "CDATA", commonNS);
                attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, messageNSPrefix, "xmlns:" + messageNSPrefix, "CDATA", namespace);
            }
        }

		interchangeContext.getControlSegmentParser().startElement(InterchangeContext.INTERCHANGE_MESSAGE_BLOCK_ELEMENT_NAME, unhSegment.getNamespace(), true, attrs);
        interchangeContext.mapControlSegment(unhSegment, false);

		// Map the message... stopping at the UNT segment...
		try {
			EDIParser parser = interchangeContext.newParser(mappingModel);

			segmentReader.setSegmentListener(untSegmentListener);

            if(hierarchyChangeListener != null) {
                hierarchyChangeListener.attachXMLReader(parser);
            } else if (!interchangeContext.isContainerManagedNamespaceStack()) {
                interchangeContext.getNamespaceDeclarationStack().pushReader(parser);
            }

            parser.parse();
		} finally {
			segmentReader.setSegmentListener(null);
            if(hierarchyChangeListener != null) {
                hierarchyChangeListener.detachXMLReader();
            } else if (!interchangeContext.isContainerManagedNamespaceStack()) {
                interchangeContext.getNamespaceDeclarationStack().popReader();
            }
		}

		// We're at the end of the UNT segment now.  See the UNTSegmentListener below.

		// Map the UNT segment...
		interchangeContext.mapControlSegment(untSegment, true);
		segmentReader.getSegmentBuffer().setLength(0);

		interchangeContext.getControlSegmentParser().endElement(InterchangeContext.INTERCHANGE_MESSAGE_BLOCK_ELEMENT_NAME, unhSegment.getNamespace(), true);
	}

    private static class UNTSegmentListener implements BufferedSegmentListener {

        public boolean onSegment(BufferedSegmentReader bufferedSegmentReader) {
            String[] fields = bufferedSegmentReader.getCurrentSegmentFields();

            // Stop the current segment consumer if we have reached the UNT segment i.e.
            // only return true if it's not UNT...
            return !fields[0].equals("UNT");
        }
    }
}
