/*
 * Milyn - Copyright (C) 2006 - 2011
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (version 2.1) as published by the Free Software
 * Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details:
 * http://www.gnu.org/licenses/lgpl.txt
 */
package org.smooks.edi.edisax.registry;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.smooks.edi.edisax.EDIConfigurationException;
import org.smooks.edi.edisax.model.EdifactModel;
import org.smooks.edi.edisax.util.EDIUtils;
import org.xml.sax.SAXException;

/**
 * Mappings registry that lazily loading UN/EDIFACT mappings
 * out of the classpath.
 * 
 * @author zubairov
 *
 */
public class LazyMappingsRegistry extends AbstractMappingsRegistry {

	@Override
	protected synchronized Map<String, EdifactModel> demandLoading(String[] nameComponents)
			throws EDIConfigurationException, IOException, SAXException {
		String urn = "urn:org.smooks.edi.unedifact:" + nameComponents[1] + nameComponents[2] + "-mapping:*";
		Map<String, EdifactModel> result = new LinkedHashMap<String, EdifactModel>();
		EDIUtils.loadMappingModels(urn.toLowerCase(), result, URI.create("/"));
		return result;
	}

}
