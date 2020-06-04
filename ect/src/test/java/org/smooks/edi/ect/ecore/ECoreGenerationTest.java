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

import static org.junit.jupiter.api.Assertions.*;
import static org.smooks.edi.ect.ecore.ECoreConversionUtils.toJavaName;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.junit.jupiter.api.Test;
import org.smooks.edi.ect.ecore.ECoreGenerator;
import org.smooks.edi.ect.formats.unedifact.UnEdifactSpecificationReader;

public class ECoreGenerationTest {

    private static final ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;

    @Test
    public void testECoreGeneration() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/D99A.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        UnEdifactSpecificationReader ediSpecificationReader = new UnEdifactSpecificationReader(
                zipInputStream, false, false);
        ECoreGenerator generator = new ECoreGenerator();
        Set<EPackage> packages = generator
                .generatePackages(ediSpecificationReader.getEdiDirectory());
        for (EPackage pkg : packages) {
            validatePackage(pkg);
            if ("cuscar".equals(pkg.getName())) {
                checkCUSCAR(pkg);
            }
            if ("common".equals(pkg.getName())) {
                assertEquals("urn:org.smooks.edi.unedifact:un:d99a:common", pkg.getNsURI(), "Common namespace don't match");
            }
        }
    }

    private void checkCUSCAR(EPackage pkg) {
        assertEquals("urn:org.smooks.edi.unedifact:un:d99a:cuscar", pkg.getNsURI(), "Namespace don't match");
        EClass clazz = (EClass) pkg.getEClassifier("CUSCAR");
        assertNotNull(clazz);
        assertEquals(13, clazz.getEStructuralFeatures().size());
        assertEquals(13, clazz.getEAllContainments().size());
        assertEquals("CUSCAR", metadata.getName(clazz));
    }

    private void validatePackage(EPackage pkg) {
        assertNotNull(metadata.getDocumentRoot(pkg), pkg.getName() + " has document root");
        EList<EClassifier> classifiers = pkg.getEClassifiers();
        Set<String> names = new HashSet<String>();
        for (EClassifier classifier : classifiers) {
            if (classifier instanceof EClass) {
                EClass clazz = (EClass) classifier;
                String location = pkg.getName() + "#" + clazz.getName();
                if (!"DocumentRoot".equals(clazz.getName())) {
                    String metadataName = metadata.getName(clazz);
                    boolean same = clazz.getName().equals(metadataName)
                            || clazz.getName().equals(
                            toJavaName(metadataName, true));
                    assertTrue(same,
                            location + " metadata missmatch " + clazz.getName()
                                    + "<>" + metadataName);
                    assertTrue(names.add(clazz.getName()), location + " duplicate");
                }
            }
        }
    }

    @Test
    public void testMissingSegmentNames() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/d96b.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        UnEdifactSpecificationReader ediSpecificationReader = new UnEdifactSpecificationReader(
                zipInputStream, false);
        ECoreGenerator generator = new ECoreGenerator();
        Set<EPackage> packages = generator
                .generatePackages(ediSpecificationReader.getEdiDirectory());
        boolean found = false;
        for (EPackage pkg : packages) {
            validatePackage(pkg);
            if ("cusdec".equals(pkg.getName())) {
                checkCUSDEC(pkg);
                found = true;
            }
        }
        assertTrue(found, "Can't find cusdec package");
    }

    private void checkCUSDEC(EPackage pkg) {
        EClass root = (EClass) pkg.getEClassifier("CUSDEC");
        assertNotNull(root);
        assertEquals(23, root.getEStructuralFeatures().size());
        assertNotNull(root.getEStructuralFeature("UNS1"));
        assertNotNull(root.getEStructuralFeature("UNS2"));
        assertNull(root.getEStructuralFeature("UNS"));
    }

}
