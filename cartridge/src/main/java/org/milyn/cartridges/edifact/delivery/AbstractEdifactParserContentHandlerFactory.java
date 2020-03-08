package org.milyn.cartridges.edifact.delivery;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.util.Misc;
import org.milyn.cartridges.dfdl.delivery.AbstractDFDLContentHandlerFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Resource(type = "edifact-parser")
public abstract class AbstractEdifactParserContentHandlerFactory extends AbstractDFDLContentHandlerFactory {

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

            final String dataProcessorName = schemaURIParameter.getValue() + ":" + String.join(":", messages.toArray(new String[]{}));
            final DataProcessor dataProcessor = compileOrGet(dataProcessorName, generatedSchema.toURI().toString(), resourceConfig.getBoolParameter("validateDFDLSchemas", false));

            return doCreate(resourceConfig, dataProcessorName, dataProcessor);
        } catch (Throwable t) {
            throw new SmooksConfigurationException(t);
        }
    }

    private String readVersion(final Parameter schemaURIParameter) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = documentBuilder.parse(Misc.getRequiredResource(schemaURIParameter.getValue()).toString());

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();

        return  (String) xpath.compile("/schema/annotation/appinfo[@source='http://www.ibm.com/dfdl/edi/un/edifact']/text()").evaluate(document, XPathConstants.STRING);
    }
}
