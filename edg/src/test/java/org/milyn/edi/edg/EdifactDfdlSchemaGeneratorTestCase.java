package org.milyn.edi.edg;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EdifactDfdlSchemaGeneratorTestCase {

    @Test
    public void testMain() throws Throwable {
        assertFalse(new File("target/d03b/EDIFACT-Segments.dfdl.xsd").exists());
        assertFalse(new File("target/d03b/EDIFACT-Messages.dfdl.xsd").exists());

        EdifactDfdlSchemaGenerator.main(new String[]{"/d03b.zip", "target/"});

        assertTrue(new File("target/d03b/EDIFACT-Segments.dfdl.xsd").exists());
        assertTrue(new File("target/d03b/EDIFACT-Messages.dfdl.xsd").exists());
    }
}
