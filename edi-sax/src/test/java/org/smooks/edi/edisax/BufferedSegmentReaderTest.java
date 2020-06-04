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

package org.smooks.edi.edisax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tfennelly
 */
public class BufferedSegmentReaderTest {

    @Test
    public void test() throws IOException {
        test("111111111ff22222222222ff333333333ff4444444444f4444444fff5555555555", "ff", "|",
                new String[]{"111111111", "22222222222", "333333333", "4444444444f4444444", "f5555555555"});

        test("a", "ff", "|", new String[]{"a"});

        test("ff", "ff", "*", new String[]{});

        test("111111111\n22222222222\n333333333\n4444444444f4444444\nf5555555555", "\n", "*",
                new String[]{"111111111", "22222222222", "333333333", "4444444444f4444444", "f5555555555"});
    }

    @Test
    public void test_ignore_cr_lf() throws IOException {
        String fieldDelimiter = "*";
        String segmentDelimiter = "'!$";
        String edi1 = "SEG0*1*2**4*5'\nSEG1*1*2*3'\r\nSEG2*1*2*3*4'";
        String edi2 = "SEG0*1*2**4*5'SEG1*1*2*3'SEG2*1*2*3*4'";

        // Check that BufferedSegmentReader reads all segments when !$ exists at end of segmentdelimiter.
        BufferedSegmentReader reader = createSegmentReader(edi1, segmentDelimiter, fieldDelimiter);
        int segIndex = 0;
        while (reader.moveToNextSegment()) {
            assertEquals("SEG" + segIndex, reader.getCurrentSegmentFields()[0], "Segment comparison failure.");
            segIndex++;
        }
        assertTrue(segIndex - 1 == 2, "The number of segments read should be three");

        // Check that BufferedSegmentReader reads all segments when !$ exists at end of segmentdelimiter and there are no newlines.
        reader = createSegmentReader(edi2, segmentDelimiter, fieldDelimiter);
        segIndex = 0;
        while (reader.moveToNextSegment()) {
            assertEquals("SEG" + segIndex, reader.getCurrentSegmentFields()[0], "Segment comparison failure.");
            segIndex++;
        }
        assertTrue(segIndex - 1 == 2, "The number of segments read should be three");

        // Check that BufferedSegmentReader can peek through \r or \n characters
        reader = createSegmentReader(edi1, segmentDelimiter, fieldDelimiter);
        segIndex = 0;
        String peek = reader.peek(edi1.length());
        assertTrue(peek.equals(edi2), "Peek should return all characters but \n and \r");

    }

    @Test
    public void test_not_ignore_cr_lf() throws IOException {
        String fieldDelimiter = "*";
        String segmentDelimiter = "'";
        String edi1 = "SEG0*1*2**4*5'\nSEG1*1*2*3'\r\nSEG2*1*2*3*4'";
        String edi2 = "SEG0*1*2**4*5'SEG1*1*2*3'SEG2*1*2*3*4'";

        // Check that BufferedSegmentReader reads all segments when !$ exists at end of segmentdelimiter.
        BufferedSegmentReader reader = createSegmentReader(edi1, segmentDelimiter, fieldDelimiter);
        int segIndex = 0;
        while (reader.moveToNextSegment()) {
            if (segIndex == 0) {
                assertEquals("SEG" + segIndex, reader.getCurrentSegmentFields()[0], "Segment comparison failure.");
            } else if (segIndex == 1) {
                assertEquals("SEG" + segIndex, reader.getCurrentSegmentFields()[0], "Segment comparison failure.");
            } else if (segIndex == 2) {
                assertEquals("SEG" + segIndex, reader.getCurrentSegmentFields()[0], "Segment comparison failure.");
            } else {
                assertTrue(false, "More segments than expected in test case.");
            }
            segIndex++;
        }
        assertTrue(segIndex - 1 == 2, "The number of segments read should be three");

        // Check that BufferedSegmentReader reads all segments when !$ exists at end of segmentdelimiter and there are no newlines.
        reader = createSegmentReader(edi2, segmentDelimiter, fieldDelimiter);
        segIndex = 0;
        while (reader.moveToNextSegment()) {
            assertEquals("SEG" + segIndex, reader.getCurrentSegmentFields()[0], "Segment comparison failure.");
            segIndex++;
        }
        assertTrue(segIndex - 1 == 2, "The number of segments read should be three");

    }

    private void test(String input, String segmentDelim, String fieldDelim, String[] segments) throws IOException {
        BufferedSegmentReader reader = createSegmentReader(input, segmentDelim, fieldDelim);
        int segIndex = 0;

        while (segIndex < segments.length && reader.moveToNextSegment()) {
            String segment = reader.getSegmentBuffer().toString();
            assertEquals(segments[segIndex], segment, "Segment comparison failure.");
            segIndex++;
        }

        assertEquals(segments.length, segIndex, "All segments not read.");
    }

    private BufferedSegmentReader createSegmentReader(String input, String segmentDelim, String fieldDelim) {
        InputSource inputSource = new InputSource(new ByteArrayInputStream(input.getBytes()));
        Delimiters delimiters = new Delimiters().setSegment(segmentDelim).setField(fieldDelim);
        delimiters.setEscape("?");
        BufferedSegmentReader reader = new BufferedSegmentReader(inputSource, delimiters);
        return reader;
    }

    public void test_split() {
        Arrays.asList(StringUtils.splitPreserveAllTokens("a*b***C*d", "*"));
    }
}
