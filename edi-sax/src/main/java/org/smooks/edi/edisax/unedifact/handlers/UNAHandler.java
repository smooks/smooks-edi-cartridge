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
import org.smooks.edi.edisax.interchange.ControlBlockHandler;
import org.smooks.edi.edisax.interchange.InterchangeContext;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.xml.sax.SAXException;

/**
 * UNA Segment Handler.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNAHandler implements ControlBlockHandler {

	public void process(InterchangeContext interchangeContext) throws IOException, SAXException {
		Delimiters delimiters = new Delimiters();
		BufferedSegmentReader segmentReader = interchangeContext.getSegmentReader();

		// The UNA segment code is still in the segment buffer... clear it before 
		// reading the segment delimiters...
		segmentReader.getSegmentBuffer().setLength(0);
		
		// Read the delimiter chars one-by-one and set in the Delimiters instance...
		
		// 1st char is the component ("sub-element") delimiter...
		delimiters.setComponent( segmentReader.read(1));
		// 2nd char is the field ("data-element") delimiter...
		delimiters.setField(     segmentReader.read(1));
		// 3rd char is the decimal point indicator...
		delimiters.setDecimalSeparator(segmentReader.read(1));
		// 4th char is the escape char ("release")...
		delimiters.setEscape(    segmentReader.read(1));
		// 5th char is reserved for future use...
		segmentReader.read(1);
		// 6th char is the segment delimiter...
		delimiters.setSegment(   segmentReader.read(1));

		interchangeContext.pushDelimiters(delimiters);
	}
}
