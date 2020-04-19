package org.smooks.cartridges.edifact;

import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.edi.EdiReaderConfigurator;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksResourceConfiguration;

import java.util.List;

public class EdifactReaderConfigurator extends EdiReaderConfigurator {

    protected final List<String> messages;

    public EdifactReaderConfigurator(final String schemaUri, final List<String> messages) {
        super(schemaUri);
        AssertArgument.isNotNull(variables, "messages");

        this.messages = messages;
    }

    protected String getDataProcessorFactory() {
        return "org.smooks.cartridges.edifact.EdifactDataProcessorFactory";
    }

    @Override
    public List<SmooksResourceConfiguration> toConfig() {
        final List<SmooksResourceConfiguration> smooksResourceConfigurations = super.toConfig();
        final SmooksResourceConfiguration smooksResourceConfiguration = smooksResourceConfigurations.get(0);

        for (String message : messages) {
            smooksResourceConfiguration.setParameter(new Parameter("message", message));
        }

        return smooksResourceConfigurations;
    }
}
