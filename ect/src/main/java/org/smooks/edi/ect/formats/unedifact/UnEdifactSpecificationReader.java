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
package org.smooks.edi.ect.formats.unedifact;

import org.smooks.edi.ect.EdiConvertionTool;
import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.ect.EdiSpecificationReader;
import org.smooks.edi.edisax.interchange.ControlBlockHandlerFactory;
import org.smooks.edi.edisax.interchange.EdiDirectory;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.model.internal.Description;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.unedifact.UNEdifactInterchangeParser;
import org.smooks.edi.edisax.unedifact.handlers.r41.UNEdifact41ControlBlockHandlerFactory;
import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * UN/EDIFACT Specification Reader.
 *
 * @author bardl
 */
public class UnEdifactSpecificationReader implements EdiSpecificationReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnEdifactSpecificationReader.class);

    public static final String INTERCHANGE_TYPE = "UNEDIFACT";

    private static final int BUFFER = 2048;
    private static final String INTERCHANGE_DEFINITION = "un-edifact-interchange-definition.xml";
    private static final String INTERCHANGE_DEFINITION_SHORTNAME = "un-edifact-interchange-definition-shortname.xml";

    private boolean useImport;
    private boolean useShortName;
    private Map<String, byte[]> definitionFiles;
    private Map<String, byte[]> messageFiles;
    private Edimap definitionModel;
    private Set<String> versions = new HashSet<String>();
    private Set<String> messages = new HashSet<String>();
    private EdiDirectory ediDirectory;
    /**
     * Matcher to recognize and parse entries like CUSCAR_D.08A
     */
	private Pattern entryFileName = Pattern.compile("^([A-Z]+)_([A-Z])\\.([0-9]+[A-Z])$");

    public UnEdifactSpecificationReader(ZipInputStream specificationInStream, boolean useImport) throws IOException {
        this(specificationInStream, useImport, true);
    }

    public UnEdifactSpecificationReader(ZipInputStream specificationInStream, boolean useImport, boolean useShortName) throws IOException {
        this.useImport = useImport;
        this.useShortName = useShortName;

        definitionFiles = new HashMap<>();
        messageFiles = new HashMap<>();
        readDefinitionEntries(specificationInStream, new ZipDirectoryEntry("eded.", definitionFiles), new ZipDirectoryEntry("edcd.", definitionFiles), new ZipDirectoryEntry("edsd.", definitionFiles), new ZipDirectoryEntry("uncl.", definitionFiles), new ZipDirectoryEntry("edmd.", "*", messageFiles));

        if (versions.size() != 1) {
            if (versions.size() == 0) {
                throw new EdiParseException("Seems that we have a directory containing 0 parseable version inside: " + versions + ".\n All messages:\n\t" + messages);
            }
            throw new EdiParseException("Seems that we have a directory containing more than one parseable version inside: " + versions + ".\n All messages:\n\t" + messages);
        }
        String version = versions.iterator().next();
        // Read Definition Configuration
        definitionModel = parseEDIDefinitionFiles();

        addMissingDefinitions(definitionModel);
        definitionModel.getDescription().setNamespace(ControlBlockHandlerFactory.NAMESPACE_ROOT + ":un:" + version + ":common");

        //Interchange envelope is inserted into the definitions. Handcoded at the moment.
        try {
            String interchangeSegmentDefinitions = INTERCHANGE_DEFINITION_SHORTNAME;
            if (!useShortName) {
                interchangeSegmentDefinitions = INTERCHANGE_DEFINITION;
            }
            EdifactModel interchangeEnvelope = new EdifactModel(ClassUtil.getResourceAsStream(interchangeSegmentDefinitions, this.getClass()));
            definitionModel.getSegments().getSegments().addAll(interchangeEnvelope.getEdimap().getSegments().getSegments());
        } catch (Exception e) {
            throw new EdiParseException(e.getMessage(), e);
        }

    }

    public Set<String> getMessageNames() {
        Set<String> names = new LinkedHashSet<String>();
        names.add(definitionModel.getDescription().getName());
        names.addAll(messageFiles.keySet());
        return names;
    }

    public Edimap getMappingModel(String messageName) throws IOException {
        if(messageName.equals(definitionModel.getDescription().getName())) {
            return definitionModel;
        } else {
            return parseEdiMessage(messageName).getEdimap();
        }
    }

	public Properties getInterchangeProperties() {
        Properties properties = new Properties();

        properties.setProperty(EdiSpecificationReader.INTERCHANGE_TYPE, INTERCHANGE_TYPE);
        properties.setProperty(EdiSpecificationReader.MESSAGE_BINDING_CONFIG, "/org/smooks/edi/unedifact/model/r41/bindings/unedifact-message.xml");
        properties.setProperty(EdiSpecificationReader.INTERCHANGE_BINDING_CONFIG, "/org/smooks/edi/unedifact/model/r41/bindings/unedifact-interchange.xml");

        return properties;
    }

    public EdiDirectory getEdiDirectory(String... includeMessages) throws IOException {
        if(ediDirectory == null) {
            Set<String> includeMessageSet = null;
            String commonMessageName = getCommmonMessageName();
            Set<String> messages = getMessageNames();
            Edimap commonModel = null;
            List<Edimap> models = new ArrayList<Edimap>();

            if(includeMessages != null && includeMessages.length > 0) {
                includeMessageSet = new HashSet<String>(Arrays.asList(includeMessages));
            }

            for(String message : messages) {
                if (includeMessageSet != null && !message.equals(commonMessageName)) {
                    if (!includeMessageSet.contains(message)) {
                        // Skip this message...
                        continue;
                    }
                }

                Edimap model = getMappingModel(message);

                EdiConvertionTool.removeDuplicateSegments(model.getSegments());

                if(message.equals(commonMessageName)) {
                    if(commonModel == null) {
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

    private String getCommmonMessageName() {
        return EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.getName();
    }

    private void addMissingDefinitions(Edimap definitionModel) {
        Segment ugh = new Segment();
        Segment ugt = new Segment();

        ugh.setSegcode("UGH");
        ugh.setXmltag("UGH");
        ugh.addField(new Field("id", UNEdifact41ControlBlockHandlerFactory.NAMESPACE, true));

        ugt.setSegcode("UGT");
        ugt.setXmltag("UGT");
        ugt.addField(new Field("id", UNEdifact41ControlBlockHandlerFactory.NAMESPACE, true));

        definitionModel.getSegments().getSegments().add(ugh);
        definitionModel.getSegments().getSegments().add(ugt);
    }

    private UnEdifactMessage parseEdiMessage(String messageName) throws IOException {
        byte[] message = messageFiles.get(messageName);

        if (message != null) {
            try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(message))) {
                return new UnEdifactMessage(reader, useImport, useShortName, definitionModel);
            }
        }
        return null;
    }

    public Edimap getDefinitionModel() {
        return definitionModel;
    }

    private Edimap parseEDIDefinitionFiles() throws IOException, EdiParseException {
        Edimap edifactModel;
        Reader codeISR = null;

        try (Reader dataISR = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("eded.")));
             Reader compositeISR = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("edcd.")));
             Reader segmentISR = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("edsd.")))) {

            if (definitionFiles.get("uncl.") != null) {
                codeISR = new InputStreamReader(new ByteArrayInputStream(definitionFiles.get("uncl.")));
            }

            edifactModel = UnEdifactDefinitionReader.parse(dataISR, compositeISR, segmentISR, codeISR, useShortName);
            edifactModel.setDescription((Description) EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.clone());
            edifactModel.getSegments().setXmltag("DefinitionMap");
            edifactModel.setDelimiters(UNEdifactInterchangeParser.defaultUNEdifactDelimiters);
        } finally {
            if (codeISR != null) {
                codeISR.close();
            }
        }

        return edifactModel;
    }

    private void readDefinitionEntries(ZipInputStream folderZip, ZipDirectoryEntry... entries) throws IOException {

        ZipEntry fileEntry = folderZip.getNextEntry();
        while (fileEntry != null) {
            String fName = new File(fileEntry.getName().toLowerCase()).getName().replaceFirst("tr", "ed");
            for (ZipDirectoryEntry entry : entries) {
                if (fName.startsWith(entry.getDirectory())) {
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

    private  boolean readZipEntry(Map<String, byte[]> files, ZipInputStream folderZip, String entry) throws IOException {

        boolean result = false;

        ZipEntry fileEntry = folderZip.getNextEntry();
        while (fileEntry != null) {
            String fileName = fileEntry.getName();
			String fName = new File(fileName.toLowerCase()).getName().replaceFirst("tr", "ed");
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

                result = true;
                messages.add(messageName);
                if (entry.equals("*")) {
					Matcher match = entryFileName.matcher(messageName);
					if (match.matches()) {
                        String entryName = match.group(1);
						files.put(entryName, baos.toByteArray());
						versions.add((match.group(2) + match.group(3)).toLowerCase());
                    }
                } else {
                    files.put(entry, baos.toByteArray());
                    break;
                }
            }
            folderZip.closeEntry();
            fileEntry = folderZip.getNextEntry();
        }

        return result;
    }

    private static void translatePseudoGraph(byte[] bytes)
    {
	for (int i=0, l=bytes.length; i<l; i++)
	{
	    switch(bytes[i])
	    {
		case (byte)0xC4:
		    bytes[i] = (byte)'-';
		    break;

		case (byte)0xC1:
		case (byte)0xBF:
		case (byte)0xD9:
		    bytes[i] = (byte)'+';
		    break;

		case (byte)0xB3:
		    bytes[i] = (byte)'|';
		    break;
	    }
	}
    }

    private static class ZipDirectoryEntry {
        private String directory;
        private String file;
        private Map<String, byte[]> entries;

        private ZipDirectoryEntry(String directory, Map<String, byte[]> entries) {
            this(directory, directory, entries);
        }

        public ZipDirectoryEntry(String directory, String file, Map<String, byte[]> entries) {
            this.directory = directory;
            this.file = file;
            this.entries = entries;
        }

        public String getDirectory() {
            return directory;
        }

        public String getFile() {
            return file;
        }

        public Map<String, byte[]> getEntries() {
            return entries;
        }
    }

}
