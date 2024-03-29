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
package org.smooks.edi.edisax.unedifact.handlers.r41;

import org.smooks.api.SmooksConfigException;
import org.smooks.edi.edisax.interchange.ControlBlockHandler;
import org.smooks.edi.edisax.interchange.ControlBlockHandlerFactory;
import org.smooks.edi.edisax.model.EDIConfigDigester;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.model.internal.SegmentGroup;
import org.smooks.edi.edisax.unedifact.handlers.*;
import org.smooks.xml.hierarchy.HierarchyChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

/**
 * UN/EDIFACT control block handler factory (Version 4, Release 1).
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNEdifact41ControlBlockHandlerFactory implements ControlBlockHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UNBHandler.class);

    public static final String NAMESPACE = NAMESPACE_ROOT + ".v41";

    private static Segment unbSegment;
    private static Segment unzSegment;
    private static Segment ungSegment;
    private static Segment uneSegment;
    private static Segment unhSegment;
    private static Segment untSegment;
    private static HashMap<String, Charset> toCharsetMapping;

    private HierarchyChangeListener hierarchyChangeListener;

    public UNEdifact41ControlBlockHandlerFactory(HierarchyChangeListener hierarchyChangeListener) {
        this.hierarchyChangeListener = hierarchyChangeListener;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public ControlBlockHandler getControlBlockHandler(String segCode) throws SAXException {

        if (segCode.equals("UNH")) {
            return new UNHHandler(unhSegment, untSegment, hierarchyChangeListener);
        } else if (segCode.equals("UNG")) {
            return new UNGHandler(ungSegment, uneSegment);
        } else if (segCode.equals("UNA")) {
            return new UNAHandler();
        } else if (segCode.equals("UNB")) {
            return new UNBHandler(unbSegment, unzSegment, toCharsetMapping);
        } else if (segCode.charAt(0) == 'U') {
            return new GenericHandler();
        }

        throw new SAXException("Unknown/Unexpected UN/EDIFACT control block segment code '" + segCode + "'.");
    }

    static {
        try {
            Edimap controlBlockSegments = EDIConfigDigester.digestConfig(UNEdifact41ControlBlockHandlerFactory.class.getResourceAsStream("v41-segments.xml"));
            List<SegmentGroup> segments = controlBlockSegments.getSegments().getSegments();
            for (SegmentGroup segment : segments) {
                if (segment.getSegcode().equals("UNB")) {
                    unbSegment = (Segment) segment;
                } else if (segment.getSegcode().equals("UNZ")) {
                    unzSegment = (Segment) segment;
                } else if (segment.getSegcode().equals("UNG")) {
                    ungSegment = (Segment) segment;
                } else if (segment.getSegcode().equals("UNE")) {
                    uneSegment = (Segment) segment;
                } else if (segment.getSegcode().equals("UNH")) {
                    unhSegment = (Segment) segment;
                } else if (segment.getSegcode().equals("UNT")) {
                    untSegment = (Segment) segment;
                }
            }
        } catch (Exception e) {
            throw new SmooksConfigException("Unexpected exception reading UN/EDIFACT v4.1 segment definitions.", e);
        }

        toCharsetMapping = new HashMap<String, Charset>();

        // http://www.gefeg.com/jswg/cl/v41/40107/cl1.htm
        addCharsetMapping("UNOA", "ASCII");
        addCharsetMapping("UNOB", "ASCII");
        addCharsetMapping("UNOC", "ISO8859-1");
        addCharsetMapping("UNOD", "ISO8859-2");
        addCharsetMapping("UNOE", "ISO8859-5");
        addCharsetMapping("UNOF", "ISO8859-7");
        addCharsetMapping("UNOG", "ISO8859-3");
        addCharsetMapping("UNOH", "ISO8859-4");
        addCharsetMapping("UNOI", "ISO8859-6");
        addCharsetMapping("UNOJ", "ISO8859-8");
        addCharsetMapping("UNOK", "ISO8859-9");
        addCharsetMapping("UNOL", "ISO8859-15");
        addCharsetMapping("UNOW", "UTF-8");
        addCharsetMapping("UNOX", "ISO-2022-CN");
        addCharsetMapping("UNOY", "UTF-8");

        // http://www.gefeg.com/jswg/cl/v41/40107/cl17.htm
        addCharsetMapping("1", "ASCII");
        addCharsetMapping("2", "ASCII");
        addCharsetMapping("3", "IBM500");
        addCharsetMapping("4", "IBM850");
        addCharsetMapping("5", "UTF-16");
        addCharsetMapping("6", "UTF-32");
        addCharsetMapping("7", "UTF-8");
        addCharsetMapping("8", "UTF-16");

        // IATA PADIS
        addCharsetMapping("IATA", "ASCII");
        addCharsetMapping("IATB", "ASCII");
    }

    private static void addCharsetMapping(String code, String charsetName) {
        if (Charset.isSupported(charsetName)) {
            toCharsetMapping.put(code, Charset.forName(charsetName));
        } else {
            LOGGER.debug("Unsupported character set '" + charsetName + "'.  Cannot support for '" + code + "' if defined on the syntaxIdentifier field on the UNB segment.  Check the JVM version etc.");
        }
    }
}
