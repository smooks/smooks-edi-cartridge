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
/*
	Milyn - Copyright (C) 2006 - 2010

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.smooks.edi.edisax.model.internal;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Segment Group.
 * 
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class SegmentGroup extends MappingNode {

    private List<SegmentGroup> segments;
    private Integer minOccurs;
    private Integer maxOccurs;

    public List<SegmentGroup> getSegments() {
        if (segments == null) {
            segments = new ArrayList<SegmentGroup>();
        }
        return this.segments;
    }

    public String getSegcode() {
        return segments.get(0).getSegcode();
    }

    public Pattern getSegcodePattern() {
        return segments.get(0).getSegcodePattern();
    }

    public int getMinOccurs() {
        if (minOccurs == null) {
            return  1;
        } else {
            return minOccurs;
        }
    }

    public void setMinOccurs(Integer value) {
        this.minOccurs = value;
    }

    public int getMaxOccurs() {
        if (maxOccurs == null) {
            return  1;
        } else {
            return maxOccurs;
        }
    }

    public void setMaxOccurs(Integer value) {
        this.maxOccurs = value;
    }
}
