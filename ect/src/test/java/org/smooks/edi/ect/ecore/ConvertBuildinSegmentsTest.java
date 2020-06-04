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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.smooks.edi.ect.ecore.SmooksMetadata.INSTANCE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.Test;
import org.smooks.edi.edisax.model.EDIConfigDigester;
import org.smooks.edi.edisax.model.internal.Edimap;

/**
 * Test case for conversion of build-in segment definitions
 *
 * @author zubairov
 */
public class ConvertBuildinSegmentsTest {

    private static final ExtendedMetaData METADATA = ExtendedMetaData.INSTANCE;

    @Test
    public void testConversion() throws Exception {
        InputStream is = ConvertBuildinSegmentsTest.class.getResourceAsStream("/org/smooks/edi/edisax/unedifact/handlers/r41/v41-segments.xml");
        assertNotNull(is, "Can't find a v41-segments.xml");
        Edimap edimap = EDIConfigDigester.digestConfig(is);
        EPackage pkg = ECoreGenerator.INSTANCE.generateSinglePackage(edimap);
        assertEquals("urn:org.smooks.edi.unedifact.v41", pkg.getNsURI());
        assertEquals("unedifact", pkg.getNsPrefix());
        assertEquals(21, pkg.getEClassifiers().size());
        List<String> codz = new ArrayList<String>();
        for (EClassifier clazz : pkg.getEClassifiers()) {
            if (SmooksMetadata.INSTANCE.isSegment(clazz)) {
                codz.add(INSTANCE.getSegcode(clazz));

            }
        }
        Collections.sort(codz);
        assertEquals("[UNB, UNE, UNG, UNH, UNT, UNZ]", codz.toString());
        // Now we need to do a trick with Document Root
        EClass docRoot = METADATA.getDocumentRoot(pkg);
        assertEquals(1, docRoot.getEStructuralFeatures().size());
        // Fix name of the root element
        EStructuralFeature feature = docRoot.getEAllStructuralFeatures().get(0);
        assertNotNull(feature, "Can't find feature of DocumentRoot");
        feature.setName("unEdifact");
        EReference ref = (EReference) docRoot.getEStructuralFeatures().get(0);
        EClassifier rootElementType = pkg.getEClassifier(ref.getEReferenceType().getName());
        METADATA.setName(rootElementType, "unEdifact");
        SchemaConverter.INSTANCE.convertEDIMap(pkg, new FileOutputStream(new File("./target/v41-segments.xsd")));
        saveECORE(pkg);
    }

    private void saveECORE(EPackage pkg) throws IOException {
        ResourceSet resourceSet = new ResourceSetImpl();
        /*
         * Register XML Factory implementation using DEFAULT_EXTENSION
         */
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());

        Resource resource = resourceSet.createResource(URI.createURI("buildin.ecore"));
        resource.getContents().add(pkg);
        resource.save(new FileOutputStream(new File("./target/buildin.ecore")), null);
    }


}
