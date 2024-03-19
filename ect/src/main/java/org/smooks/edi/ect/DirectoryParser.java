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
package org.smooks.edi.ect;

import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.edisax.interchange.EdiDirectory;
import org.smooks.edi.edisax.model.internal.Component;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;
import org.smooks.edi.edisax.model.internal.Segment;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * EDI Specification Reader.
 *
 * @author bardl
 */
public interface DirectoryParser {

    /**
     * Interchange properties key for the interchange Type e.g. "UNEDIFACT".
     */
    String INTERCHANGE_TYPE = "interchangeType";

    /**
     * Interchange properties key for the interchange message binding config.
     */
    String MESSAGE_BINDING_CONFIG = "messageBindingConfig";
    /**
     * Interchange properties key for the top level interchange binding config.
     */
    String INTERCHANGE_BINDING_CONFIG = "interchangeBindingConfig";

    String getVersion();

    /**
     * Get a list of the names of the messages defined in the EDI Specification (e.g. UN/EDIFACT
     * specification) instance.
     *
     * @return The namels of the messages.
     */
    Set<String> getMessageNames(Edimap edimap);

    /**
     * Get the EDI Mapping Model for the named message.
     * <br><br>
     * The Mapping Model is constructed after converting/translating the
     * message definition in the specification.  This is the "normalized"
     * definition of any EDI message in Smooks.  From the EDI Mapping Model,
     * EJC can be used to construct Java Bindings etc.
     *
     * @param messageName The name of the message.
     * @return The messages EDI Mapping Model.
     * @throws IOException Error reading/converting the message definition to
     *                     an EDI Mapping Model.
     */
    Edimap getMappingModel(String messageName, Edimap edimap) throws IOException;

    /**
     * Get the message interchange properties for the associated EDI specification.
     *
     * @return The message interchange properties for the associated EDI specification.
     */
    Properties getInterchangeProperties();

    /**
     * Get the {@link EdiDirectory} instance for specification.
     * <br><br>
     * Implementations should cache this instance.
     *
     * @param includeMessages Messages to include.
     * @return The EdiDirector instance.
     * @throws IOException Error reading specification.
     */
    EdiDirectory getEdiDirectory(Edimap edimap, String... includeMessages) throws IOException;

    Map<String, byte[]> getDefinitionFiles();

    Map<String, Field> readCompositeDataElements() throws IOException, EdiParseException;

    List<Segment> readSegments() throws IOException, EdiParseException;

    Map<String, Component> readDataElements() throws IOException, EdiParseException;

    boolean isUseShortName();
}