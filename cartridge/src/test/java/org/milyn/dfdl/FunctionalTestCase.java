package org.milyn.dfdl;

import org.junit.jupiter.api.Test;
import org.milyn.Smooks;
import org.milyn.edifact.ect.formats.unedifact.UnEdifactSpecificationReader;
import org.milyn.io.StreamUtils;

import java.io.InputStream;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.milyn.SmooksUtil.filterAndSerialize;

public class FunctionalTestCase {

    @Test
    public void testSmooksConfig() throws Exception {
        InputStream resourceAsStream = getClass().getResourceAsStream("/d03b.zip");
        UnEdifactSpecificationReader unEdifactSpecificationReader = new UnEdifactSpecificationReader(new ZipInputStream(resourceAsStream), true, true);

        Smooks smooks = new Smooks("/smooks-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/ORDERS_D.03B_Interchange.txt"), smooks);

        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/simpleCSV.csv")), result));
    }
}
