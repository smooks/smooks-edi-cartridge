/*-
 * ========================LICENSE_START=================================
 * smooks-ect
 * %%
 * Copyright (C) 2020 - 2021 Smooks
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
package org.smooks.edi.ect.formats.unedifact.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.EdiConvertionTool;
import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.ect.common.XmlTagEncoder;
import org.smooks.edi.edisax.interchange.EdiDirectory;
import org.smooks.edi.edisax.model.internal.*;
import org.smooks.edi.edisax.util.EDIUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnEdifactDirectoryParser implements DirectoryParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryParser.class);
    private static final int BUFFER = 2048;

    protected final Map<String, byte[]> definitionFiles;
    protected final Map<String, byte[]> messageFiles;
    protected InputStreamReader dataElementsDirectoryReader;
    protected InputStreamReader compositeDataElementsDirectoryReader;
    protected InputStreamReader standardSegmentsDirectoryReader;
    protected InputStreamReader consolidatedCodeListReader;

    private final boolean useShortName;
    private final boolean useImport;
    private final Set<String> versions = new HashSet<>();
    private final Set<String> messages = new HashSet<>();
    private EdiDirectory ediDirectory;

    /**
     * Matcher to recognize and parse entries like CUSCAR_D.08A
     */
    private static final Pattern ENTRY_FILE_NAME = Pattern.compile("^([A-Z]+)_([A-Z])\\.([0-9]+[A-Z])$");

    /**
     * Prefix used in elementnames when creating short names.
     */
    private static final String DATA_ELEMENT_PREFIX = "e";

    /**
     * Matches the line of '-' characters separating the data-, composite- or segment-definitions.
     */
    protected static final String ELEMENT_SEPARATOR = "^[-|�]+$";

    /**
     * Matches the string "..".
     */
    private static final String DOTS = "\\.\\.";

    /**
     * Extracts information from data element occuring on one single row in Composite definition.
     * Example - "010    3042  Street and number or post office box"
     * Group1 = line number
     * Group2 = id
     * Group3 = name
     * Group4 = mandatory
     * Group5 = type
     * Group6 = min occurance
     * Group7 = max occurance
     */
    private static final Pattern WHOLE_DATA_ELEMENT = Pattern.compile(" *(\\d{3})*[SX|+\\-*# ]*(\\d{4}) *(.*) *.*([CM]) *(an|n|a)(\\.*)(\\d*)");

    /**
     * Extracts information from data element occurring on one single row in Composite definition.
     * Example - "010    9173  Event description code                    C      an..35"
     * Group1 = line number
     * Group2 = id
     * Group3 = name
     */
    private static final Pattern FIRST_DATA_ELEMENT_PART = Pattern.compile(" *(\\d{3})*[SX|+\\-*# ]*(\\d{4}) *(.*) *");

    /**
     * Extracts information from data element occuring on one single row in Composite definition.
     * Example - "identifier                                M      an..35"
     * Group3 = name
     * Group4 = mandatory
     * Group5 = type
     * Group6 = min occurance
     * Group7 = max occurance
     */
    private static final Pattern SECOND_DATA_ELEMENT_PART = Pattern.compile(" *(.*) *.*([CM]) *(an|n|a)(\\.*)(\\d*)");

    /**
     * Extracts information from data header.
     * Example: "3237  Sample location description code                        [B]"
     * Group1 = id
     * Group2 = name
     * Group3 = usage (not used today)
     */
    private static final Pattern ELEMENT_HEADER = Pattern.compile("[SX|+\\-*# ]*(\\w{4}) *(.*) *\\[(\\w)]");
    private static final Pattern ELEMENT_HEADER_OLD = Pattern.compile("[SX|+\\-*# ]*(\\w{4}) *(.*)");

    /**
     * Extracts information from composite header.
     * Example: "C001 TRANSPORT MEANS"
     * Group1 = id
     * Group2 = name
     */
    private static final Pattern COMPOSITE_HEADER = Pattern.compile("[SX|+\\-*# ]*(\\w{4}) *(.*)");

    /**
     * Extracts information from segment header.
     * Example: "AGR  AGREEMENT IDENTIFICATION"
     * Group1 = id
     * Group2 = name
     */
    private static final Pattern SEGMENT_HEADER = Pattern.compile("[SX|+\\-*# ]*(\\w{3}) *(.*)");

    /**
     * Extracts information from segment element. Could be either a Composite or a Data.
     * Example: "010    C779 ARRAY STRUCTURE IDENTIFICATION             M    1"
     * Group1 = line number
     * Group2 = id
     * Group3 = name
     * Group4 = mandatory
     * Group5 = cardinality
     */
    private static final Pattern SEGMENT_ELEMENT = Pattern.compile(" *\\** *(\\d{3})*[SX|+\\-*# ]*(\\d{4}|C\\d{3}) *(.*) *( C| M) *(\\d)?.*");

    /**
     * Extracts information from first segment element when Composite or Data element description exists on several
     * lines. Could be either a Composite or a Data.
     * Example: "010    C779 ARRAY STRUCTURE IDENTIFICATION
     *                       AND SOME MORE DESCRIPTION           M    1 an..15"
     * Group1 = line number
     * Group2 = id
     * Group3 = name
     * Group4 = mandatory
     */
    private static final Pattern FIRST_SEGMENT_ELEMENT = Pattern.compile(" *(\\d{3})*[SX|+\\-*# ]*(\\d{4}|C\\d{3}) *(.*)");

    /**
     * Extracts information from second segment element when Composite or Data element description exists on several
     * lines. Could be either a Composite or a Data.
     * Example: "010    C779 ARRAY STRUCTURE IDENTIFICATION
     *                       AND SOME MORE DESCRIPTION           M    1 an..15"
     * Group1 = line number
     * Group2 = id
     * Group3 = name
     */
    private static final Pattern SECOND_SEGMENT_ELEMENT = Pattern.compile("^(.*) *( C| M) *(\\d)?.*");

    protected static final Pattern CODE_ID = Pattern.compile("[SX|+\\-*# ]*(\\w{4}) *(.*)");

    /**
     * Extracts information from a code.
     * Example: "+    533   Original accounting voucher"
     * Group1 = change indicator
     * Group2 = code
     */
    private static final Pattern CODE = Pattern.compile("^([+*#|X]{0,2} +)([A-Z0-9]+) +.+");

    private HashMap<String, CodeList> codeLists;
    private HashMap<String, Field> compositeDataElements;
    private HashMap<String, Component> dataElements;
    private ArrayList<Segment> segments;

    @Override
    public List<Segment> readSegments() throws IOException, EdiParseException {
        if (segments == null) {
            segments = new ArrayList<>();

            BufferedReader _reader = new BufferedReader(standardSegmentsDirectoryReader);
            moveToNextPart(_reader);

            Map<String, Component> datas = readDataElements();
            Map<String, Field> composites = readCompositeDataElements();
            Segment segment = getSegment(_reader, composites, datas);
            while (segment != null) {
                segments.add(segment);
                segment = getSegment(_reader, composites, datas);
            }
        }

        return segments;
    }

    public UnEdifactDirectoryParser(ZipInputStream zipInputStream, boolean useImport, boolean useShortName) throws IOException {
        this.useImport = useImport;
        this.useShortName = useShortName;

        definitionFiles = new HashMap<>();
        messageFiles = new HashMap<>();
        doReadDefinitionEntries(zipInputStream);

        if (versions.size() != 1) {
            if (versions.size() == 0) {
                throw new EdiParseException("Seems that we have a directory containing 0 parseable version inside: " + versions + ".\n All messages:\n\t" + messages);
            }
            throw new EdiParseException("Seems that we have a directory containing more than one parseable version inside: " + versions + ".\n All messages:\n\t" + messages);
        }
        // Read Definition Configuration
        parseEDIDefinitionFiles();
    }

    protected void doReadDefinitionEntries(ZipInputStream zipInputStream) throws IOException {
        readDefinitionEntries(zipInputStream,
                new ZipDirectoryEntry("eded.", definitionFiles),
                new ZipDirectoryEntry("edcd.", definitionFiles),
                new ZipDirectoryEntry("edsd.", definitionFiles),
                new ZipDirectoryEntry("uncl.", "uncl", definitionFiles),
                new ZipDirectoryEntry("edmd.", "*", messageFiles));
    }

    @Override
    public String getVersion() {
        return versions.iterator().next();
    }

    @Override
    public Set<String> getMessageNames(Edimap edimap) {
        Set<String> names = new LinkedHashSet<>();
        names.add(edimap.getDescription().getName());
        names.addAll(messageFiles.keySet());
        return names;
    }

    @Override
    public Edimap getMappingModel(String messageName, Edimap edimap) throws IOException {
        if (messageName.equals(edimap.getDescription().getName())) {
            return edimap;
        } else {
            return parseEdiMessage(messageName, edimap).getEdimap();
        }
    }

    @Override
    public Properties getInterchangeProperties() {
        Properties properties = new Properties();

        properties.setProperty(INTERCHANGE_TYPE, INTERCHANGE_TYPE);
        properties.setProperty(MESSAGE_BINDING_CONFIG, "/org/smooks/edi/unedifact/model/r41/bindings/unedifact-message.xml");
        properties.setProperty(INTERCHANGE_BINDING_CONFIG, "/org/smooks/edi/unedifact/model/r41/bindings/unedifact-interchange.xml");

        return properties;
    }

    @Override
    public EdiDirectory getEdiDirectory(Edimap edimap, String... includeMessages) throws IOException {
        if (ediDirectory == null) {
            Set<String> includeMessageSet = null;
            String commonMessageName = getCommonMessageName();
            Set<String> messages = getMessageNames(edimap);
            Edimap commonModel = null;
            List<Edimap> models = new ArrayList<>();

            if (includeMessages != null && includeMessages.length > 0) {
                includeMessageSet = new HashSet<>(Arrays.asList(includeMessages));
            }

            for (String message : messages) {
                if (includeMessageSet != null && !message.equals(commonMessageName)) {
                    if (!includeMessageSet.contains(message)) {
                        // Skip this message...
                        continue;
                    }
                }

                Edimap model = getMappingModel(message, edimap);

                EdiConvertionTool.removeDuplicateSegments(model.getSegments());

                if (message.equals(commonMessageName)) {
                    if (commonModel == null) {
                        commonModel = model;
                    } else {
                        LOGGER.warn("Common model message '" + commonMessageName + "' already read.");
                    }
                } else {
                    models.add(model);
                }
            }

            ediDirectory = new EdiDirectory(commonModel, models);
        }

        return ediDirectory;
    }

    private String getCommonMessageName() {
        return EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.getName();
    }

    private UnEdifactMessage parseEdiMessage(String messageName, Edimap edimap) throws IOException {
        byte[] message = messageFiles.get(messageName);

        if (message != null) {
            try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(message))) {
                return new UnEdifactMessage(reader, useImport, useShortName, edimap);
            }
        }
        return null;
    }

    protected void parseEDIDefinitionFiles() throws EdiParseException {
        dataElementsDirectoryReader = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("eded.")));
        compositeDataElementsDirectoryReader = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("edcd.")));
        standardSegmentsDirectoryReader = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("edsd.")));
        consolidatedCodeListReader = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("uncl")));
    }

    protected void readDefinitionEntries(ZipInputStream folderZip, ZipDirectoryEntry... entries) throws IOException {
        ZipEntry fileEntry = folderZip.getNextEntry();
        while (fileEntry != null) {
            String fName = new File(fileEntry.getName().toLowerCase()).getName().toLowerCase();
            for (ZipDirectoryEntry entry : entries) {
                if (fName.startsWith(entry.getDirectory().toLowerCase())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    byte[] bytes = new byte[BUFFER];
                    int size;
                    while ((size = folderZip.read(bytes, 0, bytes.length)) != -1) {
                        baos.write(bytes, 0, size);
                    }

                    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
                    readZipEntry(entry.getEntries(), zipInputStream, entry.getFile());
                    zipInputStream.close();
                }
            }
            folderZip.closeEntry();
            fileEntry = folderZip.getNextEntry();
        }
    }

    private void readZipEntry(Map<String, byte[]> files, ZipInputStream folderZip, String entry) throws IOException {
        ZipEntry fileEntry = folderZip.getNextEntry();
        while (fileEntry != null) {
            String fileName = fileEntry.getName();
            String fName = new File(fileName.toLowerCase()).getName();
            if (fName.startsWith(entry) || entry.equals("*")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] bytes = new byte[2048];
                int size;
                while ((size = folderZip.read(bytes, 0, bytes.length)) != -1) {
                    translatePseudoGraph(bytes);
                    baos.write(bytes, 0, size);
                }

                File file = new File(fileName);
                String messageName = file.getName().toUpperCase();

                messages.add(messageName);
                if (entry.equals("*")) {
                    Matcher match = ENTRY_FILE_NAME.matcher(messageName);
                    if (match.matches()) {
                        String entryName = match.group(1);
                        addEntry(entryName, baos.toByteArray(), files);
                        versions.add((match.group(2) + match.group(3)).toLowerCase());
                    }
                } else {
                    addEntry(entry, baos.toByteArray(), files);
                }
            }
            folderZip.closeEntry();
            fileEntry = folderZip.getNextEntry();
        }
    }

    private void addEntry(final String entry, final byte[] bytes, final Map<String, byte[]> files) {
        if (files.containsKey(entry)) {
            final byte[] available = files.get(entry);
            final byte[] combined = new byte[available.length + bytes.length];
            System.arraycopy(available, 0, combined, 0, available.length);
            System.arraycopy(bytes, 0, combined, available.length, bytes.length);
            files.put(entry, combined);
        } else {
            files.put(entry, bytes);
        }
    }

    @Override
    public Map<String, byte[]> getDefinitionFiles() {
        return this.definitionFiles;
    }

    private static void translatePseudoGraph(byte[] bytes) {
        for (int i = 0, l = bytes.length; i < l; i++) {
            switch (bytes[i]) {
                case (byte) 0xC4:
                    bytes[i] = (byte) '-';
                    break;

                case (byte) 0xC1:
                case (byte) 0xBF:
                case (byte) 0xD9:
                    bytes[i] = (byte) '+';
                    break;

                case (byte) 0xB3:
                    bytes[i] = (byte) '|';
                    break;
            }
        }
    }

    protected Segment getSegment(BufferedReader reader, Map<String, Field> fields, Map<String, Component> components) throws IOException, EdiParseException {
        //Read id and name.
        String line = readUntilValue(reader);

        if (line == null) {
            return null;
        }

        String segcode, name;
        Matcher headerMatcher = SEGMENT_HEADER.matcher(line);
        if (headerMatcher.matches()) {
            segcode = headerMatcher.group(1);
            name = headerMatcher.group(2);
        } else {
            throw new EdiParseException("Unable to extract segment code and name for Segment from line [" + line + "].");
        }

        String description = getValue(reader, "Function:");

        Segment segment = new Segment();
        segment.setSegcode(segcode);
        segment.setName(name);
        if (useShortName) {
            segment.setXmltag(XmlTagEncoder.encode(segcode.trim()));
        } else {
            segment.setXmltag(XmlTagEncoder.encode(name.trim()));
        }
        segment.setDescription(description);
        segment.setTruncatable(true);

        line = readUntilValue(reader);

        Matcher matcher;
        while (line != null && !line.matches(ELEMENT_SEPARATOR)) {
            matcher = SEGMENT_ELEMENT.matcher(line);
            if (matcher.matches()) {
                addFieldToSegment(fields, components, segment, matcher.group(2), matcher.group(4).trim().equalsIgnoreCase("M"), (matcher.groupCount() > 4 && matcher.group(5) != null) ? Integer.parseInt(matcher.group(5)) : 1);
                if (matcher.group(2).startsWith("C")) {
                    while (line != null && !line.equals("")) {
                        line = reader.readLine();
                    }
                }
            } else {
                matcher = FIRST_SEGMENT_ELEMENT.matcher(line);
                if (matcher.matches()) {
                    String id = matcher.group(2);
                    line = reader.readLine();
                    if (line == null) {
                        continue;
                    }
                    matcher = SECOND_SEGMENT_ELEMENT.matcher(line);
                    if (matcher.matches()) {
                        addFieldToSegment(fields, components, segment, id, matcher.group(2).trim().equalsIgnoreCase("M"), (matcher.groupCount() > 2 && matcher.group(3) != null) ? Integer.parseInt(matcher.group(3)) : 1);
                    }
//                    } else {
//                        throw new EdiParseException("Unable to match current line in segment description file. Erranous line [" + line + "].");
//                    }
                }
            }
            line = reader.readLine();
        }
        return segment;
    }

    protected void addFieldToSegment(Map<String, Field> fields, Map<String, Component> components, Segment segment, String id, boolean isMandatory, int cardinality) {
        final Field newField;
        if (id.toUpperCase().startsWith("C")) {
            newField = copyField(fields.get(id), isMandatory);
        } else {
            newField = convertToField(components.get(id), isMandatory);
        }
        newField.setCardinality(cardinality);
        segment.getFields().add(newField);
    }

    protected Field convertToField(Component component, boolean isMandatory) {
        Field field = new Field();
        field.setName(component.getName());
        field.setXmltag(XmlTagEncoder.encode(component.getXmltag()));
        field.setNodeTypeRef(component.getNodeTypeRef());
        field.setDocumentation(component.getDocumentation());
        field.setMaxLength(component.getMaxLength());
        field.setMinLength(component.getMinLength());
        field.setRequired(isMandatory);
        field.setTruncatable(true);
        field.setDataType(component.getDataType());
        field.setCodeList(component.getCodeList());

        return field;
    }

    protected Field copyField(Field oldField, boolean isMandatory) {
        Field field = new Field();
        field.setName(oldField.getName());
        field.setXmltag(XmlTagEncoder.encode(oldField.getXmltag()));
        field.setNodeTypeRef(oldField.getNodeTypeRef());
        field.setDocumentation(oldField.getDocumentation());
        field.setMaxLength(oldField.getMaxLength());
        field.setMinLength(oldField.getMinLength());
        field.setRequired(isMandatory);
        field.setTruncatable(true);
        field.setDataType(oldField.getDataType());
        field.getComponents().addAll(oldField.getComponents());
        return field;
    }

    @Override
    public Map<String, Field> readCompositeDataElements() throws IOException, EdiParseException {
        if (compositeDataElements == null) {
            compositeDataElements = new HashMap<>();

            BufferedReader _reader = new BufferedReader(compositeDataElementsDirectoryReader);
            moveToNextPart(_reader);

            Field field = new Field();
            Map<String, Component> dataElements = readDataElements();
            String id = populateDataElement(_reader, dataElements, field);
            while (id != null) {
                compositeDataElements.put(id, field);
                field = new Field();
                id = populateDataElement(_reader, dataElements, field);
            }
        }

        return compositeDataElements;
    }

    public Map<String, CodeList> readCodes() throws IOException, EdiParseException {
        if (codeLists == null) {
            this.codeLists = new HashMap<>();
            BufferedReader _reader = new BufferedReader(consolidatedCodeListReader);
            moveToNextPart(_reader);

            CodeList codeList = new CodeList();
            String id = populateCodeList(_reader, codeList);
            while (id != null) {
                codeLists.put(id, codeList);
                codeList = new CodeList();
                id = populateCodeList(_reader, codeList);
            }
        }

        return codeLists;
    }

    protected String populateCodeList(BufferedReader reader, CodeList codeList) throws IOException, EdiParseException {
        String line = readUntilValue(reader);

        if (line == null) {
            return null;
        }

        String id;
        Matcher headerMatcher = CODE_ID.matcher(line);
        if (headerMatcher.matches()) {
            id = headerMatcher.group(1);
        } else {
            throw new EdiParseException("Unable to extract id and name for code from line [" + line + "].");
        }

        codeList.setDocumentation(getValue(reader, "Desc:"));
        line = readUntilCode(reader);
        Matcher firstCodeMatcher = CODE.matcher(line);
        firstCodeMatcher.matches();
        int expectedIndentLength = firstCodeMatcher.group(1).length();
        while (line != null && !line.matches(ELEMENT_SEPARATOR)) {
            Matcher codeMatcher = CODE.matcher(line);
            if (isCode(codeMatcher, expectedIndentLength)) {
                codeList.getCodes().add(codeMatcher.group(2));
            }

            line = reader.readLine();
        }

        return id;
    }

    protected boolean isCode(Matcher codeMatcher, int expectedIndentLength) {
        return codeMatcher.matches() && codeMatcher.group(1).length() == expectedIndentLength;
    }

    protected String readUntilCode(BufferedReader reader) throws IOException {
        String line = readUntilValue(reader);
        while (line != null && !CODE.matcher(line).matches()) {
            while (line.length() > 0) {
                line = reader.readLine();
            }
            line = reader.readLine();
        }

        return line;
    }

    protected String populateDataElement(BufferedReader reader, Map<String, Component> dataElements, Field field) throws IOException, EdiParseException {
        //Read id and name.
        String line = readUntilValue(reader);

        if (line == null) {
            return null;
        }

        String id, name;
        Matcher headerMatcher = COMPOSITE_HEADER.matcher(line);
        if (headerMatcher.matches()) {
            id = headerMatcher.group(1);
            name = headerMatcher.group(2);
        } else {
            throw new EdiParseException("Unable to extract id and name for Composite element from line [" + line + "].");
        }

        String description = getValue(reader, "Desc:");

        field.setName(name);
        field.setNodeTypeRef(id);
        if (useShortName) {
            field.setXmltag(XmlTagEncoder.encode(id));
        } else {
            field.setXmltag(XmlTagEncoder.encode(name));
        }
        field.setDocumentation(description);

        line = readUntilValue(reader);
        LinePart linePart;
        while (line != null && !line.matches(ELEMENT_SEPARATOR)) {
            linePart = getLinePart(reader, line);
            if (linePart != null) {
                Component component = new Component();
                component.setRequired(linePart.isMandatory());
                populateComponent(component, dataElements.get(linePart.getId()));
                field.getComponents().add(component);
            }
            line = reader.readLine();
        }

        return id;
    }

    private void populateComponent(Component toComponent, Component fromComponent) {
        toComponent.setDocumentation(fromComponent.getDocumentation());
        toComponent.setMaxLength(fromComponent.getMaxLength());
        toComponent.setMinLength(fromComponent.getMinLength());
        toComponent.setTruncatable(true);
        toComponent.setDataType(fromComponent.getDataType());
        toComponent.setXmltag(XmlTagEncoder.encode(fromComponent.getXmltag()));
        toComponent.setName(fromComponent.getName());
        toComponent.setCodeList(fromComponent.getCodeList());
        toComponent.setNodeTypeRef(fromComponent.getNodeTypeRef());
    }

    @Override
    public Map<String, Component> readDataElements() throws IOException, EdiParseException {
        if (dataElements == null) {
            Map<String, CodeList> codeLists = readCodes();
            BufferedReader _reader = new BufferedReader(dataElementsDirectoryReader);
            moveToNextPart(_reader);
            Component component = new Component();
            String id = populateComponent(_reader, component, codeLists);
            dataElements = new HashMap<>();

            while (id != null) {
                dataElements.put(id, component);
                moveToNextPart(_reader);
                component = new Component();
                id = populateComponent(_reader, component, codeLists);
            }
        }

        return dataElements;
    }

    private String populateComponent(BufferedReader reader, Component component, Map<String, CodeList> codeLists) throws IOException, EdiParseException {

        //Read id and name.
        String line = readUntilValue(reader);

        if (line == null) {
            return null;
        }

        String id, name;
        Matcher headerMatcher = ELEMENT_HEADER.matcher(line);
        if (headerMatcher.matches()) {
            id = headerMatcher.group(1);
            name = headerMatcher.group(2);
        } else {
            Matcher headerMatcherOld = ELEMENT_HEADER_OLD.matcher(line);
            if (headerMatcherOld.matches()) {
                id = headerMatcherOld.group(1);
                name = headerMatcherOld.group(2);
            } else {
                throw new EdiParseException("Unable to extract id and name for Data element from line [" + line + "].");
            }
        }

        String description = getValue(reader, "Desc:");

        String repr = getValue(reader, "Repr:");
        String[] typeAndOccurance = repr.split(DOTS);

        component.setName(name);
        component.setNodeTypeRef(id);
        if (useShortName) {
            component.setXmltag(XmlTagEncoder.encode((DATA_ELEMENT_PREFIX + id).trim()));
        } else {
            component.setXmltag(XmlTagEncoder.encode(name.trim()));
        }
        component.setDataType(getType(typeAndOccurance));
        component.setMinLength(getMinLength(typeAndOccurance));
        component.setMaxLength(getMaxLength(typeAndOccurance));
        component.setDocumentation(description);
        component.setCodeList(codeLists.get(id));

        return id;
    }

    private int getMinLength(String[] typeAndOccurrence) {
        if (typeAndOccurrence.length == 0) {
            return 0;
        } else if (typeAndOccurrence.length == 1) {
            return Integer.parseInt(typeAndOccurrence[0].trim().replace("a", "").replace("n", ""));
        } else { // .. is considered to be from 0.
            return 0;
        }
    }

    private int getMaxLength(String[] typeAndOccurrence) {
        if (typeAndOccurrence.length == 0) {
            return 0;
        } else if (typeAndOccurrence.length == 1) {
            return Integer.parseInt(typeAndOccurrence[0].trim().replace("a", "").replace("n", ""));
        } else { // .. is considered to be from 0.
            return Integer.parseInt(typeAndOccurrence[1].trim());
        }
    }

    private String getType(String[] typeAndOccurrence) {
        if (typeAndOccurrence.length == 0) {
            return "String";
        }

        if (typeAndOccurrence[0].trim().startsWith("n")) {
            return "DABigDecimal";
        } else {
            return "String";
        }
    }

    protected static String getValue(BufferedReader reader, String prefix) throws IOException {
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = readUntilValue(reader)) != null) {
            line = line.replace("|", "").trim();
            if (line.startsWith(prefix)) {
                result.append(line.replace(prefix, ""));
                line = reader.readLine();
                while (line != null && line.trim().length() != 0) {
                    result.append(" ").append(line.trim());
                    line = reader.readLine();
                }
                break;
            }
        }
        return result.toString().trim();
    }

    protected static String readUntilValue(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while (line != null && line.length() == 0) {
            line = reader.readLine();
        }
        return line;
    }

    private void moveToNextPart(BufferedReader reader) throws IOException {
        String currentLine = "";

        while (currentLine != null && !currentLine.matches(ELEMENT_SEPARATOR)) {
            currentLine = reader.readLine();
        }
    }

    protected LinePart getLinePart(BufferedReader reader, String line) throws IOException {
        LinePart part = null;

        Matcher matcher = WHOLE_DATA_ELEMENT.matcher(line);
        if (matcher.matches()) {
            part = new LinePart(matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5), matcher.group(6), matcher.group(7));
        } else {
            matcher = FIRST_DATA_ELEMENT_PART.matcher(line);
            if (matcher.matches()) {
                part = new LinePart(matcher.group(2), matcher.group(3));

                line = reader.readLine();
                matcher = SECOND_DATA_ELEMENT_PART.matcher(line);
                if (matcher.matches()) {
                    part.setDescription(part.getDescription() + " " + matcher.group(1));
                    part.setMandatory(matcher.group(2));
                    part.setType(matcher.group(3));
                    part.setMinOccurance(matcher.group(4), matcher.group(5));
                    part.setMaxOccurance(matcher.group(5));
                } else {
                    // we know that the second line doesn't contain anything useful and the first
                    // line didn't contain the whole data necessary, thus we can be sure that the
                    // lines do not form a valid definition
                    return null;
                }
            }
        }

        return part;
    }

    private static class LinePart {
        private String id;
        private String description;
        private String type;
        private Integer minOccurance;
        private Integer maxOccurance;
        private boolean isMandatory;

        public LinePart(String id, String description, String mandatory, String type, String minOccurs, String maxOccurs) {
            this.id = id;
            this.description = description;
            setMandatory(mandatory);
            setType(type);
            setMinOccurance(minOccurs, maxOccurs);
            setMaxOccurance(maxOccurs);
        }

        private void setMandatory(String mandatory) {
            this.isMandatory = mandatory.equalsIgnoreCase("M");
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setMaxOccurance(String maxOccurs) {
            this.maxOccurance = Integer.valueOf(maxOccurs);
        }

        public void setMinOccurance(String minOccurs, String maxOccurs) {
            this.minOccurance = minOccurs.equals("src/main") ? 0 : Integer.parseInt(maxOccurs);
        }

        public void setType(String type) {
            if (type.equalsIgnoreCase("n")) {
                this.type = "DABigDecimal";
            } else {
                this.type = "String";
            }
        }

        public LinePart(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public Integer getMinOccurance() {
            return minOccurance;
        }

        public Integer getMaxOccurance() {
            return maxOccurance;
        }

        public boolean isMandatory() {
            return isMandatory;
        }
    }

    @Override
    public boolean isUseShortName() {
        return useShortName;
    }
}
