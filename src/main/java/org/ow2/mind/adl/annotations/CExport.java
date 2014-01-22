/**
 * Copyright (C) 2014 Schneider-Electric
 *
 * This file is part of "Mind Compiler" is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact: mind@ow2.org
 *
 * Authors: Julien Tous
 * Contributors: 
 */
package org.ow2.mind.adl.annotations;

import org.ow2.mind.adl.annotation.ADLAnnotationTarget;
import org.ow2.mind.adl.annotation.ADLLoaderPhase;
import org.ow2.mind.adl.annotation.ADLLoaderProcessor;
import org.ow2.mind.annotation.Annotation;
import org.ow2.mind.annotation.AnnotationElement;
import org.ow2.mind.annotation.AnnotationTarget;

/**
 * The CExport annotation can be associate to a server interface to specify that
 * pure C stub should be created for this interface whith associated header
 * 
 * @author Julien TOUS
 */
@ADLLoaderProcessor(processor = CExportAnnotationProcessor.class, phases = {ADLLoaderPhase.ON_SUB_COMPONENT,ADLLoaderPhase.AFTER_EXTENDS})
public class CExport implements Annotation {


	private static final long serialVersionUID = -915721749890534630L;
	private static final AnnotationTarget[] ANNOTATION_TARGETS = {ADLAnnotationTarget.INTERFACE,ADLAnnotationTarget.COMPONENT};

	public AnnotationTarget[] getAnnotationTargets() {
		return ANNOTATION_TARGETS;
	}

	@AnnotationElement(hasDefaultValue=true)
	public String value=null;
	
	@AnnotationElement(hasDefaultValue=true)
	public String prefix=null;

	@AnnotationElement(hasDefaultValue=true)
	public boolean multiInstance=false;

	public boolean isInherited() {
		return false;
	}

}
