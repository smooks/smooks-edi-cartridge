/*-
 * ========================LICENSE_START=================================
 * smooks-edi-cartridge
 * %%
 * Copyright (C) 2020 - 2023 Smooks
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
package org.smooks.cartridges.edi.parser;

import org.smooks.cartridges.dfdl.parser.DfdlParser;

import java.util.AbstractMap;

public class EdiParser extends DfdlParser {

    @Override
    protected AbstractMap<String, String> getVariables()  {
        AbstractMap<String, String> variables = super.getVariables();

        variables.put("{http://www.ibm.com/dfdl/EDI/Format}SegmentTerm", resourceConfig.getParameterValue("segmentTerminator", String.class, "'%NL;%WSP*; '%WSP*;"));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}FieldSep", resourceConfig.getParameterValue("dataElementSeparator", String.class, "+"));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}CompositeSep", resourceConfig.getParameterValue("compositeDataElementSeparator", String.class, ":"));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}EscapeChar", resourceConfig.getParameterValue("escapeCharacter", String.class, "?"));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}RepeatSep", resourceConfig.getParameterValue("repetitionSeparator", String.class, "*"));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}DecimalSep", resourceConfig.getParameterValue("decimalSign", String.class, "."));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}GroupingSep", resourceConfig.getParameterValue("triadSeparator", String.class, ","));
        variables.put("{http://www.ibm.com/dfdl/EDI/Format}Encoding", resourceConfig.getParameterValue("encoding", String.class, executionContext.getContentEncoding().toLowerCase()));

        return variables;
    }

}