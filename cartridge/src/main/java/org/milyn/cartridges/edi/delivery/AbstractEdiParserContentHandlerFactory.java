package org.milyn.cartridges.edi.delivery;

import org.apache.daffodil.japi.DataProcessor;
import org.milyn.cartridges.dfdl.DfdlSchema;
import org.milyn.cartridges.dfdl.delivery.AbstractDfdlContentHandlerFactory;
import org.milyn.cdr.Parameter;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.delivery.ContentHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEdiParserContentHandlerFactory extends AbstractDfdlContentHandlerFactory {

    @Override
    public ContentHandler create(final SmooksResourceConfiguration smooksResourceConfiguration) throws SmooksConfigurationException {
        try {
            final Parameter schemaURIParameter = smooksResourceConfiguration.getParameter("schemaURI");
            final Map<String, String> variables = new HashMap<>();
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}SegmentTerm", smooksResourceConfiguration.getStringParameter("segmentTerminator", "'%NL;%WSP*; '%WSP*; %NL;%WSP*;"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}FieldSep", smooksResourceConfiguration.getStringParameter("dataElementSeparator", "+"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}CompositeSep", smooksResourceConfiguration.getStringParameter("compositeDataElementSeparator", ":"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}EscapeChar", smooksResourceConfiguration.getStringParameter("escapeCharacter", "?"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}RepeatSep", smooksResourceConfiguration.getStringParameter("repetitionSeparator", "*"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}DecimalSep", smooksResourceConfiguration.getStringParameter("decimalSign", "."));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}GroupingSep", smooksResourceConfiguration.getStringParameter("triadSeparator", ","));
            List<Parameter> variableParameters = smooksResourceConfiguration.getParameters("variables");
            if (variableParameters != null){
                for (Parameter variableParameter : variableParameters) {
                    Map.Entry<String, String> variable = (Map.Entry<String, String>) variableParameter.getObjValue();
                    variables.put(variable.getKey(), variable.getValue());
                }
            }

            final DfdlSchema dfdlSchema = new DfdlSchema(new URI(schemaURIParameter.getValue()), variables, smooksResourceConfiguration.getBoolParameter("validateDFDLSchemas", false));
            final DataProcessor dataProcessor = compileOrGet(dfdlSchema);
            return doCreate(smooksResourceConfiguration, dfdlSchema, dataProcessor);
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }
}
