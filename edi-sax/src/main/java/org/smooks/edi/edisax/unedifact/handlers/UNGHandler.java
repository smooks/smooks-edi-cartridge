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
package org.smooks.edi.edisax.unedifact.handlers;

import java.io.IOException;

import org.smooks.edi.edisax.BufferedSegmentReader;
import org.smooks.edi.edisax.EDIParseException;
import org.smooks.edi.edisax.interchange.ControlBlockHandler;
import org.smooks.edi.edisax.interchange.InterchangeContext;
import org.smooks.edi.edisax.model.internal.Segment;
import org.xml.sax.SAXException;

/**
 * UNG Segment Handler.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNGHandler implements ControlBlockHandler {

    private Segment ungSegment;
    private Segment uneSegment;

    public UNGHandler(Segment ungSegment, Segment uneSegment) {
        this.ungSegment = ungSegment;
        this.uneSegment = uneSegment;
    }

    public void process(InterchangeContext interchangeContext) throws IOException, SAXException {
        BufferedSegmentReader segmentReader = interchangeContext.getSegmentReader();

        interchangeContext.getControlSegmentParser().startElement("group", ungSegment.getNamespace(), true);

        segmentReader.moveToNextSegment(false);
        interchangeContext.mapControlSegment(ungSegment, true);

        while (true) {
            String segCode = segmentReader.peek(3, true);

            if (segCode.equals("UNE")) {
                segmentReader.moveToNextSegment(false);
                interchangeContext.mapControlSegment(uneSegment, true);
                break;
            } else if (segCode.length() > 0) {
                ControlBlockHandler handler = interchangeContext.getControlBlockHandler(segCode);
                handler.process(interchangeContext);
            } else {
                throw new EDIParseException("Unexpected end of UN/EDIFACT data stream.  If stream was reset (e.g. after read charset was changed), please make sure underlying stream was properly reset.");
            }
        }

        interchangeContext.getControlSegmentParser().endElement("group", ungSegment.getNamespace(), true);
    }
}
