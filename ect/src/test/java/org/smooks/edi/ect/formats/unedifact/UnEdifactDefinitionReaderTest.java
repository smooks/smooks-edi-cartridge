package org.smooks.edi.ect.formats.unedifact;

import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class UnEdifactDefinitionReaderTest {

  private Reader dataReader;
  private Reader compositeReader;
  private Reader segmentReader;
  private Reader codeReader;

  @BeforeEach
  public void init() throws Exception {
    final ZipFile d08aZip = new ZipFile(getClass().getResource("/org/smooks/edi/ect/D08A.zip").getFile());

    final ZipEntry ededZip = d08aZip.getEntry("eded.zip");
    dataReader = readZipContent(d08aZip.getInputStream(ededZip), "EDED.08A");
    final ZipEntry edcdZip = d08aZip.getEntry("edcd.zip");
    compositeReader = readZipContent(d08aZip.getInputStream(edcdZip), "EDCD.08A");
    final ZipEntry edsdZip = d08aZip.getEntry("edsd.zip");
    segmentReader = readZipContent(d08aZip.getInputStream(edsdZip), "EDSD.08A");
    final ZipEntry unclZip = d08aZip.getEntry("uncl.zip");
    codeReader = readZipContent(d08aZip.getInputStream(unclZip), "UNCL.08A");
  }

  final Reader readZipContent(final InputStream is, final String nameToRead) throws IOException {
    try(final ZipInputStream zis = new ZipInputStream(is)) {
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

  @Test
  public void checkThatCodeListValuesForComponent1049AreReadCorrectly() throws Exception {
    // While checking for
    final Edimap edimap = UnEdifactDefinitionReader.parse(dataReader, compositeReader, segmentReader, codeReader, true);

    List<SegmentGroup> segments = edimap.getSegments().getSegments();
    Segment erp = findSegment(segments, "ERP");

    assertEquals(Arrays.asList("1", "2", "5", "6", "7", "8", "9", "10", "11"),
                 erp.getFields().get(0).getComponents().get(0).getCodeList().getCodes(),
                 "Not all code list values of component 1049 of complex element C701 of segment ERP are available");
  }

  @Test
  public void checkDefinitionReaderDoesNotLoseCodeListInformationOnComplexElements() throws Exception {
    final Edimap edimap = UnEdifactDefinitionReader.parse(dataReader, compositeReader, segmentReader, codeReader, true);

    List<SegmentGroup> segments = edimap.getSegments().getSegments();
    // https://service.unece.org/trade/untdid/d08a/trsd/trsddtm.htm
    Segment dtm = findSegment(segments, "DTM");
    // https://service.unece.org/trade/untdid/d08a/tred/tred2379.htm
    assertNotNull(dtm.getFields().get(0).getComponents().get(2).getCodeList(),
                  "Expected a valid code list entry on component 2379 'Date or time or period format code' of segment 'DTM'  on complex element C507 'DATE/TIME/PERIOD' but could not find one!");
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
