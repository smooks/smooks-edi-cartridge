package org.smooks.cartridges.edi;

import org.apache.daffodil.japi.ValidationMode;
import org.smooks.GenericReaderConfigurator;
import org.smooks.ReaderConfigurator;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.dfdl.parser.DfdlParser;
import org.smooks.cartridges.dfdl.parser.DfdlReaderConfigurator;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksResourceConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdiReaderConfigurator extends DfdlReaderConfigurator {

    protected String segmentTerminator = "'%NL;%WSP*; '%WSP*;";
    protected String dataElementSeparator = "+";
    protected String compositeDataElementSeparator = ":";
    protected String escapeCharacter = "?";
    protected String repetitionSeparator ="*";
    protected String decimalSign = ".";
    protected String triadSeparator = ",";

    public EdiReaderConfigurator(final String schemaUri) {
        super(schemaUri);
    }

    public EdiReaderConfigurator setSegmentTerminator(String segmentTerminator) {
        AssertArgument.isNotNullAndNotEmpty(segmentTerminator, "segmentTerminator");
        this.segmentTerminator = segmentTerminator;
        return this;
    }

    public EdiReaderConfigurator setDataElementSeparator(String dataElementSeparator) {
        AssertArgument.isNotNullAndNotEmpty(dataElementSeparator, "dataElementSeparator");
        this.dataElementSeparator = dataElementSeparator;
        return this;
    }

    public EdiReaderConfigurator setCompositeDataElementSeparator(String compositeDataElementSeparator) {
        AssertArgument.isNotNullAndNotEmpty(compositeDataElementSeparator, "compositeDataElementSeparator");
        this.compositeDataElementSeparator = compositeDataElementSeparator;
        return this;
    }

    public EdiReaderConfigurator setEscapeCharacter(String escapeCharacter) {
        AssertArgument.isNotNullAndNotEmpty(escapeCharacter, "escapeCharacter");
        this.escapeCharacter = escapeCharacter;
        return this;
    }

    public EdiReaderConfigurator setRepetitionSeparator(String repetitionSeparator) {
        AssertArgument.isNotNullAndNotEmpty(repetitionSeparator, "repetitionSeparator");
        this.repetitionSeparator = repetitionSeparator;
        return this;
    }

    public EdiReaderConfigurator setDecimalSign(String decimalSign) {
        AssertArgument.isNotNullAndNotEmpty(decimalSign, "decimalSign");
        this.decimalSign = decimalSign;
        return this;
    }

    public EdiReaderConfigurator setTriadSeparator(String triadSeparator) {
        AssertArgument.isNotNullAndNotEmpty(triadSeparator, "triadSeparator");
        this.triadSeparator = triadSeparator;
        return this;
    }

    @Override
    protected String getDataProcessorFactory() {
        return "org.smooks.cartridges.edi.EdiDataProcessorFactory";
    }

    @Override
    public List<SmooksResourceConfiguration> toConfig() {
        final List<SmooksResourceConfiguration> smooksResourceConfigurations = super.toConfig();
        final SmooksResourceConfiguration smooksResourceConfiguration = smooksResourceConfigurations.get(0);

        smooksResourceConfiguration.setParameter(new Parameter("dataProcessorFactory", getDataProcessorFactory()));
        smooksResourceConfiguration.setParameter(new Parameter("segmentTerminator", segmentTerminator));
        smooksResourceConfiguration.setParameter(new Parameter("dataElementSeparator", dataElementSeparator));
        smooksResourceConfiguration.setParameter(new Parameter("compositeDataElementSeparator", compositeDataElementSeparator));
        smooksResourceConfiguration.setParameter(new Parameter("escapeCharacter", escapeCharacter));
        smooksResourceConfiguration.setParameter(new Parameter("repetitionSeparator", repetitionSeparator));
        smooksResourceConfiguration.setParameter(new Parameter("decimalSign", decimalSign));
        smooksResourceConfiguration.setParameter(new Parameter("triadSeparator", triadSeparator));

        return smooksResourceConfigurations;
    }
}
