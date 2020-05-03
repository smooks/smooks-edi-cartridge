package org.smooks.cartridges.edi;

import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ValidationMode;
import org.smooks.cartridges.dfdl.DataProcessorFactory;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.annotation.AppContext;
import org.smooks.container.ApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdiDataProcessorFactory extends DataProcessorFactory {

    @AppContext
    protected ApplicationContext applicationContext;

    @Override
    public DataProcessor createDataProcessor() {
        try {
            final Map<String, String> variables = new HashMap<>();
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}SegmentTerm", smooksResourceConfiguration.getStringParameter("segmentTerminator", "'%NL;%WSP*; '%WSP*;"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}FieldSep", smooksResourceConfiguration.getStringParameter("dataElementSeparator", "+"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}CompositeSep", smooksResourceConfiguration.getStringParameter("compositeDataElementSeparator", ":"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}EscapeChar", smooksResourceConfiguration.getStringParameter("escapeCharacter", "?"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}RepeatSep", smooksResourceConfiguration.getStringParameter("repetitionSeparator", "*"));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}DecimalSep", smooksResourceConfiguration.getStringParameter("decimalSign", "."));
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}GroupingSep", smooksResourceConfiguration.getStringParameter("triadSeparator", ","));

            final List<Parameter> variableParameters = smooksResourceConfiguration.getParameters("variables");
            if (variableParameters != null) {
                for (Parameter variableParameter : variableParameters) {
                    final Map.Entry<String, String> variable = (Map.Entry<String, String>) variableParameter.getObjValue();
                    variables.put(variable.getKey(), variable.getValue());
                }
            }

            return doCreateDataProcessor(variables);
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    protected DataProcessor doCreateDataProcessor(final Map<String, String> variables) throws URISyntaxException {
        final DfdlSchema dfdlSchema = new DfdlSchema(new URI(schemaUri), variables, ValidationMode.valueOf(smooksResourceConfiguration.getStringParameter("validationMode", "Off")), smooksResourceConfiguration.getBoolParameter("cacheOnDisk", false), smooksResourceConfiguration.getBoolParameter("debugging", false));
        return compileOrGet(dfdlSchema);
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
