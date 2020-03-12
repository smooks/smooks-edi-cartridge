package org.milyn.cartridges.edi.delivery;

import org.apache.daffodil.japi.DataProcessor;
import org.milyn.cartridges.dfdl.DfdlSchema;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.delivery.ContentHandler;
import org.milyn.delivery.annotation.Resource;

@Resource(type = "edi-parser")
public class EdiParserContentHandlerFactory extends AbstractEdiParserContentHandlerFactory {
    @Override
    public ContentHandler doCreate(final SmooksResourceConfiguration smooksResourceConfiguration, final DfdlSchema dfdlSchema, final DataProcessor dataProcessor) {
        smooksResourceConfiguration.setParameter("dataProcessorName", dfdlSchema.getName());
        smooksResourceConfiguration.setResource("org.milyn.cartridges.dfdl.DfdlParser");
        return new ContentHandler() {
        };
    }
}
