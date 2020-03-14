package org.smooks.cartridges.edi.delivery.edifact;

import org.apache.daffodil.japi.DataProcessor;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.delivery.ContentHandler;
import org.smooks.delivery.annotation.Resource;

@Resource(type = "edifact-parser")
public class EdifactParserContentHandlerFactory extends AbstractEdifactParserContentHandlerFactory {

    @Override
    public ContentHandler doCreate(final SmooksResourceConfiguration smooksResourceConfiguration, final DfdlSchema dfdlSchema, final DataProcessor dataProcessor) {
        smooksResourceConfiguration.setParameter("dataProcessorName", dfdlSchema.getName());
        smooksResourceConfiguration.setResource("org.smooks.cartridges.dfdl.DfdlParser");
        return new ContentHandler() {
        };
    }
}
