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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.TemplateFunction;
import org.smooks.edi.edisax.model.internal.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SegmentsTemplate extends Template {
    private static final String SEGMENT_PART_REPEAT_MUSTACHE_PARTIAL = "{{> EDISegmentPartRepeatSequenceFormat.xsd.mustache}}";
    private static final String SEGMENT_MUSTACHE_PARTIAL = "{{> EDISegmentSequenceFormat.xsd.mustache}}\n";

    public SegmentsTemplate(final String version, final Edimap edimap) {
        super(version);
        final List<Map<String, Object>> segments = prepareSegments(edimap.getSegments().getSegments());
        final List<Map<String, Object>> compositeDataElements = prepareCompositeDataElements(edimap.getCompositeDataElements());
        final List<Map<String, Object>> simpleDataElements = prepareSimpleDataElements(edimap.getSimpleDataElements());

        Map<String, Map<String, String>> alphaTypes = new HashMap<>();
        Map<String, Map<String, String>> numericTypes = new HashMap<>();

        simpleDataElements.forEach(de -> de.put("base", (Function<String, String>) s -> {
            String base;
            if (de.get("dataType").equals("String")) {
                base = "alpha" + de.get("minLength") + "-" + de.get("maxLength");
                alphaTypes.put(base, new HashMap<String, String>() {{
                    this.put("minLength", String.valueOf(de.get("minLength")));
                    this.put("maxLength", String.valueOf(de.get("maxLength")));
                }});
            } else if (de.get("dataType").equals("DABigDecimal")) {
                base = "numeric";
                String textNumberPattern;
                if (de.get("minLength") == de.get("maxLength")) {
                    base += de.get("maxLength");
                    textNumberPattern = "0000";
                } else {
                    base += "1-" + de.get("maxLength");
                    textNumberPattern = "";
                }
                numericTypes.put(base, new HashMap<String, String>() {{
                    this.put("totalDigits", String.valueOf(de.get("maxLength")));
                    this.put("textNumberPattern", textNumberPattern);
                }});
            } else {
                throw new UnsupportedOperationException("Unknown data type: " + de.get("dataType"));
            }

            return base;
        }));

        scope.put("segments", segments);
        scope.put("compositeDataElements", compositeDataElements);
        scope.put("dataElements", simpleDataElements);
        scope.put("alphaTypes", alphaTypes.entrySet());
        scope.put("numericTypes", numericTypes.entrySet());
    }

    private List<Map<String, Object>> prepareSegments(final List<SegmentGroup> segments) {
        final List<SegmentGroup> undefinedSegments = segments.stream().filter(s -> !s.getSegcode().equals("UGH") && !s.getSegcode().equals("UGT") && !s.getSegcode().equals("UNS")).collect(Collectors.toList());
        undefinedSegments.forEach(s -> ((Segment) s).getFields().forEach(f -> f.setXmltag(f.getXmltag().toUpperCase())));

        final List<Map<String, Object>> undefinedSegmentsAsMap = OBJECT_MAPPER.convertValue(undefinedSegments, new TypeReference<List<Map<String, Object>>>() {
        });
        undefinedSegmentsAsMap.forEach(s -> ((List<Map<String, Object>>) s.get("fields")).forEach(f -> f.put("minOccurs", (boolean) f.get("required") ? 1 : 0)));
        reduceFields(undefinedSegmentsAsMap);

        return undefinedSegmentsAsMap;
    }

    private List<Map<String, Object>> prepareCompositeDataElements(final List<Field> compositeDataElements) {
        compositeDataElements.forEach(f -> f.getComponents().forEach(c -> {
            c.setXmltag(c.getXmltag().toUpperCase());
        }));
        final List<Map<String, Object>> compositeDataElementsAsMap = OBJECT_MAPPER.convertValue(compositeDataElements, new TypeReference<List<Map<String, Object>>>() {
        });
        compositeDataElementsAsMap.forEach(s -> ((List<Map<String, Object>>) s.get("components")).forEach(f -> f.put("minOccurs", (boolean) f.get("required") ? 1 : 0)));

        reduceComponents(compositeDataElementsAsMap);

        return compositeDataElementsAsMap;
    }

    private List<Map<String, Object>> prepareSimpleDataElements(final List<Component> simpleDataElements) {
        simpleDataElements.forEach(de -> {
            de.setXmltag(de.getXmltag().toUpperCase());
        });

        return OBJECT_MAPPER.convertValue(simpleDataElements, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private void reduceComponents(final List<Map<String, Object>> compositeDataElements) {
        for (Map<String, Object> compositeDataElement : compositeDataElements) {
            final List<Map<String, Object>> components = (List<Map<String, Object>>) compositeDataElement.get("components");
            if (!components.isEmpty()) {
                compositeDataElement.put("components", reduceComponents(components.get(0), components.size() > 1 ? components.subList(1, components.size()) : Collections.EMPTY_LIST));
            }
        }
    }

    private List<Map<String, Object>> reduceComponents(Map<String, Object> componentHead, List<Map<String, Object>> componentsTail) {
        final String xmlTagComponentHead = (String) componentHead.get("xmltag");
        final AtomicInteger maxOccurs = new AtomicInteger(1);
        final List<Map<String, Object>> components = new ArrayList<>();
        components.add(componentHead);
        if (!componentsTail.isEmpty()) {
            for (Map<String, Object> nextComponent : componentsTail) {
                final String xmlTagNextComponent = (String) nextComponent.get("xmltag");
                if (xmlTagComponentHead.equals(xmlTagNextComponent)) {
                    maxOccurs.getAndIncrement();
                } else {
                    break;
                }
            }

            if (componentsTail.size() > (maxOccurs.get() - 1)) {
                final Map<String, Object> newComponentHead = componentsTail.get(maxOccurs.get() - 1);
                final List<Map<String, Object>> newComponentsTail;
                if (componentsTail.size() > maxOccurs.get()) {
                    newComponentsTail = componentsTail.subList(maxOccurs.get(), componentsTail.size());
                } else {
                    newComponentsTail = Collections.EMPTY_LIST;
                }
                final List<Map<String, Object>> reducedTailComponents = reduceComponents(newComponentHead, newComponentsTail);
                for (Map<String, Object> nextComponent : reducedTailComponents) {
                    final String xmlTagNextComponent = (String) nextComponent.get("xmltag");
                    if (xmlTagComponentHead.equals(xmlTagNextComponent)) {
                        final char nextLetter;
                        if (Character.isLetter(xmlTagComponentHead.charAt(xmlTagComponentHead.length() - 1))) {
                            nextLetter = ((char) (xmlTagComponentHead.charAt(xmlTagComponentHead.length() - 1) + 1));
                        } else {
                            componentHead.put("xmltag", xmlTagComponentHead + "a");
                            nextLetter = 'b';
                        }
                        nextComponent.put("xmltag", xmlTagNextComponent + nextLetter);
                    }
                }

                components.addAll(reducedTailComponents);
            }
        }

        componentHead.put("maxOccurs", maxOccurs.get());
        return components;
    }

    private void reduceFields(final List<Map<String, Object>> segments) {
        for (Map<String, Object> segment : segments) {
            final List<Map<String, Object>> fields = (List<Map<String, Object>>) segment.get("fields");
            segment.put("fields", reduceFields(fields.get(0), fields.size() > 1 ? fields.subList(1, fields.size()) : Collections.EMPTY_LIST));
        }
    }

    private List<Map<String, Object>> reduceFields(Map<String, Object> fieldHead, List<Map<String, Object>> fieldsTail) {
        final AtomicInteger repetitions = new AtomicInteger(0);
        final List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(fieldHead);
        if (!fieldsTail.isEmpty()) {
            for (Map<String, Object> nextField : fieldsTail) {
                if (fieldHead.get("xmltag").equals(nextField.get("xmltag"))) {
                    repetitions.getAndIncrement();
                } else {
                    break;
                }
            }
            if (fieldsTail.size() > repetitions.get()) {
                final Map<String, Object> newFieldHead = fieldsTail.get(repetitions.get());
                final List<Map<String, Object>> newFieldsTail;
                if (fieldsTail.size() > repetitions.get() + 1) {
                    newFieldsTail = fieldsTail.subList(repetitions.get() + 1, fieldsTail.size());
                } else {
                    newFieldsTail = Collections.EMPTY_LIST;
                }
                fields.addAll(reduceFields(newFieldHead, newFieldsTail));
            }
        }

        if (repetitions.get() > 0) {
            fieldHead.put("maxOccurs", repetitions.get());
        } else {
            fieldHead.put("maxOccurs", fieldHead.get("cardinality"));
        }
        fieldHead.put("render", (TemplateFunction) s -> {
            if (repetitions.get() > 0 || ((int) fieldHead.get("cardinality")) > 1) {
                return SEGMENT_PART_REPEAT_MUSTACHE_PARTIAL;
            } else {
                return SEGMENT_MUSTACHE_PARTIAL;
            }
        });

        return fields;
    }

    @Override
    public String getName() {
        return "EDIFACT-Templates/EDIFACT-Segments.dfdl.xsd.mustache";
    }
}
