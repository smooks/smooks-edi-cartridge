package org.milyn.cartridges.edifact.delivery;

import org.apache.daffodil.japi.DataProcessor;
import org.milyn.cartridges.dfdl.DFDLUnparser;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.cdr.annotation.Configurator;
import org.milyn.delivery.ContentHandler;
import org.milyn.delivery.annotation.Resource;

@Resource(type = "edifact-unparser")
public class EDIFACTUnparserContentHandlerFactory extends AbstractEDIFACTParserContentHandlerFactory {

    @Override
    public ContentHandler doCreate(final SmooksResourceConfiguration resourceConfig, final String dataProcessorName, final DataProcessor dataProcessor) {
        return Configurator.configure(new DFDLUnparser(dataProcessor), resourceConfig, applicationContext);

    }
}
