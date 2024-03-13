/*-
 * ========================LICENSE_START=================================
 * smooks-ect
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
package org.smooks.edi.ect;

import org.eclipse.emf.ecore.EPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.smooks.assertion.AssertArgument;
import org.smooks.edi.ect.ecore.ECoreGenerator;
import org.smooks.edi.ect.ecore.SchemaConverter;
import org.smooks.edi.ect.formats.unedifact.parser.UnEdifactDirectoryParser;
import org.smooks.edi.ect.formats.unedifact.UnEdifactDefinitionReader;
import org.smooks.edi.edisax.archive.Archive;
import org.smooks.edi.edisax.interchange.EdiDirectory;
import org.smooks.edi.edisax.model.internal.*;
import org.smooks.edi.edisax.util.EDIUtils;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * EDI Convertion Tool.
 * <br><br>
 * Takes the set of messages from an {@link DirectoryParser} and generates
 * a Smooks EDI Mapping Model archive that can be written to a zip file or folder.
 *
 * @author bardl
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class EdiConvertionTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdiConvertionTool.class);

    /**
     * Write an EDI Mapping Model configuration set from a UN/EDIFACT
     * specification.
     *
     * @param specification     The UN/EDIFACT specification zip file.
     * @param modelSetOutStream The output zip stream for the generated EDI Mapping Model configuration set.
     * @param urn               The URN for the EDI Mapping model configuration set.
     * @throws IOException Error writing Mapping Model configuration set.
     */
    public static void fromUnEdifactSpec(ZipInputStream specification, ZipOutputStream modelSetOutStream, String urn, boolean useShortName) throws IOException {
        try {
            fromSpec(new UnEdifactDirectoryParser(specification, true, useShortName), modelSetOutStream, urn);
        } finally {
            specification.close();
        }
    }

    /**
     * Write an EDI Mapping Model configuration set from a UN/EDIFACT
     * specification.
     *
     * @param specification The UN/EDIFACT specification zip file.
     * @param urn           The URN for the EDI Mapping model configuration set.
     * @return Smooks EDI Mapping model Archive.
     * @throws IOException Error writing Mapping Model configuration set.
     */
    public static Archive fromUnEdifactSpec(File specification, String urn) throws IOException {
        return fromUnEdifactSpec(specification, urn, (String) null);
    }

    /**
     * Write an EDI Mapping Model configuration set from a UN/EDIFACT
     * specification.
     *
     * @param specification The UN/EDIFACT specification zip file.
     * @param urn           The URN for the EDI Mapping model configuration set.
     * @param messages      The messages to be included in the generated Archive.
     * @return Smooks EDI Mapping model Archive.
     * @throws IOException Error writing Mapping Model configuration set.
     */
    public static Archive fromUnEdifactSpec(File specification, String urn, String... messages) throws IOException {
        ZipInputStream definitionZipStream;

        try {
            definitionZipStream = new ZipInputStream(new FileInputStream(specification));
        } catch (FileNotFoundException e) {
            throw new EdiParseException("Error opening zip file containing the Un/Edifact specification '" + specification.getAbsoluteFile() + "'.", e);
        }

        return createArchive(new UnEdifactDirectoryParser(definitionZipStream, true, true), urn, messages);
    }

    /**
     * Write an EDI Mapping Model configuration set from the specified EDI Specification Reader.
     *
     * @param directoryParser   The configuration reader for the EDI interchange configuration set.
     * @param modelSetOutStream The EDI Mapping Model output Stream.
     * @param urn               The URN for the EDI Mapping model configuration set.
     * @throws IOException Error writing Mapping Model configuration set.
     */
    public static void fromSpec(DirectoryParser directoryParser, ZipOutputStream modelSetOutStream, String urn) throws IOException {
        AssertArgument.isNotNull(directoryParser, "directoryParser");
        AssertArgument.isNotNull(modelSetOutStream, "modelSetOutStream");

        try {
            Archive archive = createArchive(directoryParser, urn);

            // Now output the generated archive...
            archive.toOutputStream(modelSetOutStream);
        } catch (Throwable t) {
            LOGGER.error("Error while generating EDI Mapping Model archive for '" + urn + "'.", t);
        } finally {
            modelSetOutStream.close();
        }
    }

    /**
     * Write an EDI Mapping Model configuration set from a UN/EDIFACT
     * specification.
     *
     * @param specification     The UN/EDIFACT specification zip file.
     * @param modelSetOutFolder The output folder for the generated EDI Mapping Model configuration set.
     * @param urn               The URN for the EDI Mapping model configuration set.
     * @throws IOException Error writing Mapping Model configuration set.
     */
    public static void fromUnEdifactSpec(ZipInputStream specification, File modelSetOutFolder, String urn, boolean useShortName) throws IOException {
        try {
            fromSpec(new UnEdifactDirectoryParser(specification, true, useShortName), modelSetOutFolder, urn);
        } finally {
            specification.close();
        }
    }

    /**
     * Write an EDI Mapping Model configuration set from the specified EDI Specification Reader.
     *
     * @param directoryParser   The configuration reader for the EDI interchange configuration set.
     * @param modelSetOutFolder The output folder for the generated EDI Mapping Model configuration set.
     * @param urn               The URN for the EDI Mapping model configuration set.
     * @throws IOException Error writing Mapping Model configuration set.
     */
    public static void fromSpec(DirectoryParser directoryParser, File modelSetOutFolder, String urn) throws IOException {
        AssertArgument.isNotNull(directoryParser, "ediSpecificationReader");
        AssertArgument.isNotNull(modelSetOutFolder, "modelSetOutFolder");

        Archive archive = createArchive(directoryParser, urn);

        // Now output the generated archive...
        archive.toFileSystem(modelSetOutFolder);
    }

    private static Archive createArchive(DirectoryParser directoryParser, String urn, String... messages) throws IOException {
        Archive archive = new Archive();
        StringBuilder modelListBuilder = new StringBuilder();
        StringWriter messageEntryWriter = new StringWriter();
        String pathPrefix = urn.replace(".", "_").replace(":", "/");

        Edimap edimap = UnEdifactDefinitionReader.parse(directoryParser);
        EdiDirectory ediDirectory = directoryParser.getEdiDirectory(edimap, messages);

        // Add the common model...
        addModel(ediDirectory.getCommonModel(), pathPrefix, modelListBuilder, messageEntryWriter, archive);

        // Add each of the messages...
        for (Edimap messageModel : ediDirectory.getMessageModels()) {
            addModel(messageModel, pathPrefix, modelListBuilder, messageEntryWriter, archive);
        }

        // Now create XML Schemas
        Set<EPackage> packages = new ECoreGenerator().generatePackages(ediDirectory);
        String pluginID = "org.smooks.edi.unedifact.unknown";
        if (urn.lastIndexOf(':') > 0) {
            pluginID = urn.substring(0, urn.lastIndexOf(':')).replace(':', '.').toLowerCase();
        }
        Archive schemas = SchemaConverter.INSTANCE.createArchive(packages, pluginID, pathPrefix);
        archive.merge(schemas);

        // Add the generated mapping model to the archive...
        archive.addEntry(EDIUtils.EDI_MAPPING_MODEL_ZIP_LIST_FILE, modelListBuilder.toString());

        // Add the model set URN to the archive...
        archive.addEntry(EDIUtils.EDI_MAPPING_MODEL_URN, urn);

        // Add an entry for the interchange properties...
        Properties interchangeProperties = directoryParser.getInterchangeProperties();
        ByteArrayOutputStream propertiesOutStream = new ByteArrayOutputStream();
        try {
            interchangeProperties.store(propertiesOutStream, "UN/EDIFACT Interchange Properties");
            propertiesOutStream.flush();
            archive.addEntry(EDIUtils.EDI_MAPPING_MODEL_INTERCHANGE_PROPERTIES_FILE, propertiesOutStream.toByteArray());
        } finally {
            propertiesOutStream.close();
        }

        return archive;
    }

    private static void addModel(Edimap model, String pathPrefix, StringBuilder modelListBuilder, StringWriter messageEntryWriter, Archive archive) throws IOException {
        Description modelDesc = model.getDescription();
        String messageEntryPath = pathPrefix + "/" + modelDesc.getName() + ".xml";

        // Generate the mapping model for this message...
        messageEntryWriter.getBuffer().setLength(0);
        model.write(messageEntryWriter);

        // Add the generated mapping model to the archive...
        archive.addEntry(messageEntryPath, messageEntryWriter.toString());

        // Add this messages archive entry to the mapping model list file...
        modelListBuilder.append("/" + messageEntryPath);
        modelListBuilder.append("!" + modelDesc.getName());
        modelListBuilder.append("!" + modelDesc.getVersion());
        modelListBuilder.append("!" + modelDesc.getNamespace());
        modelListBuilder.append("\n");
    }

    public static void removeDuplicateSegments(SegmentGroup segmentGroup) {
        if (segmentGroup instanceof Segment) {
            removeDuplicateFields(((Segment) segmentGroup).getFields());
        }

        List<SegmentGroup> segments = segmentGroup.getSegments();
        if (segments != null) {
            removeDuplicateMappingNodes(segments);
            for (SegmentGroup childSegmentGroup : segments) {
                removeDuplicateSegments(childSegmentGroup);
            }
        }
    }

    private static void removeDuplicateFields(List<Field> fields) {
        if (fields != null && !fields.isEmpty()) {
            // Remove the duplicates from the fields themselves...
            removeDuplicateMappingNodes(fields);

            // Drill down into the field components...
            for (Field field : fields) {
                removeDuplicateComponents(field.getComponents());
            }
        }
    }

    private static void removeDuplicateComponents(List<Component> components) {
        if (components != null && !components.isEmpty()) {
            // Remove the duplicates from the components themselves...
            removeDuplicateMappingNodes(components);

            // Remove duplicate sub components from each component...
            for (Component component : components) {
                removeDuplicateMappingNodes(component.getSubComponents());
            }
        }
    }

    private static void removeDuplicateMappingNodes(List mappingNodes) {
        if (mappingNodes == null || mappingNodes.isEmpty()) {
            return;
        }

        Set<String> nodeNames = getMappingNodeNames(mappingNodes);

        if (nodeNames.size() < mappingNodes.size()) {
            // There may be duplicates... find them and number them...
            for (String nodeName : nodeNames) {
                int nodeCount = getMappingNodeCount(mappingNodes, nodeName);
                if (nodeCount > 1) {
                    removeDuplicateMappingNodes(mappingNodes, nodeName);
                }
            }
        }
    }

    private static void removeDuplicateMappingNodes(List mappingNodes, String nodeName) {
        int tagIndex = 1;

        for (Object mappingNodeObj : mappingNodes) {
            MappingNode mappingNode = (MappingNode) mappingNodeObj;
            String xmlTag = mappingNode.getXmltag();

            if (xmlTag != null && xmlTag.equals(nodeName)) {
                mappingNode.setXmltag(xmlTag + MappingNode.INDEXED_NODE_SEPARATOR + tagIndex);
                tagIndex++;
            }
        }
    }

    private static Set<String> getMappingNodeNames(List mappingNodes) {
        Set<String> nodeNames = new LinkedHashSet<String>();

        for (Object mappingNode : mappingNodes) {
            String xmlTag = ((MappingNode) mappingNode).getXmltag();
            if (xmlTag != null) {
                nodeNames.add(xmlTag);
            }
        }

        return nodeNames;
    }

    private static int getMappingNodeCount(List mappingNodes, String nodeName) {
        int nodeCount = 0;

        for (Object mappingNode : mappingNodes) {
            String xmlTag = ((MappingNode) mappingNode).getXmltag();
            if (xmlTag != null && xmlTag.equals(nodeName)) {
                nodeCount++;
            }
        }

        return nodeCount;
    }
}
