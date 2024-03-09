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
package org.smooks.edi.edisax.registry;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.util.EDIUtils;
import org.xml.sax.SAXException;

/**
 * Default implementation of {@link MappingsRegistry}.
 * <p>
 * Default implementation loading EDIFACT models based on the specified list of model files
 * which could be provided either via constructor or via {@link #addModelReferences(String, URI)} method.
 *
 * @author zubairov
 */
public class DefaultMappingsRegistry extends AbstractMappingsRegistry {

    private final Map<String, URI> modelReferences = new HashMap<String, URI>();

    /**
     * Constructor mostly used for tests
     *
     * @param models
     */
    public DefaultMappingsRegistry(EdifactModel... models) {
        for (EdifactModel model : models) {
            content.put(EDIUtils.toLookupName(model.getDescription()), model);
        }
    }

    /**
     * Loading mapping model out of ZIP file
     *
     * @param string
     * @param baseURI
     * @throws SAXException
     * @throws IOException
     * @throws EDIConfigurationException
     */
    public DefaultMappingsRegistry(String mappingModelFiles, URI baseURI) throws EDIConfigurationException, IOException, SAXException {
        addModelReferences(mappingModelFiles, baseURI);
    }

    /**
     * Add references to the lookup list.
     *
     * @param references
     */
    public void addModelReferences(String models, URI baseURI) {
        String[] mappingModelFileTokens = models.split(",");
        for (String modelRef : mappingModelFileTokens) {
            modelReferences.put(modelRef, baseURI);
        }
    }

    /**
     * This method load all mapping models which are declared in
     * {@link #modelReferences} map and returns them all back.
     * It is actually ignoring nameComponents parameter
     * because no on-demand loading is happening here.
     *
     * @param nameComponents
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws EDIConfigurationException
     */
    protected synchronized Map<String, EdifactModel> demandLoading(String[] nameComponents) throws EDIConfigurationException, IOException, SAXException {
        Map<String, EdifactModel> result = new LinkedHashMap<String, EdifactModel>();
        Set<Entry<String, URI>> set = modelReferences.entrySet();
        for (Entry<String, URI> entry : set) {
            EDIUtils.loadMappingModels(entry.getKey(), result, entry.getValue());
        }
        return result;
    }


}
