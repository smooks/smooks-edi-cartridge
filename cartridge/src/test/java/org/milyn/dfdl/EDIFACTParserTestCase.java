package org.milyn.dfdl;

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ProcessorFactory;
import org.junit.jupiter.api.Test;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.container.MockApplicationContext;
import org.milyn.delivery.sax.SAXHandler;
import org.milyn.dfdl.delivery.AbstractDFDLContentHandlerFactory;
import org.milyn.edifact.EDIFACTParser;
import org.milyn.io.StreamUtils;
import org.milyn.namespace.NamespaceDeclarationStack;
import org.xml.sax.InputSource;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EDIFACTParserTestCase {

    @Test
    public void testParse() throws Exception {
        org.apache.daffodil.japi.Compiler compiler = Daffodil.compiler();
        ProcessorFactory processorFactory = compiler.compileSource(getClass().getResource("/csv.dfdl.xsd").toURI());
        DataProcessor dataProcessor = processorFactory.onPath("/");

        Map<String, DataProcessor> schemas = new HashMap<>();
        schemas.put("classpath:/csv.dfdl.xsd", dataProcessor);

        ExecutionContext executionContext = new Smooks().createExecutionContext();
        executionContext.setAttribute(NamespaceDeclarationStack.class, new NamespaceDeclarationStack());

        StringWriter stringWriter = new StringWriter();
        SAXHandler saxHandler = new SAXHandler(executionContext, stringWriter);

        EDIFACTParser EDIFACTParser = new EDIFACTParser();
        EDIFACTParser.setSchemaURI("classpath:/csv.dfdl.xsd");
        EDIFACTParser.setApplicationContext(new MockApplicationContext());
        EDIFACTParser.setIndent(true);
        EDIFACTParser.setContentHandler(saxHandler);
        EDIFACTParser.getApplicationContext().setAttribute(AbstractDFDLContentHandlerFactory.class, schemas);

        EDIFACTParser.initialize();
        EDIFACTParser.parse(new InputSource(getClass().getResourceAsStream("/simpleCSV.csv")));

        assertEquals(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/simpleCSV.xml")), stringWriter.toString());
    }

    @Test
    public void testParseGivenIndentIsFalse() throws Exception {
        org.apache.daffodil.japi.Compiler compiler = Daffodil.compiler();
        ProcessorFactory processorFactory = compiler.compileSource(getClass().getResource("/csv.dfdl.xsd").toURI());
        DataProcessor dataProcessor = processorFactory.onPath("/");

        Map<String, DataProcessor> schemas = new HashMap<>();
        schemas.put("classpath:/csv.dfdl.xsd", dataProcessor);

        ExecutionContext executionContext = new Smooks().createExecutionContext();
        executionContext.setAttribute(NamespaceDeclarationStack.class, new NamespaceDeclarationStack());

        StringWriter stringWriter = new StringWriter();
        SAXHandler saxHandler = new SAXHandler(executionContext, stringWriter);

        EDIFACTParser EDIFACTParser = new EDIFACTParser();
        EDIFACTParser.setSchemaURI("classpath:/csv.dfdl.xsd");
        EDIFACTParser.setApplicationContext(new MockApplicationContext());
        EDIFACTParser.setIndent(false);
        EDIFACTParser.setContentHandler(saxHandler);
        EDIFACTParser.getApplicationContext().setAttribute(AbstractDFDLContentHandlerFactory.class, schemas);

        EDIFACTParser.initialize();
        EDIFACTParser.parse(new InputSource(getClass().getResourceAsStream("/simpleCSV.csv")));

        assertEquals(StreamUtils.trimLines(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/simpleCSV.xml"))), stringWriter.toString());
    }
}
