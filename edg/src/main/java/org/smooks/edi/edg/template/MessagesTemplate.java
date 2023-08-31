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
package org.smooks.edi.edg.template;

import com.github.mustachejava.TemplateFunction;
import org.apache.commons.jxpath.JXPathContext;
import org.smooks.edi.ect.DirectoryParser;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.util.EDIUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class MessagesTemplate extends Template {
    private static final String SEQUENCE_XML_START_TAG = "<xsd:sequence dfdl:initiatedContent=\"yes\">\n";
    private static final String SEQUENCE_XML_END_TAG = "</xsd:sequence>\n";
    private static final String MESSAGE_SEGMENT_MUSTACHE_PARTIAL = "{{> MessageSegment.xsd.mustache}}\n";
    private static final String MESSAGE_SEGMENT_GROUP_MUSTACHE_PARTIAL = "{{> MessageSegmentGroup.xsd.mustache}}\n";

    private final List<String> messageTypes;

    public MessagesTemplate(final String version, final DirectoryParser directoryParser, final Edimap edimap) throws IOException {
        super(version);
        final List<Map<String, Object>> messageDefinitions = new ArrayList<>();
        messageTypes = directoryParser.getMessageNames(edimap).stream().filter(m -> !m.equals(EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.getName())).collect(Collectors.toList());
        for (String messageType : messageTypes) {
            final Edimap messageTypeEdimap = directoryParser.getMappingModel(messageType, edimap);
            final Map<String, Object> messageDefinition = OBJECT_MAPPER.convertValue(messageTypeEdimap, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            final List<Map<String, Object>> segments = (List<Map<String, Object>>) JXPathContext.newContext(messageDefinition).getValue("/segments/segments", List.class);
            templatizeSegments(segments);

            messageDefinitions.add(messageDefinition);
        }
        scope.put("messages", messageDefinitions);
    }

    private void templatizeSegments(final List<Map<String, Object>> segments) {
        templatizeSegments(segments.get(0), tail(segments), true, 0);
    }

    private int templatizeSegments(final Map<String, Object> headSegment, final List<Map<String, Object>> tailSegments, final boolean isFirstSegment, final int segmentGroupCounter) {
        final int segmentGroupIndex;
        final StringWriter stringWriter = new StringWriter();
        final List<Map<String, Object>> nestedSegments = (List<Map<String, Object>>) headSegment.get("segments");
        if (((int) headSegment.get("minOccurs") == 0) || ((int) headSegment.get("maxOccurs"))  > 1) {
            headSegment.put("occursCountKind", "parsed");
        }
        if (isEmpty(nestedSegments)) {
            segmentGroupIndex = segmentGroupCounter;
            writeSegment(tailSegments, stringWriter, isFirstSegment);
        } else {
            final int nestedSegmentGroupIndex = segmentGroupCounter + 1;
            writeSegmentGroup(headSegment, tailSegments, stringWriter, nestedSegmentGroupIndex);
            segmentGroupIndex = templatizeSegments(nestedSegments.get(0), tail(nestedSegments), true, nestedSegmentGroupIndex);
        }

        if (headSegment.get("segcode").equals("UNS") || headSegment.get("segcode").equals("UGH") || headSegment.get("segcode").equals("UGT")) {
            headSegment.put("namespacePrefix", "srv");
        } else {
            headSegment.put("namespacePrefix", version);
        }
        headSegment.put("render", (TemplateFunction) s -> stringWriter.toString());

        if (!tailSegments.isEmpty()) {
            return templatizeSegments(tailSegments.get(0), tail(tailSegments), false, segmentGroupIndex);
        } else {
            return segmentGroupIndex;
        }
    }

    private void writeSegment(final List<Map<String, Object>> tailSegments, final StringWriter stringWriter, final boolean isFirstSegment) {
        if (isFirstSegment) {
            stringWriter.write(SEQUENCE_XML_START_TAG);
        }
        stringWriter.write(MESSAGE_SEGMENT_MUSTACHE_PARTIAL);
        if (tailSegments.isEmpty() || !isEmpty((Collection) tailSegments.get(0).get("segments"))) {
            stringWriter.write(SEQUENCE_XML_END_TAG);
        }
    }

    private void writeSegmentGroup(final Map<String, Object> headSegment, final List<Map<String, Object>> tailSegments, final StringWriter stringWriter, final int segmentGroupIndex) {
        headSegment.put("xmltag", "SegGrp-" + segmentGroupIndex);
        stringWriter.write(MESSAGE_SEGMENT_GROUP_MUSTACHE_PARTIAL);
        if (!tailSegments.isEmpty() && isEmpty((Collection<?>) tailSegments.get(0).get("segments"))) {
            stringWriter.write(SEQUENCE_XML_START_TAG);
        }
    }

    private List<Map<String, Object>> tail(final List<Map<String, Object>> list) {
        if (list.isEmpty() || list.size() < 2) {
            return Collections.EMPTY_LIST;
        } else {
            return list.subList(1, list.size());
        }
    }

    private boolean isEmpty(final Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    @Override
    public String getName() {
        return "EDIFACT-Templates/EDIFACT-Messages.dfdl.xsd.mustache";
    }

    public List<String> getMessageTypes() {
        return messageTypes;
    }
}
