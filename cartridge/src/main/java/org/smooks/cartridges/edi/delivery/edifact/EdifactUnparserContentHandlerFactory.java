package org.smooks.cartridges.edi.delivery.edifact;

import org.apache.daffodil.japi.DataProcessor;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cartridges.dfdl.DfdlUnparser;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.annotation.Configurator;
import org.smooks.delivery.ContentHandler;
import org.smooks.delivery.annotation.Resource;

@Resource(type = "edifact-unparser")
public class EdifactUnparserContentHandlerFactory extends AbstractEdifactParserContentHandlerFactory {

    @Override
    public ContentHandler doCreate(final SmooksResourceConfiguration smooksResourceConfiguration, final DfdlSchema dfdlSchema, final DataProcessor dataProcessor) {
        return Configurator.configure(new DfdlUnparser(dataProcessor), smooksResourceConfiguration, applicationContext);
    }
}
