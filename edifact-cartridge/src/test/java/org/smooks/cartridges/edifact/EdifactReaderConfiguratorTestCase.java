package org.smooks.cartridges.edifact;

import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.smooks.SmooksUtil.filterAndSerialize;

public class EdifactReaderConfiguratorTestCase {

    @Test
    public void testToConfig() throws IOException {
        EdifactReaderConfigurator edifactReaderConfigurator = new EdifactReaderConfigurator("/d03b/EDIFACT-Messages.dfdl.xsd").setMessageTypes(Arrays.asList("INVOIC"));

        Smooks smooks = new Smooks();
        smooks.setReaderConfig(edifactReaderConfigurator);

        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/data/INVOIC_D.03B_Interchange_with_UNA.txt"), smooks);
        assertFalse(DiffBuilder.compare(getClass().getResourceAsStream("/data/INVOIC_D.03B_Interchange_with_UNA.xml")).ignoreWhitespace().withTest(result).build().hasDifferences());
    }
}
