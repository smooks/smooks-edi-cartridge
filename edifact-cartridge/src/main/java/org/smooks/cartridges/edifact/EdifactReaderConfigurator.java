package org.smooks.cartridges.edifact;

import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.edi.EdiReaderConfigurator;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksResourceConfiguration;

import java.util.ArrayList;
import java.util.List;

public class EdifactReaderConfigurator extends EdiReaderConfigurator {

    protected List<String> messageTypes = new ArrayList<>();

    public EdifactReaderConfigurator(final String schemaUri) {
        super(schemaUri);
    }

    protected String getDataProcessorFactory() {
        return "org.smooks.cartridges.edifact.EdifactDataProcessorFactory";
    }

    public List<String> getMessageTypes() {
        return messageTypes;
    }

    public EdifactReaderConfigurator setMessageTypes(List<String> messageTypes) {
        AssertArgument.isNotNull(variables, "messageTypes");
        this.messageTypes = messageTypes;

        return this;
    }

    @Override
    public List<SmooksResourceConfiguration> toConfig() {
        final List<SmooksResourceConfiguration> smooksResourceConfigurations = super.toConfig();
        final SmooksResourceConfiguration smooksResourceConfiguration = smooksResourceConfigurations.get(0);

        for (String messageType : messageTypes) {
            smooksResourceConfiguration.setParameter(new Parameter("messageType", messageType));
        }

        return smooksResourceConfigurations;
    }
}
