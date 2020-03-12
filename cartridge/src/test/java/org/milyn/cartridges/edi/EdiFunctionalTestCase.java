package org.milyn.cartridges.edi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.milyn.Smooks;
import org.milyn.io.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.milyn.SmooksUtil.filterAndSerialize;

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
