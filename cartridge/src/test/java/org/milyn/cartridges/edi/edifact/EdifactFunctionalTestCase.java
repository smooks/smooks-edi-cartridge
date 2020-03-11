package org.milyn.cartridges.edi.edifact;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.container.standalone.StandaloneApplicationContext;
import org.milyn.io.StreamUtils;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.milyn.SmooksUtil.filterAndSerialize;

public class EdifactFunctionalTestCase {

    @ParameterizedTest
    @CsvSource({"/edifact/data/INVOIC_D.03B_Interchange_with_UNA.txt, /edifact/data/INVOIC_D.03B_Interchange_with_UNA.xml", "/edifact/data/ORDERS_D.03B_Interchange.txt, /edifact/data/ORDERS_D.03B_Interchange.xml"})
    public void testSmooksConfigGivenParser(String fileName, String expectedResult) throws Exception {
        Smooks smooks = new Smooks("/edifact/smooks-parser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream(fileName), smooks);

        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream(expectedResult)).ignoreWhitespace().withTest(result).build().hasDifferences());
    }

    @ParameterizedTest
    @CsvSource({"/edifact/data/INVOIC_D.03B_Interchange_with_UNA.xml, /edifact/data/INVOIC_D.03B_Interchange_with_UNA.txt", "/edifact/data/ORDERS_D.03B_Interchange.xml, /edifact/data/ORDERS_D.03B_Interchange.txt"})
    public void testSmooksConfigGivenUnparser(String fileName, String expectedResult) throws Exception {
        Smooks smooks = new Smooks("/edifact/smooks-unparser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream(fileName), smooks);

        assertEquals(StreamUtils.readStreamAsString(getClass().getResourceAsStream(expectedResult)).replaceAll("\\n", "\r\n"), result);
    }
}
