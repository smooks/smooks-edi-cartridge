package org.milyn.edifact.edg;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EDIFACTDfdlSchemaGeneratorTestCase {

    @Test
    public void testMain() throws Throwable {
        assertFalse(new File("target/EDIFACT-Segments.dfdl.xsd").exists());
        assertFalse(new File("target/EDIFACT-Messages.dfdl.xsd").exists());

        EDIFACTDfdlSchemaGenerator.main(new String[]{"/d03b.zip", "target/"});

        assertTrue(new File("target/EDIFACT-Segments.dfdl.xsd").exists());
        assertTrue(new File("target/EDIFACT-Messages.dfdl.xsd").exists());
    }
}
