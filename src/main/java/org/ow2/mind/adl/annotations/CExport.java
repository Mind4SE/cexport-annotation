
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
