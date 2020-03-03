package org.milyn.cartridges.edifact.delivery;

import org.apache.daffodil.japi.DataProcessor;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.delivery.ContentHandler;
import org.milyn.delivery.annotation.Resource;

@Resource(type = "edifact-parser")
public class EDIFACTParserContentHandlerFactory extends AbstractEDIFACTParserContentHandlerFactory {

    @Override
    public ContentHandler doCreate(SmooksResourceConfiguration resourceConfig, String dataProcessorName, DataProcessor dataProcessor) {
        resourceConfig.setParameter("dataProcessorName", dataProcessorName);
        resourceConfig.setResource("org.milyn.cartridges.dfdl.DFDLParser");
        return null;
    }
}
