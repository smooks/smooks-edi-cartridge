package org.smooks.cartridges.edi.edifact;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.smooks.Smooks;
import org.smooks.io.StreamUtils;
import org.xmlunit.builder.DiffBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.smooks.SmooksUtil.filterAndSerialize;

public class EdifactFunctionalTestCase {

    private Smooks smooks;

    @BeforeEach
    public void beforeEach() {
        smooks = new Smooks();
    }

    @AfterEach
    public void afterEach() {
        smooks.close();
    }

    @ParameterizedTest
    @CsvSource({"/edifact/data/INVOIC_D.03B_Interchange_with_UNA.txt, /edifact/data/INVOIC_D.03B_Interchange_with_UNA.xml", "/edifact/data/ORDERS_D.03B_Interchange.txt, /edifact/data/ORDERS_D.03B_Interchange.xml"})
    public void testSmooksConfigGivenParser(String fileName, String expectedResult) throws Exception {
        smooks.addConfigurations("/edifact/smooks-parser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream(fileName), smooks);

        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream(expectedResult)).ignoreWhitespace().withTest(result).build().hasDifferences());
    }

    @ParameterizedTest
    @CsvSource({"/edifact/data/INVOIC_D.03B_Interchange_with_UNA.xml, /edifact/data/INVOIC_D.03B_Interchange_with_UNA.txt", "/edifact/data/ORDERS_D.03B_Interchange.xml, /edifact/data/ORDERS_D.03B_Interchange.txt"})
    public void testSmooksConfigGivenUnparser(String fileName, String expectedResult) throws Exception {
        smooks.addConfigurations("/edifact/smooks-unparser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream(fileName), smooks);

        assertEquals(StreamUtils.readStreamAsString(getClass().getResourceAsStream(expectedResult)).replaceAll("\\n", "\r\n"), result);
    }
}
