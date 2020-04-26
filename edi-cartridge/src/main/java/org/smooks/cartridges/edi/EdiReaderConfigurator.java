package org.smooks.cartridges.edi;

import org.smooks.GenericReaderConfigurator;
import org.smooks.ReaderConfigurator;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.dfdl.parser.DfdlParser;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksResourceConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdiReaderConfigurator implements ReaderConfigurator {

    protected final String schemaUri;

    protected Boolean validateDFDLSchemas = false;
    protected Boolean indent = false;
    protected String targetProfile;
    protected String segmentTerminator = "'%NL;%WSP*; '%WSP*;";
    protected String dataElementSeparator = "+";
    protected String compositeDataElementSeparator = ":";
    protected String escapeCharacter = "?";
    protected String repetitionSeparator ="*";
    protected String decimalSign = ".";
    protected String triadSeparator = ",";
    protected Map<String, String> variables = new HashMap<>();

    public EdiReaderConfigurator(final String schemaUri) {
        AssertArgument.isNotNullAndNotEmpty(schemaUri, "schemaUri");
        this.schemaUri = schemaUri;
    }

    public EdiReaderConfigurator setTargetProfile(String targetProfile) {
        AssertArgument.isNotNullAndNotEmpty(targetProfile, "targetProfile");
        this.targetProfile = targetProfile;
        return this;
    }

    public EdiReaderConfigurator setValidateDFDLSchemas(Boolean validateDFDLSchemas) {
        AssertArgument.isNotNull(validateDFDLSchemas, "validateDFDLSchemas");
        this.validateDFDLSchemas = validateDFDLSchemas;
        return this;
    }

    public EdiReaderConfigurator setVariables(Map<String, String> variables) {
        AssertArgument.isNotNull(variables, "variables");
        this.variables = variables;
        return this;
    }

    public EdiReaderConfigurator setIndent(Boolean indent) {
        AssertArgument.isNotNull(indent, "indent");
        this.indent = indent;
        return this;
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

    protected String getDataProcessorFactory() {
        return "org.smooks.cartridges.edi.EdiDataProcessorFactory";
    }

    @Override
    public List<SmooksResourceConfiguration> toConfig() {
        final GenericReaderConfigurator genericReaderConfigurator = new GenericReaderConfigurator(DfdlParser.class);

        genericReaderConfigurator.getParameters().setProperty("schemaURI", schemaUri);
        genericReaderConfigurator.getParameters().setProperty("validateDFDLSchemas", Boolean.toString(validateDFDLSchemas));
        genericReaderConfigurator.getParameters().setProperty("indent", Boolean.toString(indent));
        genericReaderConfigurator.getParameters().setProperty("dataProcessorFactory", getDataProcessorFactory());
        genericReaderConfigurator.getParameters().setProperty("segmentTerminator", segmentTerminator);
        genericReaderConfigurator.getParameters().setProperty("dataElementSeparator", dataElementSeparator);
        genericReaderConfigurator.getParameters().setProperty("compositeDataElementSeparator", compositeDataElementSeparator);
        genericReaderConfigurator.getParameters().setProperty("escapeCharacter", escapeCharacter);
        genericReaderConfigurator.getParameters().setProperty("repetitionSeparator", repetitionSeparator);
        genericReaderConfigurator.getParameters().setProperty("decimalSign", decimalSign);
        genericReaderConfigurator.getParameters().setProperty("triadSeparator", triadSeparator);

        final List<SmooksResourceConfiguration> smooksResourceConfigurations = genericReaderConfigurator.toConfig();
        final SmooksResourceConfiguration smooksResourceConfiguration = smooksResourceConfigurations.get(0);

        for (Map.Entry<String, String> variable : variables.entrySet()) {
            smooksResourceConfiguration.setParameter(new Parameter("variables", variable));
        }

        smooksResourceConfiguration.setTargetProfile(targetProfile);

        return smooksResourceConfigurations;
    }
}
