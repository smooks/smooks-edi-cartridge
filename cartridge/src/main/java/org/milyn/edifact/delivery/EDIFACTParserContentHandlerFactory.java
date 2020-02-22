package org.milyn.edifact.delivery;

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ProcessorFactory;
import org.milyn.SmooksException;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.delivery.ContentHandler;
import org.milyn.delivery.annotation.Resource;
import org.milyn.dfdl.DFDLParser;
import org.milyn.dfdl.delivery.AbstractDFDLContentHandlerFactory;
import org.milyn.resource.URIResourceLocator;
import org.milyn.util.ClassUtil;
import scala.Predef;
import scala.collection.JavaConverters;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Resource(type = "edifact-parser")
public class EDIFACTParserContentHandlerFactory extends AbstractDFDLContentHandlerFactory {

    @Override
    public ContentHandler doCreate(final SmooksResourceConfiguration resourceConfig, final DataProcessor dataProcessor) throws SmooksConfigurationException {
        resourceConfig.setResource("org.milyn.edifact.EDIFACTParser");
        return null;
    }
}
