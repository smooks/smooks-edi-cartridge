package org.smooks.cartridges.edi;

import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.io.StreamUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.smooks.SmooksUtil.filterAndSerialize;

public class EdiReaderConfiguratorTestCase {

    @Test
    public void testToConfig() throws IOException {
        EdiReaderConfigurator ediReaderConfigurator = new EdiReaderConfigurator("/edi-to-xml-mapping.dfdl.xsd").setDataElementSeparator("*").setSegmentTerminator("%NL;").setCompositeDataElementSeparator("^");

        Smooks smooks = new Smooks();
        smooks.setReaderConfig(ediReaderConfigurator);

        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/data/edi-input.txt"), smooks);
        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/data/expected.xml")), result));
    }
}
