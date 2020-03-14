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
package org.smooks.edi.edisax.unedifact.no_ung;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.EDIParser;
import org.smooks.edi.edisax.MockContentHandler;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.registry.DefaultMappingsRegistry;
import org.smooks.edi.edisax.unedifact.UNEdifactInterchangeParser;
import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.io.StreamUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UNEdifactInterchangeParser_no_ung_Test {
    private static final String XSLT_IDENTITY = ""
            + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
            + "  <xsl:template match=\"@*|node()\">\n"
            + "    <xsl:copy>\n"
            + "      <xsl:apply-templates select=\"@*|node()\"/>\n"
            + "    </xsl:copy>\n"
            + "  </xsl:template>\n"
            + "</xsl:stylesheet>\n";

    @Test
    public void test_unzipped() throws IOException, SAXException, EDIConfigurationException {
        UNEdifactInterchangeParser parser = newUnEdifactInterchangeParser();

        testExchanges(parser);
    }

    private UNEdifactInterchangeParser newUnEdifactInterchangeParser() throws IOException, SAXException {
        EdifactModel model1 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG1-model.xml"));
        EdifactModel model2 = EDIParser.parseMappingModel(getClass().getResourceAsStream("../MSG2-model.xml"));

        UNEdifactInterchangeParser parser = new UNEdifactInterchangeParser();
        parser.setMappingsRegistry(new DefaultMappingsRegistry(model1, model2));
        return parser;
    }

    @Test
    public void test_zipped() throws IOException, SAXException, EDIConfigurationException {
        createZip();
        UNEdifactInterchangeParser parser = new UNEdifactInterchangeParser();
        parser.setMappingsRegistry(new DefaultMappingsRegistry("mapping-models.zip", URI.create("./target")));
        testExchanges(parser);
    }

    @Test
    public void parseWithTransformerHandler() throws TransformerConfigurationException, IOException, SAXException {
        TransformerFactory factory = TransformerFactory.newInstance();
        assertTrue(factory.getFeature(SAXTransformerFactory.FEATURE));
        SAXTransformerFactory saxFactory = (SAXTransformerFactory) factory;
        Templates identity = saxFactory.newTemplates(new StreamSource(new StringReader(XSLT_IDENTITY)));
        TransformerHandler handler = saxFactory.newTransformerHandler(identity);
        MockContentHandler result = new MockContentHandler();
        handler.setResult(new SAXResult(result));

        UNEdifactInterchangeParser parser = newUnEdifactInterchangeParser();
        parser.setContentHandler(handler);

        URL edifact = getClass().getResource("unedifact-msg-01.edi");
        assertNotNull(edifact);

        InputSource inputSource = new InputSource(edifact.toExternalForm());
        try {

            // XXX BufferedSegmentReader does not support InputSource without stream
            inputSource.setByteStream(edifact.openStream());
            parser.parse(inputSource);

            InputStream expectedXml = getClass().getResourceAsStream("unedifact-msg-expected.xml");
            assertNotNull(expectedXml);
			assertFalse(DiffBuilder.compare(expectedXml).ignoreWhitespace().withTest(result.xmlMapping.toString()).build().hasDifferences());
        } finally {
            if (inputSource.getByteStream() != null) {
                inputSource.getByteStream().close();
            }
        }
    }

    private void testExchanges(UNEdifactInterchangeParser parser) throws IOException, SAXException {
        MockContentHandler handler;

        // Test message 01 - no UNA segment...
        handler = new MockContentHandler();
        parser.setContentHandler(handler);
        parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-01.edi")));
        //System.out.println(handler.xmlMapping);
		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("unedifact-msg-expected.xml")).withTest(handler.xmlMapping.toString()).ignoreWhitespace().build().hasDifferences());

        // Test message 01 - has a UNA segment...
        handler = new MockContentHandler();
        parser.setContentHandler(handler);
        parser.parse(new InputSource(getClass().getResourceAsStream("unedifact-msg-02.edi")));

		assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("unedifact-msg-expected.xml")).withTest(handler.xmlMapping.toString()).ignoreWhitespace().build().hasDifferences());

	}

    private void createZip() throws IOException {
        File zipFile = new File("target/mapping-models.zip");

        zipFile.delete();

        ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            addZipEntry("test/models/subs/MSG1-model.xml", "../MSG1-model.xml", zipStream);
            addZipEntry("test/models/subs/MSG2-model.xml", "../MSG2-model.xml", zipStream);
            addZipEntry("test/models/MSG3-model.xml", "../MSG3-model.xml", zipStream);
            addZipEntry(EDIUtils.EDI_MAPPING_MODEL_ZIP_LIST_FILE, "../mapping-models.lst", zipStream);
        } finally {
            zipStream.close();
        }
    }

    private void addZipEntry(String name, String resource, ZipOutputStream zipStream) throws IOException {
        ZipEntry zipEntry = new ZipEntry(name);
        byte[] resourceBytes = StreamUtils.readStream(getClass().getResourceAsStream(resource));

        zipStream.putNextEntry(zipEntry);
        zipStream.write(resourceBytes);
    }
}
