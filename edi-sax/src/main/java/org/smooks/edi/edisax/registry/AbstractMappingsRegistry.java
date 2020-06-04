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
import java.util.LinkedHashMap;
import java.util.Map;

import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.util.EDIUtils;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.xml.sax.SAXException;


/**
 * Base implementation of {@link MappingsRegistry} interface
 * 
 * @author zubairov
 *
 */
public abstract class AbstractMappingsRegistry implements MappingsRegistry {

	/**
	 * Internal storage 
	 */
	protected final Map<String, EdifactModel> content = new LinkedHashMap<String, EdifactModel>();

	/**
	 * {@inheritDoc}
	 */
	public EdifactModel getMappingModel(String messageName,
			Delimiters delimiters) throws EDIConfigurationException, SAXException, IOException {
		String[] nameComponents = EDIUtils.split(messageName,
				delimiters.getComponent(), delimiters.getEscape());
		StringBuilder lookupNameBuilder = new StringBuilder();
		// First 4 components are mandatory...we use those as the lookup...
		for (int i = 0; i < 4; i++) {
			if (i > 0) {
				lookupNameBuilder.append(':');
			}
			lookupNameBuilder.append(nameComponents[i]);
		}
		String lookupName = lookupNameBuilder.toString().trim();
		EdifactModel result = content.get(lookupName);
		if (result == null) {
            synchronized (content) {
                result = content.get(lookupName);
		        if (result == null) {
			        content.putAll(demandLoading(nameComponents));
                }
            }
			// Try again
			result = content.get(lookupName);
			if (result != null) {
				return result;
			}
		} else {
			return result;
		}
		throw new EDIConfigurationException("Mapping Model '" + messageName
				+ "' not found in supplied set of Mapping model.");
	}

	/**
	 * Loading mapping models on demand.
	 * This method should return either one or many mapping models
	 * loaded on-demand or just eagerly.
	 * 
	 * @param nameComponents
	 * @return
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws EDIConfigurationException 
	 */
	protected abstract Map<String, EdifactModel> demandLoading(String[] nameComponents) throws EDIConfigurationException, IOException, SAXException;

	
}
