package org.milyn.cartridges.edi.delivery.edifact;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.util.Misc;
import org.milyn.cartridges.dfdl.DfdlSchema;
import org.milyn.cartridges.dfdl.delivery.AbstractDfdlContentHandlerFactory;
import org.milyn.cdr.Parameter;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.delivery.ContentHandler;
import org.milyn.delivery.annotation.Resource;
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
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractEdifactParserContentHandlerFactory extends AbstractDfdlContentHandlerFactory {

    private static final Mustache MUSTACHE;

    static {
        try {
            MUSTACHE = new DefaultMustacheFactory().compile("EDIFACT-Common/EDIFACT-Message.dfdl.xsd.mustache");
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    @Override
    public ContentHandler create(final SmooksResourceConfiguration resourceConfig) throws SmooksConfigurationException {
        try {
            final Parameter schemaURIParameter = resourceConfig.getParameter("schemaURI");
            final String version = readVersion(schemaURIParameter);

            final List<String> messages = resourceConfig.getParameters("message").stream().map(m -> m.getValue()).collect(Collectors.toList());

            final File generatedSchema = File.createTempFile("EDIFACT-Message", ".dfdl.xsd");
            try (FileWriter fileWriter = new FileWriter(generatedSchema)) {
                MUSTACHE.execute(fileWriter, new HashMap<String, Object>() {{
                    this.put("schemaLocation", schemaURIParameter.getValue());
                    this.put("messages", messages);
                    this.put("version", version);
                }});
            }

            final DfdlSchema dfdlSchema = new DfdlSchema(generatedSchema.toURI(), new HashMap<>(), resourceConfig.getBoolParameter("validateDFDLSchemas", false)) {
                @Override
                public String getName() {
                    return schemaURIParameter.getValue() + ":" + isValidateSchemas() + ":" + getVariables().toString();
                }
            };

            final DataProcessor dataProcessor = compileOrGet(dfdlSchema);

            return doCreate(resourceConfig, dfdlSchema, dataProcessor);
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    private String readVersion(final Parameter schemaURIParameter) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = documentBuilder.parse(Misc.getRequiredResource(schemaURIParameter.getValue()).toString());

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();

        return (String) xpath.compile("/schema/annotation/appinfo[@source='http://www.ibm.com/dfdl/edi/un/edifact']/text()").evaluate(document, XPathConstants.STRING);
    }
}
