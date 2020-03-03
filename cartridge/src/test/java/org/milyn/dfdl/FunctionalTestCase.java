package org.milyn.dfdl;

import org.junit.jupiter.api.Test;
import org.milyn.Smooks;

import static org.milyn.SmooksUtil.filterAndSerialize;

public class FunctionalTestCase {

    @Test
    public void testSmooksConfig() throws Exception {
        Smooks smooks = new Smooks("/smooks-config.xml");
        String result = filterAndSerialize(smooks.createExecutionContext(), getClass().getResourceAsStream("/INVOIC_D.03B_Interchange_with_UNA.txt"), smooks);

        System.out.println(result);
//        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/ORDERS_D.03B_Interchange.txt")), result));
    }
}
