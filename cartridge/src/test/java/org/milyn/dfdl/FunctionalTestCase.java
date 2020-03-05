package org.milyn.dfdl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.io.StreamUtils;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.milyn.SmooksUtil.filterAndSerialize;

public class FunctionalTestCase {

    private static Smooks smooks;
    private static ExecutionContext executionContext;

    @BeforeAll
    public static void beforeAll() throws IOException, SAXException {
        smooks = new Smooks("/smooks-config.xml");
        executionContext = smooks.createExecutionContext();
    }

    @ParameterizedTest
    @CsvSource({"/data/edifact/INVOIC_D.03B_Interchange_with_UNA.txt", "/data/edifact/ORDERS_D.03B_Interchange.txt"})
    public void testSmooksConfig(String fileName) throws Exception {
        String result = filterAndSerialize(executionContext, getClass().getResourceAsStream(fileName), smooks);

        assertEquals(StreamUtils.readStreamAsString(getClass().getResourceAsStream(fileName)), result);
    }
}
