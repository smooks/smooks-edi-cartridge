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

import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.ect.EdiParseException;
import org.smooks.edi.edisax.interchange.ControlBlockHandlerFactory;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.model.internal.Component;
import org.smooks.edi.edisax.model.internal.Description;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.model.internal.SegmentGroup;
import org.smooks.edi.edisax.unedifact.UNEdifactInterchangeParser;
import org.smooks.edi.edisax.unedifact.handlers.r41.UNEdifact41ControlBlockHandlerFactory;
import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.support.ClassUtils;

import java.io.IOException;
import java.util.*;

public class UnEdifactDefinitionReader {

    private static final String INTERCHANGE_DEFINITION = "un-edifact-interchange-definition.xml";
    private static final String INTERCHANGE_DEFINITION_SHORTNAME = "un-edifact-interchange-definition-shortname.xml";

    public static Edimap parse(DirectoryParser directoryParser) throws IOException, EdiParseException {
        Map<String, Component> datas = directoryParser.readDataElements();
        Map<String, Field> composites = directoryParser.readCompositeDataElements();
        List<Segment> segments = directoryParser.readSegments();

        Edimap edimap = new Edimap();
        edimap.setSegments(new SegmentGroup());
        edimap.getSegments().getSegments().addAll(segments);
        edimap.setSimpleDataElements(Arrays.asList(datas.values().toArray(new Component[]{})));
        edimap.setCompositeDataElements(Arrays.asList(composites.values().toArray(new Field[]{})));
        edimap.setDescription((Description) EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.clone());
        edimap.getSegments().setXmltag("DefinitionMap");
        edimap.setDelimiters(UNEdifactInterchangeParser.defaultUNEdifactDelimiters);

        addMissingDefinitions(edimap);
        edimap.getDescription().setNamespace(ControlBlockHandlerFactory.NAMESPACE_ROOT + ":un:" + directoryParser.getVersion() + ":common");

        try {
            String interchangeSegmentDefinitions = INTERCHANGE_DEFINITION_SHORTNAME;
            if (!directoryParser.isUseShortName()) {
                interchangeSegmentDefinitions = INTERCHANGE_DEFINITION;
            }
            EdifactModel interchangeEnvelope = new EdifactModel(ClassUtils.getResourceAsStream(interchangeSegmentDefinitions, UnEdifactDefinitionReader.class));
            edimap.getSegments().getSegments().addAll(interchangeEnvelope.getEdimap().getSegments().getSegments());
        } catch (Exception e) {
            throw new EdiParseException(e.getMessage(), e);
        }

        return edimap;
    }

    private static void addMissingDefinitions(Edimap definitionModel) {
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
}