/*-
 * ========================LICENSE_START=================================
 * smooks-edg
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
package org.smooks.edi.edg;

import org.apache.daffodil.io.processors.charset.CharsetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class EdifactDfdlSchemaGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdifactDfdlSchemaGenerator.class);

    private EdifactDfdlSchemaGenerator() {

    }

    public static void main(final String[] args) {
        //FIXME: https://issues.apache.org/jira/browse/DAFFODIL-2827
        CharsetUtils.supportedEncodingsString();
        EdifactDfdlSchemaFactory edifactDfdlSchemaFactory = new EdifactDfdlSchemaFactory();
        Arrays.stream(Arrays.copyOfRange(args, 0, args.length - 1)).parallel().forEach(a -> {
            final String[] argAsArray = a.split(",");
            final String directoryPath = argAsArray[0];
            final String directoryParserImpl = argAsArray[1];

            LOGGER.info("Generating schema from {}...", directoryPath);
            try {
                EdifactDfdlSchemaFile edifactDfdlSchemaFile = edifactDfdlSchemaFactory.create(directoryPath, directoryParserImpl, args[args.length - 1]);
                edifactDfdlSchemaFile.write();
            } catch (Exception e) {
                throw new EdifactDfdlSchemaGeneratorException(e);
            }
        });
    }
}
