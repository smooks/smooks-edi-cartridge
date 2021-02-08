/*-
 * ========================LICENSE_START=================================
 * smooks-ect
 * %%
 * Copyright (C) 2020 - 2021 Smooks
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
package org.smooks.edi.ect.formats.unedifact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.model.internal.SegmentGroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class UnEdifactDefinitionReaderTest {

  private Reader dataReader;
  private Reader compositeReader;
  private Reader segmentReader;
  private Reader codeReader;

  @Nested
  @DisplayName("with D08A definitions")
  class D08A {

    @BeforeEach
    public void init() throws Exception {
      final ZipFile
          d08aZip =
          new ZipFile(getClass().getResource("/org/smooks/edi/ect/D08A.zip").getFile());

      final ZipEntry ededZip = d08aZip.getEntry("eded.zip");
      dataReader = readZipContent(d08aZip.getInputStream(ededZip), "EDED.08A");
      final ZipEntry edcdZip = d08aZip.getEntry("edcd.zip");
      compositeReader = readZipContent(d08aZip.getInputStream(edcdZip), "EDCD.08A");
      final ZipEntry edsdZip = d08aZip.getEntry("edsd.zip");
      segmentReader = readZipContent(d08aZip.getInputStream(edsdZip), "EDSD.08A");
      final ZipEntry unclZip = d08aZip.getEntry("uncl.zip");
      codeReader = readZipContent(d08aZip.getInputStream(unclZip), "UNCL.08A");
    }

    @Test
    @DisplayName("should extract all code list values properly")
    public void checkThatCodeListValuesForComponent1049AreReadCorrectly() throws Exception {
      // During parsing of the code list entries, the algorithm used looked for the next line
      // separator. As we already stopped at that line separator before, it skipped the whole
      // subsequent code list entry, i.e. 1049. If this test is run with the original code a NPE will
      // be thrown as there is no available code list entry here
      final Edimap
          edimap =
          UnEdifactDefinitionReader
              .parse(dataReader, compositeReader, segmentReader, codeReader, true);

      List<SegmentGroup> segments = edimap.getSegments().getSegments();
      Segment erp = findSegment(segments, "ERP");

      assertEquals(Arrays.asList("1", "2", "5", "6", "7", "8", "9", "10", "11"),
                   erp.getFields().get(0).getComponents().get(0).getCodeList().getCodes(),
                   "Not all code list values of component 1049 of complex element C701 of segment ERP are available");
    }

    @Test
    @DisplayName("should not lose values of code list definitions")
    public void checkDefinitionReaderDoesNotLoseCodeListInformationOnComplexElements()
        throws Exception {
      final Edimap
          edimap =
          UnEdifactDefinitionReader
              .parse(dataReader, compositeReader, segmentReader, codeReader, true);

      List<SegmentGroup> segments = edimap.getSegments().getSegments();
      // https://service.unece.org/trade/untdid/d08a/trsd/trsddtm.htm
      Segment dtm = findSegment(segments, "DTM");
      // https://service.unece.org/trade/untdid/d08a/tred/tred2379.htm
      assertNotNull(dtm.getFields().get(0).getComponents().get(2).getCodeList(),
                    "Expected a valid code list entry on component 2379 'Date or time or period format code' of segment 'DTM'  on complex element C507 'DATE/TIME/PERIOD' but could not find one!");
    }
  }

  @Nested
  @DisplayName("with D00A definitions")
  class D00A {

    @BeforeEach
    public void init() throws Exception {
      final ZipFile
          d000aZip =
          new ZipFile(getClass().getResource("/org/smooks/edi/ect/d00a.zip").getFile());

      final ZipEntry ededZip = d000aZip.getEntry("EDED.ZIP");
      dataReader = readZipContent(d000aZip.getInputStream(ededZip), "EDED.00A");
      final ZipEntry edcdZip = d000aZip.getEntry("EDCD.ZIP");
      compositeReader = readZipContent(d000aZip.getInputStream(edcdZip), "EDCD.00A");
      final ZipEntry edsdZip = d000aZip.getEntry("EDSD.ZIP");
      segmentReader = readZipContent(d000aZip.getInputStream(edsdZip), "EDSD.00A");
      final ZipEntry unclZip = d000aZip.getEntry("UNCL.ZIP");
      codeReader = readZipContent(d000aZip.getInputStream(unclZip), "UNCL.00A");
    }

    @Test
    @DisplayName("should extract code list values with multiple changes indicators properly")
    public void checkThatCodeListValuesForComponent1131DoNotContainUnwantedOrDuplicateEntries() throws Exception {
      // During DFDL validation D00A failed as code list entries contained a duplicate A entry on
      // code list entries for E1131. This component is used i.e. within C002 that is part of BGM
      // segment
      //
      // UNCL.00A
      //
      // ...
      // |    35    Rail additional charges
      //               A code list identifying specific rail charges included
      //               in the payment conditions in addition to the freight
      //               cost.
      //
      // #|   36    Railway company network
      //               A code list identifying the different railway companies
      //               as member of the International Union of Railways.
      //
      //      37    Railway locations
      //               Code identifying a location in railway environment.
      //
      // #|   38    Railway customer
      //               A code list identifying rail customers.
      //
      //      39    Rail unified nomenclature of goods
      //               Self explanatory.
      // ...
      //
      // -------------------------------------------------------------------------------------------
      //
      // EDIFACT-Segments.dfdl.xsd
      //
      // ...
      //            <xsd:enumeration value="35"/>
      //            <xsd:enumeration value="A"/>
      //            <xsd:enumeration value="37"/>
      //            <xsd:enumeration value="A"/>
      //            <xsd:enumeration value="39"/>
      // ...

      final Edimap
          edimap =
          UnEdifactDefinitionReader
              .parse(dataReader, compositeReader, segmentReader, codeReader, true);

      List<SegmentGroup> segments = edimap.getSegments().getSegments();
      Segment erp = findSegment(segments, "BGM");

      final List<String> codeListValues =
          erp.getFields().get(0).getComponents().get(0).getCodeList().getCodes();
      final List<String> needToBeIncluded = Arrays.asList("35", "36", "37", "38", "39");
      assertTrue(codeListValues.containsAll(needToBeIncluded),
                   "Some of the expected code list values are missing");
      assertFalse(codeListValues.contains("A"), "Unexpected value contained in code list values of component 1131");
    }
  }

  final Reader readZipContent(final InputStream is, final String nameToRead) throws IOException {
    try (final ZipInputStream zis = new ZipInputStream(is)) {
      ZipEntry entry = zis.getNextEntry();
      while (entry != null) {
        if (entry.getName().equals(nameToRead)) {

          ByteArrayOutputStream baos = new ByteArrayOutputStream();

          byte[] bytes = new byte[2048];
          int size;
          while ((size = zis.read(bytes, 0, bytes.length)) != -1) {
            baos.write(bytes, 0, size);
          }

          return new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        }

        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    }
    throw new IllegalArgumentException("Could not find " + nameToRead + " in provided ZIP");
  }

  private Segment findSegment(final List<SegmentGroup> segments, final String name) {
    if (segments == null || segments.isEmpty()) {
      fail("Expected a valid list of segments to be present");
    }
    for (final SegmentGroup segment : segments) {
      if (segment instanceof Segment) {
        Segment seg = (Segment) segment;
        if (seg.getSegcode().equals(name)) {
          return seg;
        }
      } else {
        return findSegment(segment.getSegments(), name);
      }
    }
    return null;
  }
}
