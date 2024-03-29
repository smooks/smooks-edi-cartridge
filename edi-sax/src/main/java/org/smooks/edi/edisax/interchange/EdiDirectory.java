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
package org.smooks.edi.edisax.interchange;

import org.smooks.edi.edisax.model.internal.Edimap;

import java.util.ArrayList;
import java.util.List;

/**
 * EDI directory model.
 * <br><br>
 * Contains the mapping models for all message in an EDI directory/specification e.g.
 * for UN/EDIFACT it contains EDI mapping models for all the messages in a
 * UN/EDIFACT directory specification zip file.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class EdiDirectory {

    private Edimap commonModel = null;
    private List<Edimap> messageModels = new ArrayList<Edimap>();

    /**
     * Public constructor.
     *
     * @param commonModel   The "common" model.  Contains common types used across
     *                      (and imported by) the messages in the directory.
     * @param messageModels The individual message models.
     */
    public EdiDirectory(Edimap commonModel, List<Edimap> messageModels) {
        this.commonModel = commonModel;
        this.messageModels = messageModels;
    }

    /**
     * Get the common model.
     *
     * @return The common model.
     */
    public Edimap getCommonModel() {
        return commonModel;
    }

    /**
     * Get the message models.
     * <br><br>
     * This list does not contain the {@link #getCommonModel() common} model.
     *
     * @return The message models.
     */
    public List<Edimap> getMessageModels() {
        return messageModels;
    }
}
