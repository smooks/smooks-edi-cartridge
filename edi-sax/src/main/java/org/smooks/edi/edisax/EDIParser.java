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
package org.smooks.edi.edisax;

import org.smooks.assertion.AssertArgument;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.model.internal.*;
import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.namespace.NamespaceDeclarationStack;
import org.smooks.resource.URIResourceLocator;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.XMLConstants;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EDI Parser.
 * <br><br>
 * Generates a stream of SAX events from an EDI message stream based on the supplied
 * {@link #setMappingModel(EdifactModel) mapping model}.
 *
 * <h3>Usage</h3>
 * <pre>
 * 	InputStream ediInputStream = ....
 * 	InputStream <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi2SaxMappingConfig</a> = ....
 *    {@link ContentHandler} contentHandler = ....
 *
 * 	EDIParser parser = new EDIParser();
 *
 * 	parser.setContentHandler(contentHandler);
 * 	parser.{@link #setMappingModel(EdifactModel) setMappingModel}(EDIParser.{@link #parseMappingModel(InputStream) parseMappingModel}(<a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi2SaxMappingConfig</a>));
 * 	parser.parse(new InputSource(ediInputStream));
 * 	etc...
 * </pre>
 *
 * <h3>Mapping Model</h3>
 * The EDI to SAX Event mapping is performed based on an "Mapping Model" supplied to
 * the parser.  This model must be based on the
 * <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi-message-mapping-1.0.xsd</a>
 * schema.
 * <br><br>
 * From this schema you can see that segment groups are supported (nested segments), including groups within groups,
 * repeating segments and repeating segment groups.  Be sure to review the
 * <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">schema</a>.
 *
 * <h3>Example (Input EDI, EDI to XML Mapping and Output SAX Events)</h3>
 * The following illustration attempts to create a visualisation of the mapping process.  The "input-message.edi" file
 * specifies the EDI input, "edi-to-xml-order-mapping.xml" describes how to map that EDI message to SAX events and
 * "expected.xml" illustrates the XML that would result from applying the mapping.
 * <br><br>
 * <img src="doc-files/edi-mapping.png" />
 * <br><br>
 * So the above illustration attempts to highlight the following:
 * <ol>
 * 	<li>How the message delimiters (segment, field, component and sub-component) are specified in the mapping.  In particular, how special
 * 		characters like the linefeed character are specified using XML Character References.</li>
 * 	<li>How segment groups (nested segments) are specified.  In this case the first 2 segments are part of a group.</li>
 * 	<li>How the actual field, component and sub-component values are specified and mapped to the target SAX events (to generate the XML).</li>
 * </ol>
 *
 * <h3>Segment Cardinality</h3>
 * What's not shown above is how the &lt;medi:segment&gt; element supports the 2 optional attributes "minOccurs" and
 * "maxOccurs" (default value of 1 in both cases).  These attributes can be used to control the optional and required
 * characteristics of a segment.  A maxOccurs value of -1 indicates that the segment can repeat any number of times
 * in that location of the EDI message (unbounded).
 *
 * <h3>Segment Groups</h3>
 * Segment groups can be added using the &lt;segmentGroup&gt; element.  A Segment group is matched by the first segment
 * in the group.  A Segment Group can contain nested &lt;segmentGroup&gt; elements, but the first element in a &lt;segmentGroup&gt;
 * must be a &lt;segment&gt;.  &lt;segmentGroup&gt; elements support minOccurs/maxOccurs cardinality.  They also support
 * an optional "xmlTag" attribute, when if present will result in the XML generated by a matched segment group
 * being inserted inside an element having the name of the xmlTag attribute value.
 *
 * <h3>Segment Matching</h3>
 * Segments are matched in one of 2 ways:
 * <ol>
 *  <li>By an exact match on the segment code (segcode).</li>
 *  <li>By a {@link java.util.regex regex pattern match} on the full segment, where the segcode attribute defines the
 *      regex pattern (e.g. <i>segcode="1A\*a.*"</i>).</li>
 * </ol>
 *
 * <h3>Required Values</h3>
 * &lt;field&gt;, &lt;component&gt; and &lt;sub-component&gt; configurations support a "required" attribute, which
 * flags that &lt;field&gt;, &lt;component&gt; or &lt;sub-component&gt; as requiring a value.
 * <br><br>
 * By default, values are not required (fields, components and sub-components).
 *
 * <h3>Truncation</h3>
 * &lt;segment&gt;, &lt;field&gt; and &lt;component&gt; configurations support a "truncatable" attribute.  For a
 * segment, this means that parser errors will not be generated when that segment does not specify trailing
 * fields that are not "required" (see "required" attribute above). Likewise for fields/components and
 * components/sub-components.
 * <br><br>
 * By default, segments, fields, and components are not truncatable.
 *
 * @author tfennelly
 */
public class EDIParser implements XMLReader {

    public static final String FEATURE_VALIDATE = "http://xml.org/sax/features/validation";
    public static final String FEATURE_IGNORE_NEWLINES = "http://xml.org/sax/features/ignore-newlines";
    public static final String FEATURE_IGNORE_EMPTY_NODES = "http://smooks.org/edi/sax/features/ignore-empty-nodes";
    private static final Attributes EMPTY_ATTRIBS = new AttributesImpl();

    private Map<String, Boolean> features;

    private NamespaceDeclarationStack nsStack;

    private ContentHandler contentHandler;
    private Integer indentDepth;
    private static final Pattern EMPTY_LINE = Pattern.compile("[\n\r ]*");

    private EdifactModel edifactModel;
    private BufferedSegmentReader segmentReader;
    private Boolean ignoreEmptyNodes;

    /**
     * Set the {@link NamespaceDeclarationStack} to be used by the reader instance.
     *
     * @param nsStack The {@link NamespaceDeclarationStack} to be used by the reader instance.
     */
    public void setNamespaceDeclarationStack(NamespaceDeclarationStack nsStack) {
        this.nsStack = nsStack;
    }

    /**
     * Parse the supplied mapping model config path and return the generated EdiMap.
     * <br><br>
     * Can be used to set the mapping model to be used during the parsing operation.
     * See {@link #setMappingModel(EdifactModel)}.
     *
     * @param mappingConfig Config path.  Must conform with the
     *                      <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi-message-mapping-1.0.xsd</a>
     *                      schema.
     * @param baseURI       The base URI against which the config path is to be resolved.  This works on down
     *                      and helps in resolving imported model.
     * @return The Edimap for the mapping model.
     * @throws IOException               Error reading the model stream.
     * @throws SAXException              Invalid model.
     * @throws EDIConfigurationException when edi-mapping-configuration is incorrect.
     */
    public static EdifactModel parseMappingModel(String mappingConfig, URI baseURI) throws IOException, SAXException, EDIConfigurationException {
        String[] mappingConfigTokens = mappingConfig.split("!");
        String ediMappingModel;
        Description mappingDescription = null;

        if (mappingConfigTokens.length == 1) {
            ediMappingModel = mappingConfigTokens[0];
        } else if (mappingConfigTokens.length == 3) {
            ediMappingModel = mappingConfigTokens[0];
            mappingDescription = new Description().setName(mappingConfigTokens[1]).setVersion(mappingConfigTokens[2]);
        } else if (mappingConfigTokens.length == 4) {
            ediMappingModel = mappingConfigTokens[0];
            mappingDescription = new Description().setName(mappingConfigTokens[1]).setVersion(mappingConfigTokens[2]).setNamespace(mappingConfigTokens[3]);
        } else {
            throw new EDIConfigurationException("Invalid mapping model configuration '" + mappingConfig + "'.  Must contain either 1 or 3 tokens.");
        }

        if (isValidURI(ediMappingModel)) {
            URIResourceLocator resourceLocator = new URIResourceLocator();
            URI importBaseURI;

            resourceLocator.setBaseURI(baseURI);
            URI resourceURI = resourceLocator.resolveURI(ediMappingModel);
            importBaseURI = URIResourceLocator.extractBaseURI(resourceURI);

            return parseMappingModel(getMappingConfigData(resourceLocator, ediMappingModel), mappingDescription, resourceURI, importBaseURI);
        } else {
            return parseMappingModel(new StringReader(ediMappingModel), mappingDescription, null, baseURI);
        }
    }

    /**
     * Parse the supplied mapping model config stream and return the generated EdiMap.
     * <br><br>
     * Can be used to set the mapping model to be used during the parsing operation.
     * See {@link #setMappingModel(EdifactModel)}.
     *
     * @param mappingConfigStream Config stream.  Must conform with the
     *                            <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi-message-mapping-1.0.xsd</a>
     *                            schema.
     * @return The Edimap for the mapping model.
     * @throws IOException               Error reading the model stream.
     * @throws SAXException              Invalid model.
     * @throws EDIConfigurationException when edi-mapping-configuration is incorrect.
     */
    public static EdifactModel parseMappingModel(InputStream mappingConfigStream) throws IOException, SAXException, EDIConfigurationException {
        return parseMappingModel(mappingConfigStream, null, null, URIResourceLocator.getSystemBaseURI());
    }

    /**
     * Parse the supplied mapping model config stream and return the generated EdiMap.
     * <br><br>
     * Can be used to set the mapping model to be used during the parsing operation.
     * See {@link #setMappingModel(EdifactModel)}.
     *
     * @param mappingConfigStream Config stream.  Must conform with the
     *                            <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi-message-mapping-1.0.xsd</a>
     *                            schema.
     * @param mappingDescription  Mapping Model Description.
     * @param resourceURI         The resource URI.
     * @param importBaseURI       The base URI for loading imports.
     * @return The Edimap for the mapping model.
     * @throws IOException               Error reading the model stream.
     * @throws SAXException              Invalid model.
     * @throws EDIConfigurationException when edi-mapping-configuration is incorrect.
     */
    public static EdifactModel parseMappingModel(InputStream mappingConfigStream, Description mappingDescription, URI resourceURI, URI importBaseURI) throws IOException, SAXException, EDIConfigurationException {
        AssertArgument.isNotNull(mappingConfigStream, "mappingConfigStream");
        try {
            return parseMappingModel(new InputStreamReader(mappingConfigStream), mappingDescription, resourceURI, importBaseURI);
        } finally {
            mappingConfigStream.close();
        }
    }

    /**
     * Parse the supplied mapping model config stream and return the generated EdiMap.
     * <br><br>
     * Can be used to set the mapping model to be used during the parsing operation.
     * See {@link #setMappingModel(EdifactModel)}.
     *
     * @param mappingConfigStream Config stream.  Must conform with the
     *                            <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi-message-mapping-1.0.xsd</a>
     *                            schema.
     * @return The EdifactModel for the mapping model.
     * @throws IOException               Error reading the model stream.
     * @throws SAXException              Invalid model.
     * @throws EDIConfigurationException when edi-mapping-configuration is incorrect.
     */
    public static EdifactModel parseMappingModel(Reader mappingConfigStream) throws IOException, SAXException, EDIConfigurationException {
        return parseMappingModel(mappingConfigStream, null, null, URIResourceLocator.getSystemBaseURI());
    }

    /**
     * Parse the supplied mapping model config stream and return the generated EdiMap.
     * <br><br>
     * Can be used to set the mapping model to be used during the parsing operation.
     * See {@link #setMappingModel(EdifactModel)}.
     *
     * @param mappingConfigStream Config stream.  Must conform with the
     *                            <a href="http://www.milyn.org/schema/edi-message-mapping-1.0.xsd">edi-message-mapping-1.0.xsd</a>
     *                            schema.
     * @param mappingDescription  Mapping Model Description.
     * @param resourceURI         The resource URI.
     * @param importBaseURI       The base URI for loading imports.
     * @return The EdifactModel for the mapping model.
     * @throws IOException               Error reading the model stream.
     * @throws SAXException              Invalid model.
     * @throws EDIConfigurationException when edi-mapping-configuration is incorrect.
     */
    public static EdifactModel parseMappingModel(Reader mappingConfigStream, Description mappingDescription, URI resourceURI, URI importBaseURI) throws IOException, SAXException, EDIConfigurationException {
        AssertArgument.isNotNull(mappingConfigStream, "mappingConfigStream");
        AssertArgument.isNotNull(importBaseURI, "importBaseURI");
        // The resourceURI can be null e.g. when the mapping model was inlined in the Smooks config.

        EdifactModel edifactModel;

        edifactModel = new EdifactModel(resourceURI, importBaseURI, mappingConfigStream);
        edifactModel.setDescription(mappingDescription);

        return edifactModel;
    }

    /**
     * Get the actual mapping configuration data (the XML).
     *
     * @param resourceLocator Resource locator used to open the config stream.
     * @param mappingConfig   Mapping config path.
     * @return The mapping configuration data stream.
     */
    private static InputStream getMappingConfigData(URIResourceLocator resourceLocator, String mappingConfig) {
        InputStream configStream = null;

        try {
            configStream = resourceLocator.getResource(mappingConfig);
        } catch (IOException e) {
            IllegalStateException state = new IllegalStateException("Invalid EDI mapping model config specified for " + EDIParser.class.getName() + ".  Unable to access URI based mapping model [" + resourceLocator.resolveURI(mappingConfig) + "].");
            state.initCause(e);
            throw state;
        }

        return configStream;
    }

    private static boolean isValidURI(String string) {
        try {
            new URI(string);
        } catch (URISyntaxException e) {
            // It's not a valid URI...
            return false;
        }
        return true;
    }

    /**
     * Set the EDI mapping model to be used in all subsequent parse operations.
     * <br><br>
     * The model can be generated through a call to the {@link EDIParser}.
     *
     * @param mappingModel The mapping model.
     */
    public void setMappingModel(EdifactModel mappingModel) {
        AssertArgument.isNotNull(mappingModel, "mappingModel");
        edifactModel = mappingModel;
    }

    /**
     * Get the indent depth counter
     *
     * @return Indent depth counter.
     */
    public Integer getIndentDepth() {
        return indentDepth;
    }

    /**
     * Set the indent depth counter
     *
     * @param indentDepth Indent depth counter.
     */
    public void setIndentDepth(Integer indentDepth) {
        this.indentDepth = indentDepth;
    }

    /**
     * Parse an EDI InputSource.
     */
    public void parse(InputSource ediInputSource) throws IOException, SAXException {
        if (contentHandler == null) {
            throw new IllegalStateException("'contentHandler' not set.  Cannot parse EDI stream.");
        }

        if (edifactModel == null || edifactModel.getEdimap() == null) {
            throw new IllegalStateException("'mappingModel' not set.  Cannot parse EDI stream.");
        }

        try {
            // Create a reader for reading the EDI segments...
            segmentReader = new BufferedSegmentReader(ediInputSource, edifactModel.getDelimiters());
            segmentReader.setIgnoreNewLines(getFeature(FEATURE_IGNORE_NEWLINES));

            // Initialize the indent counter...
            indentDepth = 0;

            // Fire the startDocument event, as well as the startElement event...
            contentHandler.startDocument();
            parse(false);
            contentHandler.endDocument();
        } finally {
            contentHandler = null;
        }
    }

    /**
     * Parse an EDI message, using a supplied segment reader.
     */
    public void parse() throws IOException, SAXException {
        if (contentHandler == null) {
            throw new IllegalStateException("'contentHandler' not set.  Cannot parse EDI stream.");
        }

        if (segmentReader == null) {
            throw new IllegalStateException("'bufferedSegmentReader' not set.  Cannot parse EDI stream.");
        }

        if (edifactModel == null || edifactModel.getEdimap() == null) {
            throw new IllegalStateException("'mappingModel' not set.  Cannot parse EDI stream.");
        }

        try {
            parse(true);
        } finally {
            contentHandler = null;
        }
    }

    public EDIParser setBufferedSegmentReader(BufferedSegmentReader segmentReader) {
        this.segmentReader = segmentReader;
        return this;
    }

    private void parse(boolean indent) throws SAXException, IOException, EDIParseException {
        boolean ignoreUnmappedSegment = edifactModel.getEdimap().isIgnoreUnmappedSegments();

        startElement(edifactModel.getEdimap().getSegments(), indent);

        // Work through all the segments in the model.  Move to the first segment before starting...
        if (segmentReader.moveToNextSegment()) {
            mapSegments(edifactModel.getEdimap().getSegments().getSegments());

            // If we reach the end of the mapping model and we still have more EDI segments in the message....
            while (segmentReader.hasCurrentSegment()) {
                if (!EMPTY_LINE.matcher(segmentReader.getSegmentBuffer().toString()).matches()
                        && !ignoreUnmappedSegment) {
                    throw new EDIParseException(edifactModel.getEdimap(), "Reached end of mapping model but there are more EDI segments in the incoming message.  Read " + segmentReader.getCurrentSegmentNumber() + " segment(s). Current EDI segment is [" + segmentReader.getSegmentBuffer() + "]");
                }
                segmentReader.moveToNextSegment();
            }
        }

        // Fire the endDocument event, as well as the endElement event...
        endElement(edifactModel.getEdimap().getSegments(), true);
    }

    /**
     * Map a list of EDI Segments to SAX events.
     * <br><br>
     * Reads the segments from the input stream and maps them based on the supplied list of expected segments.
     *
     * @param expectedSegments The list of expected segments.
     * @throws IOException  Error reading an EDI segment from the input stream.
     * @throws SAXException EDI processing exception.
     */
    private void mapSegments(List<SegmentGroup> expectedSegments) throws IOException, SAXException {
        mapSegments(expectedSegments, null);
    }

    /**
     * Map a list of EDI Segments to SAX events.
     * <br><br>
     * Reads the segments from the input stream and maps them based on the supplied list of expected segments.
     *
     * @param expectedSegments       The list of expected segments.
     * @param preLoadedSegmentFields Preloaded segment.  This can happen in the case of a segmentGroup.
     * @throws IOException  Error reading an EDI segment from the input stream.
     * @throws SAXException EDI processing exception.
     */
    private void mapSegments(List<SegmentGroup> expectedSegments, String[] preLoadedSegmentFields) throws IOException, SAXException {
        int segmentMappingIndex = 0; // The current index within the supplied segment list.
        int segmentProcessingCount = 0; // The number of times the current segment definition from the supplied segment list has been applied to message segments on the incomming EDI message.
        String[] currentSegmentFields = preLoadedSegmentFields;
        boolean ignoreUnmappedSegment = edifactModel.getEdimap().isIgnoreUnmappedSegments(); // Used to relax parsing compared to the mapping model

        if (expectedSegments.size() == 0) {
            return;
        }

        while (segmentMappingIndex < expectedSegments.size() && segmentReader.hasCurrentSegment()) {
            SegmentGroup expectedSegmentGroup = expectedSegments.get(segmentMappingIndex);
            int minOccurs = expectedSegmentGroup.getMinOccurs();
            int maxOccurs = expectedSegmentGroup.getMaxOccurs();

            // A negative max value indicates an unbound max....
            if (maxOccurs < 0) {
                maxOccurs = Integer.MAX_VALUE;
            }
            // Make sure min is not greater than max...
            if (minOccurs > maxOccurs) {
                maxOccurs = minOccurs;
            }

            // Only load the next segment if currentSegmentFields == null i.e. we don't have a set of
            // preLoadedSegmentFields (see method args) that need to be processed first...
            if (currentSegmentFields == null) {
                currentSegmentFields = segmentReader.getCurrentSegmentFields();
            }

            // If the current segment being read from the incoming message doesn't match the expected
            // segment code....
            if (!currentSegmentFields[0].equals(expectedSegmentGroup.getSegcode())) {
                Matcher matcher = expectedSegmentGroup.getSegcodePattern().matcher(segmentReader.getSegmentBuffer());
                if (!matcher.matches()) {
                    if (segmentProcessingCount < minOccurs) {
                        // check if strict segment matching is inforced
                        if (!ignoreUnmappedSegment) {
                            // If we haven't read the minimum number of instances of the current "expected" segment, raise an error...
                            throw new EDIParseException(edifactModel.getEdimap(), "Must be a minimum of " + minOccurs + " instances of segment [" + expectedSegmentGroup.getSegcode() + "].  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedSegmentGroup, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
                        } else {
                            // skip unmapped current segment
                            segmentReader.moveToNextSegment();
                            currentSegmentFields = null;
                            // check that there still are messages in the EDI message stream for the required segments in the model
                            if (!segmentReader.hasCurrentSegment()) {
                                throw new EDIParseException(edifactModel.getEdimap(), "Reached end of EDI message stream but there must be a minimum of " + minOccurs + " instances of segment [" + expectedSegmentGroup.getSegcode() + "].  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedSegmentGroup, segmentReader.getCurrentSegmentNumber(), null);
                            }
                            continue;
                        }
                    } else {
                        // Otherwise, move to the next "expected" segment and start the loop again...
                        segmentMappingIndex++;
                        segmentProcessingCount = 0;
                        continue;
                    }
                }
            }

            if (segmentProcessingCount >= maxOccurs) {
                // Move to the next "expected" segment and start the loop again...
                segmentMappingIndex++;
                segmentProcessingCount = 0;
                continue;
            }

            // The current read message segment appears to match that expected according to the mapping model.
            // Proceed to process the segment fields and the segments sub-segments...

            if (expectedSegmentGroup instanceof Segment) {
                mapSegment(currentSegmentFields, (Segment) expectedSegmentGroup);
            } else {
                startElement(expectedSegmentGroup, true);
                mapSegments(expectedSegmentGroup.getSegments(), currentSegmentFields);
                endElement(expectedSegmentGroup, true);
            }

            // Increment the count on the number of times the current "expected" mapping config has been applied...
            segmentProcessingCount++;
            currentSegmentFields = null;

            if (segmentProcessingCount < minOccurs && !segmentReader.hasCurrentSegment()) {
                throw new EDIParseException(edifactModel.getEdimap(), "Reached end of EDI message stream but there must be a minimum of " + minOccurs + " instances of segment [" + expectedSegmentGroup.getSegcode() + "].  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedSegmentGroup, segmentReader.getCurrentSegmentNumber(), null);
            }
        }
    }

    /**
     * Map a single segment based on the current set of segment fields read from input and the segment mapping
     * config that these fields should map to.
     *
     * @param currentSegmentFields Current set of segment fields read from input.
     * @param expectedSegment      The segment mapping config that the currentSegmentFields should map to.
     * @throws IOException  Error reading an EDI segment from the input stream.  This will happen as the segment
     *                      reader tries to move to the next segment after performing this mapping.
     * @throws SAXException EDI processing exception.
     */
    private void mapSegment(String[] currentSegmentFields, Segment expectedSegment) throws IOException, SAXException {
        startElement(expectedSegment, true);

        mapFields(currentSegmentFields, expectedSegment);
        if (segmentReader.moveToNextSegment()) {
            mapSegments(expectedSegment.getSegments());
        }

        endElement(expectedSegment, true);
    }

    /**
     * Map the individual field values based on the supplied expected field configs.
     *
     * @param currentSegmentFields Segment fields from the input message.
     * @param segment              List of expected field mapping configurations that the currentSegmentFields
     *                             are expected to map to.
     * @throws SAXException EDI processing exception.
     */
    public void mapFields(String[] currentSegmentFields, Segment segment) throws SAXException {
        String segmentCode = segment.getSegcode();

        List<Field> expectedFields = segment.getFields();

        // Make sure all required fields are present in the incoming message...
        assertFieldsOK(currentSegmentFields, segment);

        // Iterate over the fields and map them...
        int numFields = currentSegmentFields.length - 1; // It's "currentSegmentFields.length - 1" because we don't want to include the segment code.
        int numFieldsMapped = segment.getFields().size();
        boolean ignoreUnmappedFields = segment.isIgnoreUnmappedFields();
        Delimiters delimiters = segmentReader.getDelimiters();
        String fieldRepeat = delimiters.getFieldRepeat();
        for (int i = 0; i < numFields; i++) {
            if (ignoreUnmappedFields && i >= numFieldsMapped) {
                break;
            }
            String fieldMessageVal = currentSegmentFields[i + 1]; // +1 to skip the segment code
            Field expectedField = expectedFields.get(i);

            if (fieldRepeat != null) {
                String[] repeatedFields = EDIUtils.split(fieldMessageVal, fieldRepeat, delimiters.getEscape());
                for (String repeatedField : repeatedFields) {
                    mapField(repeatedField, expectedField, i, segmentCode);
                }
            } else {
                mapField(fieldMessageVal, expectedField, i, segmentCode);
            }
        }
    }

    /**
     * Map an individual segment field.
     *
     * @param fieldMessageVal The field message value.
     * @param expectedField   The mapping config to which the field value is expected to map.
     * @param fieldIndex      The field index within its segment (base 0).
     * @param segmentCode     The segment code within which the field exists.
     * @throws SAXException EDI processing exception.
     */
    private void mapField(String fieldMessageVal, Field expectedField, int fieldIndex, String segmentCode) throws SAXException {
        List<Component> expectedComponents = expectedField.getComponents();

        // If there are components defined on this field...
        if (expectedComponents.size() != 0) {
            Delimiters delimiters = segmentReader.getDelimiters();
            String[] currentFieldComponents = EDIUtils.split(fieldMessageVal, delimiters.getComponent(), delimiters.getEscape());

            assertComponentsOK(expectedField, fieldIndex, segmentCode, expectedComponents, currentFieldComponents);

            if (currentFieldComponents.length > 0 || !ignoreEmptyNodes()) {
                startElement(expectedField, true);
                // Iterate over the field components and map them...
                for (int i = 0; i < currentFieldComponents.length; i++) {
                    String componentMessageVal = currentFieldComponents[i];
                    Component expectedComponent = expectedComponents.get(i);

                    mapComponent(componentMessageVal, expectedComponent, fieldIndex, i, segmentCode, expectedField.getXmltag());
                }
                endElement(expectedField, true);
            }
        } else {
            if (expectedField.isRequired() && fieldMessageVal.length() == 0) {
                throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segmentCode + "], field " + (fieldIndex + 1) + " (" + expectedField.getXmltag() + ") expected to contain a value.  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedField, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }

            if (fieldMessageVal.length() > 0 || !ignoreEmptyNodes()) {
                startElement(expectedField, true);
                writeToContentHandler(fieldMessageVal);
                endElement(expectedField, false);
            }
        }
    }

    /**
     * Map an individual component.
     *
     * @param componentMessageVal Component message value read from EDI input.
     * @param expectedComponent   The mapping config to which the component value is expected to map.
     * @param fieldIndex          The field index within its segment (base 0) in which the component exists.
     * @param componentIndex      The component index within its field (base 0).
     * @param segmentCode         The segment code within which the component exists.
     * @param field               Field within which the component exists.
     * @throws SAXException EDI processing exception.
     */
    private void mapComponent(String componentMessageVal, Component expectedComponent, int fieldIndex, int componentIndex, String segmentCode, String field) throws SAXException {
        List<SubComponent> expectedSubComponents = expectedComponent.getSubComponents();

        if (expectedSubComponents.size() != 0) {
            Delimiters delimiters = segmentReader.getDelimiters();
            String[] currentComponentSubComponents = EDIUtils.split(componentMessageVal, delimiters.getSubComponent(), delimiters.getEscape());

            assertSubComponentsOK(expectedComponent, fieldIndex, componentIndex, segmentCode, field, expectedSubComponents, currentComponentSubComponents);

            if (currentComponentSubComponents.length > 0 || !ignoreEmptyNodes()) {
                startElement(expectedComponent, true);
                for (int i = 0; i < currentComponentSubComponents.length; i++) {
                    if (expectedSubComponents.get(i).isRequired() && currentComponentSubComponents[i].length() == 0) {
                        throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segmentCode + "], field " + (fieldIndex + 1) + " (" + field + "), component " + (componentIndex + 1) + " (" + expectedComponent.getXmltag() + "), sub-component " + (i + 1) + " (" + expectedSubComponents.get(i).getXmltag() + ") expected to contain a value.  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedSubComponents.get(i), segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
                    }

                    startElement(expectedSubComponents.get(i), true);
                    writeToContentHandler(currentComponentSubComponents[i]);
                    endElement(expectedSubComponents.get(i), false);
                }
                endElement(expectedComponent, true);
            }
        } else {
            if (expectedComponent.isRequired() && componentMessageVal.length() == 0) {
                throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segmentCode + "], field " + (fieldIndex + 1) + " (" + field + "), component " + (componentIndex + 1) + " (" + expectedComponent.getXmltag() + ") expected to contain a value.  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedComponent, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }

            if (componentMessageVal.length() > 0 || !ignoreEmptyNodes()) {
                startElement(expectedComponent, true);
                writeToContentHandler(componentMessageVal);
                endElement(expectedComponent, false);
            }
        }
    }

    private void assertFieldsOK(String[] currentSegmentFields, Segment segment) throws EDIParseException {

        List<Field> expectedFields = segment.getFields();

        int numFieldsExpected = expectedFields.size() + 1; // It's "expectedFields.length + 1" because the segment code is included.
        int numberOfFieldsToValidate = 0;

        if (currentSegmentFields.length < numFieldsExpected) {
            boolean throwException = false;

            // If we don't have all the fields we're expecting, check is the Segment truncatable
            // and are the missing fields required or not...
            if (segment.isTruncatable()) {
                int numFieldsMissing = numFieldsExpected - currentSegmentFields.length;
                for (int i = expectedFields.size() - 1; i > (expectedFields.size() - numFieldsMissing - 1); i--) {
                    if (expectedFields.get(i).isRequired()) {
                        throwException = true;
                        break;
                    }
                }
            } else {
                throwException = true;
            }

            if (throwException) {
                throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segment.getSegcode() + "] expected to contain " + (numFieldsExpected - 1) + " fields.  Actually contains " + (currentSegmentFields.length - 1) + " fields (not including segment code).  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", segment, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }

            numberOfFieldsToValidate = currentSegmentFields.length;

        } else if (currentSegmentFields.length > numFieldsExpected) {
            // we have more fields than we are expecting.
            if (segment.isIgnoreUnmappedFields()) {
                numberOfFieldsToValidate = numFieldsExpected;
            } else {
                throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segment.getSegcode() + "] expected to contain " + (numFieldsExpected - 1) + " fields.  Actually contains " + (currentSegmentFields.length - 1) + " fields (not including segment code).  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", segment, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }
        } else {
            // number of fields matches the expected number of fields.
            numberOfFieldsToValidate = currentSegmentFields.length;
        }

        for (int i = 1; i < numberOfFieldsToValidate; i++) {
            Field field = expectedFields.get(i - 1);
            if (field.getComponents().size() == 0 && (!currentSegmentFields[i].equals(""))) {
                validateValueNode(field, currentSegmentFields[i]);
            }
        }
    }

    private void assertComponentsOK(Field expectedField, int fieldIndex, String segmentCode, List<Component> expectedComponents, String[] currentFieldComponents) throws EDIParseException {
        if (currentFieldComponents.length != expectedComponents.size()) {
            boolean throwException = false;

            if (expectedField.isTruncatable()) {

                //When there are no Components in Field it should not throw exception, since
                //the Field is just created (with Field-separator) for satisfying requirement for Fields
                //that are required later in Segment.
                if (currentFieldComponents.length == 0) {
                    return;
                }

                int numComponentsMissing = expectedComponents.size() - currentFieldComponents.length;
                for (int i = expectedComponents.size() - 1; i > (expectedComponents.size() - numComponentsMissing - 1); i--) {
                    if (expectedComponents.get(i).isRequired()) {
                        throwException = true;
                        break;
                    }
                }
            } else {
                throwException = true;
            }

            if (throwException) {
                throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segmentCode + "], field " + (fieldIndex + 1) + " (" + expectedField.getXmltag() + ") expected to contain " + expectedComponents.size() + " components.  Actually contains " + currentFieldComponents.length + " components.  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedField, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }
        }

        for (int i = 0; i < currentFieldComponents.length; i++) {
            Component component = expectedComponents.get(i);
            if (component.getSubComponents().size() == 0 && (!currentFieldComponents[i].equals(""))) {
                validateValueNode(component, currentFieldComponents[i]);
            }
        }
    }

    private void assertSubComponentsOK(Component expectedComponent, int fieldIndex, int componentIndex, String segmentCode, String field, List<SubComponent> expectedSubComponents, String[] currentComponentSubComponents) throws EDIParseException {
        if (currentComponentSubComponents.length != expectedSubComponents.size()) {
            boolean throwException = false;

            if (expectedComponent.isTruncatable()) {

                //When there are no SubComponents in field it should not throw exception, since
                //the Component is just created (with Component-separator) for satisfying requirement
                //for Components that are required later in Field.
                if (currentComponentSubComponents.length == 0) {
                    return;
                }

                int numSubComponentsMissing = expectedSubComponents.size() - currentComponentSubComponents.length;
                for (int i = expectedSubComponents.size() - 1; i > (expectedSubComponents.size() - numSubComponentsMissing - 1); i--) {
                    if (expectedSubComponents.get(i).isRequired()) {
                        throwException = true;
                        break;
                    }
                }
            } else {
                throwException = true;
            }

            if (throwException) {
                throw new EDIParseException(edifactModel.getEdimap(), "Segment [" + segmentCode + "], field " + (fieldIndex + 1) + " (" + field + "), component " + (componentIndex + 1) + " (" + expectedComponent.getXmltag() + ") expected to contain " + expectedSubComponents.size() + " sub-components.  Actually contains " + currentComponentSubComponents.length + " sub-components.  Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", expectedComponent, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }
        }

        for (int i = 0; i < currentComponentSubComponents.length; i++) {
            SubComponent subComponent = expectedSubComponents.get(i);
            if (!currentComponentSubComponents[i].equals("")) {
                validateValueNode(subComponent, currentComponentSubComponents[i]);
            }
        }
    }

    private void validateValueNode(ValueNode valueNode, String value) throws EDIParseException {

        // Return when validation is turned off.
        if (!getFeature(FEATURE_VALIDATE)) {
            return;
        }

        //Test minLength.
        if (valueNode.getMinLength() != null) {
            if (value.length() < valueNode.getMinLength()) {
                throw new EDIParseException(edifactModel.getEdimap(), "Value [" + value + "] should have a length greater than [" + valueNode.getMinLength() + "]. Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", valueNode, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }
        }

        //Test maxLength.
        if (valueNode.getMaxLength() != null) {
            if (value.length() > valueNode.getMaxLength()) {
                throw new EDIParseException(edifactModel.getEdimap(), "Value [" + value + "] exceeds allowed maximum length of [" + valueNode.getMaxLength() + "]. Currently at segment number " + segmentReader.getCurrentSegmentNumber() + ".", valueNode, segmentReader.getCurrentSegmentNumber(), segmentReader.getCurrentSegmentFields());
            }
        }
    }

    public void startElement(MappingNode node, boolean indent) throws SAXException {
        if (node.getXmltag() != null) {
            startElement(node.getXmltag(), node.getNamespace(), indent);
        }
    }

    public void startElement(String elementName, String namespace, boolean indent) throws SAXException {
        startElement(elementName, namespace, indent, EMPTY_ATTRIBS);
    }

    public void startElement(String elementName, String namespace, boolean indent, Attributes attributes) throws SAXException {
        if (indent) {
            indent();
        }
        AssertArgument.isNotNull(namespace, "Empty namespace detected for elemnet " + elementName);

        String nsPrefix = getNamespacePrefix(namespace);
        if (nsPrefix != null) {
            contentHandler.startElement(namespace, elementName, nsPrefix + ":" + elementName, attributes);
        } else {
            contentHandler.startElement(namespace, elementName, elementName, attributes);
        }

        indentDepth++;
    }

    public void endElement(MappingNode node, boolean indent) throws SAXException {
        if (node.getXmltag() != null) {
            endElement(node.getXmltag(), node.getNamespace(), indent);
        }
    }

    public void endElement(String elementName, String namespace, boolean indent) throws SAXException {
        indentDepth--;
        if (indent) {
            indent();
        }

        String nsPrefix = getNamespacePrefix(namespace);
        if (nsPrefix != null) {
            contentHandler.endElement(namespace, elementName, nsPrefix + ":" + elementName);
        } else {
            contentHandler.endElement(namespace, elementName, elementName);
        }
    }

    /**
     * This method returns a namespace prefix associated with
     * given namespace.
     *
     * @param namespace The namespace.
     * @return The namespace prefix.
     */
    private String getNamespacePrefix(String namespace) {
        if (nsStack == null) {
            return null;
        }
        if (namespace == null || XMLConstants.NULL_NS_URI.equals(namespace)) {
            return null;
        }

        return nsStack.getPrefix(namespace);
    }

    // HACK :-) it's hardly going to be deeper than this!!
    private static final char[] indentChars = (new String("\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t").toCharArray());


    private void indent() throws SAXException {
        if (indentDepth == null) {
            throw new IllegalStateException("'indentDepth' property not set on parser instance.  Cannot indent.");
        }
        contentHandler.characters(indentChars, 0, indentDepth + 1);
    }

    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    private void writeToContentHandler(String messageVal) throws SAXException {
        if (edifactModel.getDelimiters() != null && edifactModel.getDelimiters().getEscape() != null) {
            String escapeDelimiter = edifactModel.getDelimiters().getEscape();
            messageVal = messageVal.replace(escapeDelimiter + escapeDelimiter, escapeDelimiter);
        }
        contentHandler.characters(messageVal.toCharArray(), 0, messageVal.length());
    }

    public Map<String, Boolean> getFeatures() {
        if (features == null) {
            initializeFeatures();
        }
        return features;
    }

    private void initializeFeatures() {
        features = new HashMap<String, Boolean>();
        features.put(FEATURE_VALIDATE, false);
        features.put(FEATURE_IGNORE_NEWLINES, false);
        features.put(FEATURE_IGNORE_EMPTY_NODES, true);
    }

    private boolean ignoreEmptyNodes() {
        if (ignoreEmptyNodes == null) {
            ignoreEmptyNodes = getFeature(FEATURE_IGNORE_EMPTY_NODES);
        }

        return ignoreEmptyNodes;
    }

    /****************************************************************************
     *
     * The following methods are currently unimplemnted...
     *
     ****************************************************************************/

    public void parse(String systemId) throws IOException, SAXException {
        throw new UnsupportedOperationException("Operation not supports by this reader.");
    }

    public boolean getFeature(String name) {
        return getFeatures().get(name);
    }

    public void setFeature(String name, boolean value) {
        getFeatures().put(name, value);
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public void setDTDHandler(DTDHandler arg0) {
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public void setEntityResolver(EntityResolver arg0) {
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setErrorHandler(ErrorHandler arg0) {
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
    }
}
