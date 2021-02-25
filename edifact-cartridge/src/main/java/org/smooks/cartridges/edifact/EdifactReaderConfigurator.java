/*-
 * ========================LICENSE_START=================================
 * smooks-edifact-cartridge
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
package org.smooks.cartridges.edifact;

import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.edi.EdiReaderConfigurator;
import org.smooks.engine.resource.config.DefaultParameter;

import java.util.ArrayList;
import java.util.List;

public class EdifactReaderConfigurator extends EdiReaderConfigurator {

    protected List<String> messageTypes = new ArrayList<>();

    public EdifactReaderConfigurator(final String schemaUri) {
        super(schemaUri);
    }

    protected String getDataProcessorFactory() {
        return "org.smooks.cartridges.edifact.EdifactDataProcessorFactory";
    }

    public List<String> getMessageTypes() {
        return messageTypes;
    }

    public EdifactReaderConfigurator setMessageTypes(List<String> messageTypes) {
        AssertArgument.isNotNull(variables, "messageTypes");
        this.messageTypes = messageTypes;

        return this;
    }

    @Override
    public List<ResourceConfig> toConfig() {
        final List<ResourceConfig> resourceConfigs = super.toConfig();
        final ResourceConfig resourceConfig = resourceConfigs.get(0);

        for (String messageType : messageTypes) {
            resourceConfig.setParameter(new DefaultParameter<>("messageType", messageType));
        }

        return resourceConfigs;
    }
}
