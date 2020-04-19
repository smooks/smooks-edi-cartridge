package org.smooks.cartridges.edifact;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.util.Misc;
import org.smooks.cartridges.dfdl.DfdlSchema;
import org.smooks.cartridges.edi.EdiDataProcessorFactory;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.annotation.AppContext;
import org.smooks.container.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EdifactDataProcessorFactory extends EdiDataProcessorFactory {

    private static final Mustache MUSTACHE;

    static {
        try {
            MUSTACHE = new DefaultMustacheFactory().compile("EDIFACT-Common/EDIFACT-Message.dfdl.xsd.mustache");
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    @AppContext
    protected ApplicationContext applicationContext;

    @Override
    public DataProcessor doCreateDataProcessor(final Map<String, String> variables) {
        try {
            final Parameter schemaURIParameter = smooksResourceConfiguration.getParameter("schemaURI");
            final String version = readVersion(schemaURIParameter);

            final List<String> messages = smooksResourceConfiguration.getParameters("message").stream().map(m -> m.getValue()).collect(Collectors.toList());

            final File generatedSchema = File.createTempFile("EDIFACT-Message", ".dfdl.xsd");
            try (FileWriter fileWriter = new FileWriter(generatedSchema)) {
                MUSTACHE.execute(fileWriter, new HashMap<String, Object>() {{
                    this.put("schemaLocation", schemaURIParameter.getValue());
                    this.put("messages", messages);
                    this.put("version", version);
                }});
            }

            final DfdlSchema dfdlSchema = new DfdlSchema(generatedSchema.toURI(), new HashMap<>(), smooksResourceConfiguration.getBoolParameter("validateDFDLSchemas", false)) {
                @Override
                public String getName() {
                    return schemaURIParameter.getValue() + ":" + isValidateSchemas() + ":" + getVariables().toString();
                }
            };

            return compileOrGet(dfdlSchema);
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    protected String readVersion(final Parameter schemaURIParameter) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = documentBuilder.parse(Misc.getRequiredResource(schemaURIParameter.getValue()).toString());

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();

        return (String) xpath.compile("/schema/annotation/appinfo[@source='http://www.ibm.com/dfdl/edi/un/edifact']/text()").evaluate(document, XPathConstants.STRING);
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
