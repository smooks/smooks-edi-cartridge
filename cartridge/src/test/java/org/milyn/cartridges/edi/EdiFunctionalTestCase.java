package org.milyn.cartridges.edi;

import org.junit.jupiter.api.Test;
import org.milyn.Smooks;
import org.milyn.io.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.milyn.SmooksUtil.filterAndSerialize;

public class EdiFunctionalTestCase {

    @Test
    public void testSmooksConfigGivenParser() throws Exception {
        Smooks smooks = new Smooks("/edi/smooks-parser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/edi/data/edi-input.txt"), smooks);

        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/edi/data/expected.xml")), result));
    }

    @Test
    public void testSmooksConfigGivenUnparser() throws Exception {
        Smooks smooks = new Smooks("/edi/smooks-unparser-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/edi/data/expected.xml"), smooks);

        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/edi/data/edi-input.txt")), result));
    }
}
