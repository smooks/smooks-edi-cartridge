package org.smooks.edi.edg;

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ProcessorFactory;
import org.smooks.edi.ect.formats.unedifact.UnEdifactSpecificationReader;
import org.smooks.edi.edg.template.InterchangeTemplate;
import org.smooks.edi.edg.template.MessagesTemplate;
import org.smooks.edi.edg.template.SegmentsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

public final class EdifactDfdlSchemaGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdifactDfdlSchemaGenerator.class);
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();


    private EdifactDfdlSchemaGenerator() {

    }

    public static void main(final String[] args) {
        Arrays.stream(Arrays.copyOfRange(args, 0, args.length - 1)).parallel().forEach(s -> {
            try {
                LOGGER.info("Generating schema from {}...", s);
                generateDfdlSchemas(s, args[args.length - 1]);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void generateDfdlSchemas(String spec, String outputDirectory) throws Throwable {
        final InputStream resourceAsStream = EdifactDfdlSchemaGenerator.class.getResourceAsStream(spec);
        final UnEdifactSpecificationReader unEdifactSpecificationReader = new UnEdifactSpecificationReader(new ZipInputStream(resourceAsStream), true, true);

        final String[] namespace = unEdifactSpecificationReader.getDefinitionModel().getDescription().getNamespace().split(":");
        final String version = namespace[3].replace("-", "").toUpperCase();
        final String versionOutputDirectory = outputDirectory + "/" + version.toLowerCase();

        new File(versionOutputDirectory).mkdirs();

        final String segmentsSchema = new SegmentsTemplate(version, unEdifactSpecificationReader).materialise();
        final File segmentSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Segments.dfdl.xsd");
        write(segmentsSchema, segmentSchemaFile);

        final MessagesTemplate messagesTemplate = new MessagesTemplate(version, unEdifactSpecificationReader);
        String messagesSchema = messagesTemplate.materialise();
        final File messageSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Messages.dfdl.xsd");
        write(messagesSchema, messageSchemaFile);

        final File interchangeSchemaFile = new File(versionOutputDirectory + "/EDIFACT-Interchange.dfdl.xsd");
        String interchangeSchema = new InterchangeTemplate(version, messagesTemplate.getMessageTypes()).materialise();
        write(interchangeSchema, interchangeSchemaFile);

        LOGGER.info("Validating schema {}...", messageSchemaFile.getPath());
        final ProcessorFactory processorFactory = Daffodil.compiler().compileFile(messageSchemaFile);
        for (Diagnostic diagnostic : processorFactory.getDiagnostics()) {
            if (diagnostic.isError()) {
                throw diagnostic.getSomeCause();
            }
        }
    }

    private static void write(final String xml, final File file) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        final DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        final Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
        stripWhitespaceTextNodes(document);

        final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");

        final FileWriter fileWriter = new FileWriter(file);
        transformer.transform(new DOMSource(document), new StreamResult(fileWriter));
    }

    private static void stripWhitespaceTextNodes(final Document document) throws XPathExpressionException {
        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
        final NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(document, XPathConstants.NODESET);

        for (int i = 0; i < emptyTextNodes.getLength(); i++) {
            final Node emptyTextNode = emptyTextNodes.item(i);
            emptyTextNode.getParentNode().removeChild(emptyTextNode);
        }
    }
}
