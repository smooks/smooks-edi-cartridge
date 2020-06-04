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

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.smooks.edi.edisax.interchange.EdiDirectory;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.model.internal.Field;
import org.smooks.edi.edisax.model.internal.Segment;
import org.smooks.edi.edisax.model.internal.SegmentGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class is responsible for generating ECore model based on the UN EDI
 * Model
 * 
 * @author zubairov
 * 
 */
public class ECoreGenerator {

	public static final ECoreGenerator INSTANCE = new ECoreGenerator();

	public static final String COMMON_PACKAGE_NAME = "common";

	private static final Logger LOGGER = LoggerFactory.getLogger(ECoreGenerator.class);

	/**
	 * This method will convert information available in {@link EdiDirectory}
	 * into the set of {@link EPackage} packages.
	 * 
	 * Set will contain one package with common definitions and one package per
	 * each {@link Edimap} that is using common classes
	 * 
	 * @param ediDirectory
	 *            The EdiDirectory.
	 * @return The EPackages.
	 */
	public Set<EPackage> generatePackages(EdiDirectory ediDirectory) {
		LOGGER.debug("Converting UN EDIFACT Model");
		Set<EPackage> result = new HashSet<EPackage>();

		// Creating common package
		Edimap commonModel = ediDirectory.getCommonModel();
		Map<String, EClass> commonClasses = new HashMap<String, EClass>();
		EPackage commonPackage = EcoreFactory.eINSTANCE.createEPackage();
		commonPackage.setName(COMMON_PACKAGE_NAME);
		commonPackage.setNsPrefix("common");
		commonPackage.setNsURI(commonModel.getDescription().getNamespace());
		Collection<EClass> clzz = createCommonClasses(commonModel,
				commonClasses);
		commonPackage.getEClassifiers().addAll(clzz);
		result.add(commonPackage);

		// Processing individual packages
		for (Edimap mappingModel : ediDirectory.getMessageModels()) {
			EPackage pkg = processPackage(mappingModel, commonClasses);
			if (!result.add(pkg)) {
				LOGGER.warn("WARN: Duplicated package " + pkg.getName() + " for ");
			}
		}
		LOGGER.debug("Converted EDIFACT Model  into " + result.size()
				+ " EPackages");
		return result;
	}

	/**
	 * Generate a single {@link EPackage}. This method assumes that given
	 * {@link Edimap} model does not depend/import any other {@link Edimap}
	 * model
	 * 
	 * @param mappingModel
	 * @return
	 */
	public EPackage generateSinglePackage(Edimap mappingModel) {
		HashMap<String, EClass> commonClasses = new HashMap<String, EClass>();
		EPackage result = processPackage(mappingModel, commonClasses);
		result.getEClassifiers().addAll(commonClasses.values());
		return result;
	}

	private EPackage processPackage(Edimap mappingModel,
			Map<String, EClass> commonClasses) {
		EPackage pkg = ECoreConversionUtils
				.mappingModelToEPackage(mappingModel);
		pkg.getEClassifiers().addAll(
				createMappingClases(mappingModel.getSegments(), commonClasses));
		return pkg;
	}

	/**
	 * Creating mapping classes
	 * 
	 * @param root
	 * @param commonClasses
	 * @return
	 */
	private Set<EClass> createMappingClases(SegmentGroup root,
			Map<String, EClass> commonClasses) {
		Set<EClass> result = new HashSet<EClass>();
		EClass rootClass = ECoreConversionUtils.segmentGroupToEClass(root);
		// We need to change the name of the Root class so it is not
		// the same as name of the package
		rootClass.setName(rootClass.getName().toUpperCase());
		result.add(rootClass);
		ExtendedMetaData.INSTANCE.setName(rootClass, rootClass.getName());
		result.add(ECoreConversionUtils.createDocumentRoot(rootClass));
		processSegments(root.getSegments(), commonClasses, result, rootClass);
		return result;
	}

	/**
	 * Process segments
	 * 
	 * @param segments
	 * @param commonClasses
	 * @param result
	 * @param rootClass
	 */
	private void processSegments(List<SegmentGroup> segments,
			final Map<String, EClass> commonClasses, final Set<EClass> result,
			final EClass parent) {
		for (SegmentGroup arg0 : segments) {
			if (arg0 instanceof Segment) {
				Segment segment = (Segment) arg0;
				EClass clazz = null;
				if (segment.getNodeTypeRef() == null) {
					// Segment without reference
					clazz = ECoreConversionUtils.segmentToEClass(segment);
					if (!segment.getFields().isEmpty()) {
						commonClasses.put(segment.getSegcode(), clazz);
						Collection<EStructuralFeature> fields = processFields(
								segment.getFields(), commonClasses);
						clazz.getEStructuralFeatures().addAll(fields);
					}
					result.add(clazz);
				} else {
					// Segment with reference
					clazz = commonClasses.get(getLocalPart(segment));
				}
				EReference segmentRef = ECoreConversionUtils
						.segmentToEReference(segment, clazz);
				if (parent.getEStructuralFeature(segmentRef.getName()) == null) {
					parent.getEStructuralFeatures().add(segmentRef);
				} else {
					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn("Duplicate segment " + segmentRef.getName()
								+ " (tag: "
								+ ExtendedMetaData.INSTANCE.getName(segmentRef)
								+ ")" + " in " + parent.getName());
					}
				}
			} else if (arg0 instanceof SegmentGroup) {
				SegmentGroup grp = (SegmentGroup) arg0;
				EClass refClass = ECoreConversionUtils
						.segmentGroupToEClass(grp);
				EReference reference = ECoreConversionUtils
						.segmentGroupToEReference(grp, refClass);
				if (parent.getEStructuralFeature(reference.getName()) == null) {
					parent.getEStructuralFeatures().add(reference);
				}
				if (!result.add(refClass)) {
					throw new RuntimeException("Reference class "
							+ refClass.getName() + " is duplicated in package");
				}
				processSegments(grp.getSegments(), commonClasses, result,
						refClass);
			}
		}
	}

	/**
	 * This method converting classes for common mapping model
	 * 
	 * @param commonModel
	 * @param commonClasses
	 * @param commonPackage
	 */
	private Collection<EClass> createCommonClasses(Edimap commonModel,
			final Map<String, EClass> commonClasses) {
		Map<String, EClass> result = new HashMap<String, EClass>();
		for (SegmentGroup grp : commonModel.getSegments().getSegments()) {
			// No segment groups are allowed in common part
			Segment segment = (Segment) grp;
			EClass clazz = ECoreConversionUtils.segmentToEClass(segment);
			if (!segment.getFields().isEmpty()) {
				commonClasses.put(segment.getSegcode(), clazz);
				Collection<EStructuralFeature> fields = processFields(
						segment.getFields(), result);
				clazz.getEStructuralFeatures().addAll(fields);
			}
			result.put(clazz.getName(), clazz);
		}
		// Adding DocumentRoot
		EClass droot = ECoreConversionUtils.createDocumentRoot(null);
		result.put(droot.getName(), droot);
		return result.values();
	}

	/**
	 * Here we transform {@link Field} to {@link EStructuralFeature} which is
	 * either {@link EAttribute} or {@link EReference}
	 * 
	 * In case of {@link EReference} we would need to add a new {@link EClass}
	 * to the result EClass set
	 * 
	 * @param fields
	 * @param result
	 */
	private Collection<EStructuralFeature> processFields(List<Field> fields,
			Map<String, EClass> classes) {
		// We need to preserve order therefore we are going
		// to use separate list and set for controlling duplicates
		List<EStructuralFeature> result = new ArrayList<EStructuralFeature>();
		Set<String> names = new HashSet<String>();
		for (Field field : fields) {
			if (field.getComponents().isEmpty()) {
				// We have a simple field without components
				EAttribute attribute = ECoreConversionUtils
						.fieldToEAttribute(field);
				if (!names.contains(attribute.getName())) {
					result.add(attribute);
					names.add(attribute.getName());
				}
			} else {
				// We have a complex field --> need to define a new
				// class
				EReference reference = ECoreConversionUtils.fieldToEReference(
						field, classes);
				if (!names.contains(reference.getName())) {
					result.add(reference);
					names.add(reference.getName());
				}
			}
		}
		return result;
	}

	/**
	 * Just cut out a local part from the fully qualified name
	 * 
	 * @param segment
	 * @return
	 */
	private String getLocalPart(Segment segment) {
		// TODO Fix this hack
		String ref = segment.getNodeTypeRef();
		return ref.substring(ref.indexOf(":") + 1);
	}
}
