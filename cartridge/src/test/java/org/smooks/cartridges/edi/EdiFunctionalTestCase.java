package org.smooks.cartridges.edi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.io.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.smooks.SmooksUtil.filterAndSerialize;

public class EdiFunctionalTestCase {

    private Smooks smooks;

    @BeforeEach
    public void beforeEach() {
        smooks = new Smooks();
    }

    @AfterEach
    public void afterEach() {
        smooks.close();
    }

    @Test
    public void testSmooksConfigGivenParser() throws Exception {
        smooks.addConfigurations("/edi/smooks-parser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/edi/data/edi-input.txt"), smooks);

        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/edi/data/expected.xml")), result));
        smooks.close();
    }

    @Test
    public void testSmooksConfigGivenUnparser() throws Exception {
        smooks.addConfigurations("/edi/smooks-unparser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/edi/data/expected.xml"), smooks);

        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/edi/data/edi-input.txt")), result));
    }
}
