package org.milyn.dfdl;

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ProcessorFactory;
import org.junit.jupiter.api.Test;
import org.milyn.container.MockExecutionContext;
//import org.milyn.edifact.DFDLUnparser;
import org.milyn.io.StreamUtils;
import org.milyn.xml.XmlUtil;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DFDLUnparserTestCase {

    @Test
    public void testVisitAfter() throws Exception {
        org.apache.daffodil.japi.Compiler compiler = Daffodil.compiler();
        ProcessorFactory processorFactory = compiler.compileSource(getClass().getResource("/csv.dfdl.xsd").toURI());
        DataProcessor dataProcessor = processorFactory.onPath("/");

        Document document = XmlUtil.parseStream(getClass().getResourceAsStream("/simpleCSV.xml"));

        DFDLUnparser dfdlUnparser = new DFDLUnparser(dataProcessor);
        dfdlUnparser.visitAfter(document.getDocumentElement(), new MockExecutionContext());

        assertTrue(StreamUtils.compareCharStreams(StreamUtils.readStreamAsString(getClass().getResourceAsStream("/simpleCSV.csv")), document.getDocumentElement().getTextContent()));
    }

}
