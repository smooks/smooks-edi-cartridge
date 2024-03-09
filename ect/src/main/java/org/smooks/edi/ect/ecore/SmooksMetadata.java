/*-
 * ========================LICENSE_START=================================
 * smooks-ect
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
package org.smooks.edi.ect.ecore;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EModelElement;

/**
 * Interface to access Smooks-related EMF Annotations
 *
 * @author zubairov
 */
public interface SmooksMetadata {

    public static final String ANNOTATION_TYPE = "smooks-mapping-data";
    public static final String SEGMENT_TYPE = "segment";
    public static final String SEGMENT_GROUP_TYPE = "group";
    public static final String FIELD_TYPE = "field";
    public static final String COMPONENT_TYPE = "component";
    public static final String ANNOTATION_TYPE_KEY = "type";
    public static final String SEGCODE = "segcode";

    /**
     * Returns {@link EAnnotation} or throws {@link IllegalArgumentException}
     *
     * @param element
     * @return
     */
    public EAnnotation getSmooksAnnotation(EModelElement element);

    /**
     * Returns true if given {@link EModelElement} annotated as segment
     *
     * @param element
     * @return
     */
    public boolean isSegment(EModelElement element);

    /**
     * Return segcode or throws {@link IllegalArgumentException}
     *
     * @param feature
     * @return
     */
    public String getSegcode(EModelElement element);

    /**
     * Returns true if given {@link EModelElement} has annotation type group
     *
     * @param feature
     * @return
     */
    public boolean isSegmentGroup(EModelElement element);

    /**
     * Returns true or false or throws {@link IllegalArgumentException}
     *
     * @param element
     * @return
     */
    public boolean isField(EModelElement element);

    /**
     * Returns true of false or throws {@link IllegalArgumentException}
     *
     * @param feature
     * @return
     */
    public boolean isComponent(EModelElement feature);

    /**
     * SINGLETON instance
     */
    public static final SmooksMetadata INSTANCE = new SmooksMetadata() {

        /**
         * {@inheritDoc}
         */
        public boolean isSegment(EModelElement element) {
            EAnnotation annotation = getSmooksAnnotation(element);
            if (annotation == null) {
                return false;
            }
            return SEGMENT_TYPE.equals(annotation.getDetails().get(
                    ANNOTATION_TYPE_KEY));
        }

        /**
         * {@inheritDoc}
         */
        public EAnnotation getSmooksAnnotation(EModelElement element)
                throws IllegalArgumentException {
            EAnnotation annotation = element.getEAnnotation(ANNOTATION_TYPE);
            return annotation;
        }

        /**
         * {@inheritDoc}
         */
        public String getSegcode(EModelElement element)
                throws IllegalArgumentException {
            EAnnotation annotation = getSmooksAnnotation(element);
            if (annotation == null) {
                return null;
            }
            return annotation.getDetails().get(SEGCODE);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isSegmentGroup(EModelElement element) {
            EAnnotation annotation = getSmooksAnnotation(element);
            if (annotation == null) {
                return false;
            }
            return SEGMENT_GROUP_TYPE.equals(annotation.getDetails().get(
                    ANNOTATION_TYPE_KEY));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isField(EModelElement element)
                throws IllegalArgumentException {
            EAnnotation annotation = element.getEAnnotation(ANNOTATION_TYPE);
            if (annotation == null) {
                return false;
            }
            return FIELD_TYPE.equals(annotation.getDetails().get(
                    ANNOTATION_TYPE_KEY));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isComponent(EModelElement element) {
            EAnnotation annotation = element.getEAnnotation(ANNOTATION_TYPE);
            if (annotation == null) {
                return false;
            }
            return COMPONENT_TYPE.equals(annotation.getDetails().get(
                    ANNOTATION_TYPE_KEY));
        }

    };

}
