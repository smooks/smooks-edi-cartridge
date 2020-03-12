package org.milyn.edi.edisax.edimap_writer;

import org.junit.jupiter.api.Test;
import org.milyn.edi.edisax.model.EDIConfigDigester;
import org.milyn.edi.edisax.model.internal.Edimap;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class EdimapWriterTest {

    @Test
    public void test() throws IOException, SAXException {
        test("edimap-01.xml");
    }

    public void test(String edimapfile) throws IOException, SAXException {
        Edimap edimap = EDIConfigDigester.digestConfig(getClass().getResourceAsStream(edimapfile));
        StringWriter result = new StringWriter();

        edimap.write(result);

        System.out.println(result);

        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream(edimapfile)).ignoreWhitespace().withTest(result.toString()).build().hasDifferences());
    }
}
