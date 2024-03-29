/*-
 * ========================LICENSE_START=================================
 * smooks-edi-sax
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.edi.edisax.model;

import org.smooks.assertion.AssertArgument;
import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.internal.*;
import org.smooks.resource.URIResourceLocator;
import org.smooks.support.XmlUtils;
import org.smooks.xml.XsdDOMValidator;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.List;


/**
 * Digests an edi-message-mapping and populates a {@link Edimap}.
 *
 * @author bardl
 */
public class EDIConfigDigester {

    private URI modelURI;
    private URIResourceLocator importLocator;

    public static final String XSD_V10 = "http://www.milyn.org/schema/edi-message-mapping-1.0.xsd";
    public static final String XSD_V11 = "http://www.milyn.org/schema/edi-message-mapping-1.1.xsd";
    public static final String XSD_V12 = "http://www.milyn.org/schema/edi-message-mapping-1.2.xsd";
    public static final String XSD_V13 = "http://www.milyn.org/schema/edi-message-mapping-1.3.xsd";
    public static final String XSD_V14 = "http://www.milyn.org/schema/edi-message-mapping-1.4.xsd";
    public static final String XSD_V15 = "http://www.milyn.org/schema/edi-message-mapping-1.5.xsd";
    public static final String XSD_V16 = "http://www.milyn.org/schema/edi-message-mapping-1.6.xsd";
    public static final String XSD_V17 = "https://www.smooks.org/schema/edi-message-mapping-1.7.xsd";

    private static final String NAMESPACE_SUFFIX = ":";

    /**
     * Public default Constructor.
     */
    public EDIConfigDigester() {
        importLocator = new URIResourceLocator();
    }

    /**
     * Public constructor.
     *
     * @param modelURI      The model resource URI.
     * @param importBaseURI The base URI for loading imports.
     */
    public EDIConfigDigester(URI modelURI, URI importBaseURI) {
        AssertArgument.isNotNull(importBaseURI, "importBaseURI");
        this.modelURI = modelURI;
        importLocator = new URIResourceLocator();
        importLocator.setBaseURI(importBaseURI);
    }

    /**
     * Digest the XML edi-message-mapping configuration stream.
     *
     * @param stream the edi-message-mapping stream.
     * @return the {@link Edimap}.
     * @throws IOException               Error parsing the XML stream.
     * @throws SAXException              Error parsing the XML stream.
     * @throws EDIConfigurationException Multiple or no namespaces in edi-message-mapping.
     */
    public static Edimap digestConfig(InputStream stream) throws IOException, SAXException, EDIConfigurationException {
        return new EDIConfigDigester().digestEDIConfig(stream);
    }

    /**
     * Digest the XML edi-message-mapping configuration stream.
     *
     * @param stream the edi-message-mapping stream.
     * @return the {@link Edimap}.
     * @throws IOException               Error parsing the XML stream.
     * @throws SAXException              Error parsing the XML stream.
     * @throws EDIConfigurationException Multiple or no namespaces in edi-message-mapping.
     */
    public Edimap digestEDIConfig(InputStream stream) throws IOException, SAXException, EDIConfigurationException {
        Document configDoc;

        try {
            configDoc = XmlUtils.parseStream(stream);
        } catch (ParserConfigurationException ee) {
            throw new SAXException("Unable to parse Smooks configuration.", ee);
        }

        return digestEDIConfig(configDoc);
    }

    /**
     * Digest the XML edi-message-mapping configuration stream.
     *
     * @param stream the edi-message-mapping stream.
     * @return the {@link Edimap}.
     * @throws IOException               Error parsing the XML stream.
     * @throws SAXException              Error parsing the XML stream.
     * @throws EDIConfigurationException Multiple or no namespaces in edi-message-mapping.
     */
    public Edimap digestEDIConfig(Reader stream) throws IOException, SAXException, EDIConfigurationException {
        Document configDoc;

        try {
            configDoc = XmlUtils.parseStream(stream);
        } catch (ParserConfigurationException ee) {
            throw new SAXException("Unable to parse Smooks configuration.", ee);
        }

        return digestEDIConfig(configDoc);
    }

    private Edimap digestEDIConfig(Document configDoc) throws SAXException, EDIConfigurationException, IOException {
        XsdDOMValidator validator = new XsdDOMValidator(configDoc);

        if (validator.getNamespaces().size() == 0) {
            throw new EDIConfigurationException("The edi-message-mapping configuration must contain a namespace.");
        }
        if (validator.getNamespaces().size() > 1) {
            throw new EDIConfigurationException("Unsupported use of multiple configuration namespaces from inside the edi-message-mapping configuration.");
        }

        String ediNS = validator.getNamespaces().get(0).toString();

        validator.validate();

        Edimap edimap = new Edimap(modelURI);

        if (assertValidXSD(ediNS)) {
            digestXSDValidatedConfig(configDoc, edimap, ediNS);
        } else {
            throw new SAXException("Cannot parse edi-message-mapping configuration.  Unsupported default Namespace '" + ediNS + "'.");
        }
        return edimap;
    }

    /**
     * Assert that schema used for validation are valid, i.e. the schema is
     * either edi-message-mapping-1.0 or edi-message-mapping-1.1.
     *
     * @param ediNS the schema used for validation.
     * @return true if ediNS is valid, false otherwise.
     */
    private static boolean assertValidXSD(String ediNS) {
        return XSD_V10.equals(ediNS) || XSD_V11.equals(ediNS) || XSD_V12.equals(ediNS) || XSD_V13.equals(ediNS) || XSD_V14.equals(ediNS) || XSD_V15.equals(ediNS) || XSD_V16.equals(ediNS) || XSD_V17.equals(ediNS);
    }

    /**
     * Digest child elements of edimap.
     *
     * @param configDoc  the Edimap element.
     * @param edimap     the {@link Edimap} to populate.
     * @param schemaName the schema uri.
     * @throws EDIConfigurationException is thrown when unable to retrieve namespace in configuration.
     */
    private void digestXSDValidatedConfig(Document configDoc, Edimap edimap, String schemaName) throws EDIConfigurationException {

        // Check the namespace attribute
        Element documentElement = configDoc.getDocumentElement();

        //Retrieve the namespace for the schema.
        String namespacePrefix = retrieveNamespace(documentElement, schemaName);

        NodeList nodes = documentElement.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeName().equalsIgnoreCase(namespacePrefix + "import")) {
                digestImport(node, edimap);
            } else if (node.getNodeName().equalsIgnoreCase(namespacePrefix + "description")) {
                digestDescription(node, edimap);
            } else if (node.getNodeName().equalsIgnoreCase(namespacePrefix + "delimiters")) {
                digestDelimiters(node, edimap);
            } else if (node.getNodeName().equalsIgnoreCase(namespacePrefix + "segments")) {
                digestSegments(node, edimap, namespacePrefix);
            }
        }
    }

    /**
     * Digest attributes of Delimiter element and populate Delimiter.
     *
     * @param node   the Delimiter element.
     * @param edimap the {@link Edimap} to populate.
     */
    private static void digestDelimiters(Node node, Edimap edimap) {
        Delimiters delimiters = new Delimiters();
        edimap.setDelimiters(delimiters);
        delimiters.setSegment(getAttributeValue(node, "segment"));
        delimiters.setField(getAttributeValue(node, "field"));
        delimiters.setFieldRepeat(getAttributeValue(node, "fieldRepeat"));
        delimiters.setComponent(getAttributeValue(node, "component"));
        delimiters.setSubComponent(getAttributeValue(node, "sub-component"));
        delimiters.setEscape(getAttributeValue(node, "escape"));
        delimiters.setDecimalSeparator(getAttributeValue(node, "decimalSeparator"));
    }

    /**
     * Digest attributes of Description element and populate Description.
     *
     * @param node   the Description element.
     * @param edimap the {@link Edimap} to populate.
     */
    private static void digestDescription(Node node, Edimap edimap) {
        Description description = new Description();
        edimap.setDescription(description);

        description.setName(getAttributeValue(node, "name"));
        description.setVersion(getAttributeValue(node, "version"));

        String namespace = getAttributeValue(node, "namespace");
        if (namespace != null) {
            description.setNamespace(namespace);
        } else {
            description.setNamespace(XMLConstants.NULL_NS_URI);
        }
    }

    /**
     * Digest attributes of Import element and populate Import.
     *
     * @param node   the Import element.
     * @param edimap the {@link Edimap} to populate.
     */
    private void digestImport(Node node, Edimap edimap) {
        Import edimapImport = new Import();
        edimap.getImports().add(edimapImport);

        URI resourceURI = importLocator.resolveURI(getAttributeValue(node, "resource"));
        edimapImport.setResourceURI(resourceURI);

        edimapImport.setNamespace(getAttributeValue(node, "namespace"));
        edimapImport.setTruncatableFields(getNodeValueAsBoolean(node, "truncatableFields"));
        edimapImport.setTruncatableComponents(getNodeValueAsBoolean(node, "truncatableComponents"));
        edimapImport.setTruncatableSegments(getNodeValueAsBoolean(node, "truncatableSegments"));
    }

    /**
     * Digest attributes and child elements of Segments element. Populates Segments.
     *
     * @param node            the Segments element.
     * @param edimap          the {@link Edimap} to populate.
     * @param namespacePrefix the prefix used to name elements in xml.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void digestSegments(Node node, Edimap edimap, String namespacePrefix) throws EDIConfigurationException {
        SegmentGroup segments = new SegmentGroup();
        setValuesForMappingNode(node, segments, namespacePrefix, null);
        segments.setNamespace(edimap.getDescription().getNamespace());
        edimap.setSegments(segments);
        edimap.setIgnoreUnmappedSegments(getNodeValueAsBoolean(node, "ignoreUnmappedSegments"));

        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node currentNode = nodes.item(i);

            digestSegmentGroup(currentNode, edimap.getSegments().getSegments(), namespacePrefix, segments);
        }
    }

    /**
     * Digests attributes and child elements of Segment element.
     *
     * @param node            the Segment element.
     * @param segmentGroup    the {@link SegmentGroup} to populate.
     * @param namespacePrefix the prefix used to name elements in xml.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void digestSegment(Node node, SegmentGroup segmentGroup, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {

        if (segmentGroup instanceof Segment) {
            Segment segment = (Segment) segmentGroup;
            setValuesForSegment(segment, node, namespacePrefix, parent);

            NodeList nodes = node.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node currentNode = nodes.item(i);

                if (currentNode.getNodeName().equalsIgnoreCase(namespacePrefix + "field")) {
                    Field field = new Field();
                    segment.getFields().add(field);
                    digestField(currentNode, field, namespacePrefix, segment);
                } else {
                    digestSegmentGroup(currentNode, segment.getSegments(), namespacePrefix, segment);
                }
            }
        } else {
            segmentGroup.setMaxOccurs(getNodeValueAsInteger(node, "maxOccurs"));
            segmentGroup.setMinOccurs(getNodeValueAsInteger(node, "minOccurs"));
            setValuesForMappingNode(node, segmentGroup, namespacePrefix, parent);

            NodeList nodes = node.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node currentNode = nodes.item(i);

                digestSegmentGroup(currentNode, segmentGroup.getSegments(), namespacePrefix, segmentGroup);
            }
        }
    }

    private boolean digestSegmentGroup(Node currentNode, List<SegmentGroup> segmentGroupList, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {

        if (currentNode.getNodeName().equalsIgnoreCase(namespacePrefix + "segmentGroup")) {
            SegmentGroup segment = new SegmentGroup();
            segment.setDocumentation(getNodeValue(currentNode, namespacePrefix + "documentation"));
            segmentGroupList.add(segment);
            digestSegment(currentNode, segment, namespacePrefix, parent);

            return true;
        } else if (currentNode.getNodeName().equalsIgnoreCase(namespacePrefix + "segment")) {
            Segment segment = new Segment();
            segmentGroupList.add(segment);
            digestSegment(currentNode, segment, namespacePrefix, parent);

            return true;
        }

        return false;
    }

    /**
     * Digests attributes and child elements of Field element.
     *
     * @param node            the Field element.
     * @param field           the {@link Field} to populate
     * @param namespacePrefix the prefix used to name elements in xml.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void digestField(Node node, Field field, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {
        setValuesForField(field, node, namespacePrefix, parent);

        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node currentNode = nodes.item(i);

            if (currentNode.getNodeName().equalsIgnoreCase(namespacePrefix + "component")) {
                Component component = new Component();
                field.getComponents().add(component);
                digestComponent(currentNode, component, namespacePrefix, field);
            }
        }
    }

    /**
     * Digests attributes and child elements of Component element.
     *
     * @param node            the Component element.
     * @param component       the {@link Component} to populate.
     * @param namespacePrefix the prefix used to name elements in xml.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void digestComponent(Node node, Component component, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {
        setValuesForComponent(component, node, namespacePrefix, parent);

        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node currentNode = nodes.item(i);

            if (currentNode.getNodeName().equalsIgnoreCase(namespacePrefix + "sub-component")) {
                SubComponent subComponent = new SubComponent();
                component.getSubComponents().add(subComponent);
                setValuesForSubComponent(currentNode, subComponent, namespacePrefix, component);
            }
        }
    }

    /**
     * Set values in {@link Segment}.
     *
     * @param segment the {@link Segment} to populate.
     * @param node    the Segment element.
     */
    private void setValuesForSegment(Segment segment, Node node, String namespacePrefix, MappingNode parent) {
        segment.setMaxOccurs(getNodeValueAsInteger(node, "maxOccurs"));
        segment.setMinOccurs(getNodeValueAsInteger(node, "minOccurs"));
        segment.setSegcode(getAttributeValue(node, "segcode"));
        segment.setTruncatable(getNodeValueAsBoolean(node, "truncatable"));
        segment.setIgnoreUnmappedFields(getNodeValueAsBoolean(node, "ignoreUnmappedFields"));
        segment.setDescription(getAttributeValue(node, "description"));
        setValuesForMappingNode(node, segment, namespacePrefix, parent);

        if (segment.getNodeTypeRef() == null) {
            // Backward compatibility support.  Schema pre v 1.4 supported a segref
            // on <segment>, which we thn generalised as nodeTypeRef for all node types.
            segment.setNodeTypeRef(getAttributeValue(node, "segref"));
        }
    }

    /**
     * Set values in {@link Field}.
     *
     * @param field the {@link Field} to populate.
     * @param node  the Field element.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void setValuesForField(Field field, Node node, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {
        field.setRequired(getNodeValueAsBoolean(node, "required"));
        field.setTruncatable(getNodeValueAsBoolean(node, "truncatable"));
        setValuesForValueNode(node, field, namespacePrefix, parent);
    }

    /**
     * Set values in {@link Component}.
     *
     * @param component the {@link Component} to populate.
     * @param node      the Component element.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void setValuesForComponent(Component component, Node node, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {
        component.setRequired(getNodeValueAsBoolean(node, "required"));
        component.setTruncatable(getNodeValueAsBoolean(node, "truncatable"));
        setValuesForValueNode(node, component, namespacePrefix, parent);
    }

    /**
     * Set values in {@link SubComponent}.
     *
     * @param node         the {@link SubComponent} to populate.
     * @param subComponent the SubComponent element.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void setValuesForSubComponent(Node node, SubComponent subComponent, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {
        subComponent.setRequired(getNodeValueAsBoolean(node, "required"));
        setValuesForValueNode(node, subComponent, namespacePrefix, parent);
    }

    /**
     * Set values in {@link MappingNode}.
     *
     * @param node        the {@link MappingNode} to populate.
     * @param mappingNode the MappingNode element.
     */
    private void setValuesForMappingNode(Node node, MappingNode mappingNode, String namespacePrefix, MappingNode parent) {
        mappingNode.setName(getAttributeValue(node, "name"));
        mappingNode.setXmltag(getAttributeValue(node, "xmltag"));
        mappingNode.setNodeTypeRef(getAttributeValue(node, "nodeTypeRef"));
        mappingNode.setDocumentation(getNodeValue(node, namespacePrefix + "documentation"));
        mappingNode.setParent(parent);
        if (parent != null) {
            mappingNode.setNamespace(parent.getNamespace());
        }
    }

    /**
     * Set values in {@link ValueNode}.
     *
     * @param node      the {@link ValueNode} to populate.
     * @param valueNode the ValueNode element.
     * @throws EDIConfigurationException is thrown when values are badly formatted.
     */
    private void setValuesForValueNode(Node node, ValueNode valueNode, String namespacePrefix, MappingNode parent) throws EDIConfigurationException {
        setValuesForMappingNode(node, valueNode, namespacePrefix, parent);
        String type = getAttributeValue(node, "dataType");
        if (type != null) {
            valueNode.setDataType(type);
        } else {
            valueNode.setDataType(getAttributeValue(node, "type"));
        }
        valueNode.setMinLength(getNodeValueAsInteger(node, "minLength"));
        valueNode.setMaxLength(getNodeValueAsInteger(node, "maxLength"));
    }

    /**
     * Gets attribute value from node if it exists. Otherwise returns null.
     *
     * @param node the node.
     * @param name the name of the attribute.
     * @return Boolean value if attribute exists in node.
     */
    private static Boolean getNodeValueAsBoolean(Node node, String name) {
        String value = getAttributeValue(node, name);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets attribute value from node if it exists. Otherwise returns null.
     *
     * @param node the node.
     * @param name the name of the attribute.
     * @return Integer value if attribute exists in node.
     */
    private static Integer getNodeValueAsInteger(Node node, String name) {
        String value = getAttributeValue(node, name);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
    }

    /**
     * Gets attribute value from node if it exists. Otherwise returns null.
     *
     * @param node the node.
     * @param name the name of the attribute.
     * @return String value if attribute exists in node.
     */
    private static String getAttributeValue(Node node, String name) {
        return node.getAttributes().getNamedItem(name) != null ? node.getAttributes().getNamedItem(name).getNodeValue() : null;
    }

    /**
     * Gets attribute value from node if it exists. Otherwise returns null.
     *
     * @param node the node.
     * @param name the name of the attribute.
     * @return String value if attribute exists in node.
     */
    private static String getNodeValue(Node node, String name) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeName().equals(name)) {
                return n.getTextContent();
//            } else if (node.hasChildNodes()) {
//                String value = getNodeValue(n, name);
//                if (value != null) {
//                    return value;
//                }
            }
        }
        return null;  //().getNamedItem(name) != null ? node.getAttributes().getNamedItem(name).getNodeValue() : null;
    }

    /**
     * Retrives the namespaceprefix from edimap element in edi-message-mapping.
     *
     * @param documentElement the edimap element.
     * @param schemaName      the schema uri.
     * @return the namespaceprefix used in the edi-message-mapping.
     * @throws EDIConfigurationException is thrown when no namespace exists in configuration.
     */
    private static String retrieveNamespace(Element documentElement, String schemaName) throws EDIConfigurationException {
        NamedNodeMap attributes = documentElement.getAttributes();
        Node node;
        for (int i = 0; i < attributes.getLength(); i++) {
            node = attributes.item(i);
            if (node.getNodeValue().equals(schemaName)) {
                String prefix = node.getNodeName();
                if (prefix.startsWith("xmlns:")) {
                    return node.getNodeName().replace("xmlns:", "") + NAMESPACE_SUFFIX;
                } else if (prefix.startsWith("xmlns")) {
                    return node.getNodeName().replace("xmlns", "");
                } else {
                    throw new EDIConfigurationException("No namespace exists in edi-message-mapping. A namespace must be declared in order to digest configuration file.");
                }
            }
        }
        return "";
    }
}
