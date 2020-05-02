package org.smooks.edi.edg;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.io.FileUtils;
import org.apache.daffodil.tdml.Runner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import scala.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EdifactDfdlSchemaGeneratorTestCase {

    @BeforeEach
    public void beforeEach() throws IOException {
        FileUtils.deleteQuietly(new File("target/generated-test-resources"));
    }

    @Test
    public void testMain() throws Throwable {
        assertFalse(new File("target/generated-test-resources/d03b/EDIFACT-Interchange.dfdl.xsd").exists());
        assertFalse(new File("target/generated-test-resources/d03b/EDIFACT-Messages.dfdl.xsd").exists());
        assertFalse(new File("target/generated-test-resources/d03b/EDIFACT-Segments.dfdl.xsd").exists());

        EdifactDfdlSchemaGenerator.main(new String[]{"/d03b.zip", "target/generated-test-resources"});

        assertTrue(new File("target/generated-test-resources/d03b/EDIFACT-Interchange.dfdl.xsd").exists());
        assertTrue(new File("target/generated-test-resources/d03b/EDIFACT-Messages.dfdl.xsd").exists());
        assertTrue(new File("target/generated-test-resources/d03b/EDIFACT-Segments.dfdl.xsd").exists());
    }

    @Test
    public void testDfdlSchema() throws Throwable {
        EdifactDfdlSchemaGenerator.main(new String[]{"/d03b.zip", "target/generated-test-resources"});
        Runner runner = new Runner(null, "", "parse.tdml", true, true, false, Runner.defaultRoundTripDefaultDefault(), Runner.defaultValidationDefaultDefault(), Runner.defaultImplementationsDefaultDefault());
        runner.runOneTest("PAXLST", Option.empty(), true);
        runner.runOneTest("INVOIC", Option.empty(), true);
    }
}
