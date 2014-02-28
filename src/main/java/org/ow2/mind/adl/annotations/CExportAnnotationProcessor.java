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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Node;
import org.objectweb.fractal.adl.NodeFactory;
import org.objectweb.fractal.adl.interfaces.Interface;
import org.objectweb.fractal.adl.interfaces.InterfaceContainer;
import org.objectweb.fractal.adl.types.TypeInterface;
import org.ow2.mind.SourceFileWriter;
import org.ow2.mind.adl.annotation.ADLLoaderPhase;
import org.ow2.mind.adl.annotation.AbstractADLLoaderAnnotationProcessor;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.ast.Component;
import org.ow2.mind.adl.ast.ComponentContainer;
import org.ow2.mind.adl.ast.DefinitionReference;
import org.ow2.mind.adl.ast.ImplementationContainer;
import org.ow2.mind.adl.ast.MindInterface;
import org.ow2.mind.adl.ast.Source;
import org.ow2.mind.adl.idl.InterfaceDefinitionDecorationHelper;
import org.ow2.mind.annotation.Annotation;
import org.ow2.mind.annotation.AnnotationHelper;
import org.ow2.mind.idl.IDLLocator;
import org.ow2.mind.idl.ast.IDL;
import org.ow2.mind.idl.ast.InterfaceDefinition;
import org.ow2.mind.io.OutputFileLocator;

import com.google.inject.Inject;

/**
 * @author Julien TOUS
 */
public class CExportAnnotationProcessor
extends
AbstractADLLoaderAnnotationProcessor {

	@Inject
	protected OutputFileLocator   outFileLocator;

	@Inject
	protected IDLLocator   idlLocator;

	@Inject
	protected NodeFactory         nodeFactory;

	//String template that holds the declaration of the C symbols 
	protected static final String CEXPORTHEADER_TEMPLATE_NAME  = "st.CEXPORTHEADER";
	//String template that holds the source code of the to be created component
	protected static final String CEXPORTWRAPPER_TEMPLATE_NAME = "st.CEXPORTWRAPPER";

	private static int count = 0;
	/**
	 *	Create a singleton component in front (or behind) the annotated interface 
	 *  to expose it's method for plain C usage.
	 */
	public Definition processAnnotation(final Annotation annotation,
			final Node node, final Definition definition, final ADLLoaderPhase phase,
			final Map<Object, Object> context) throws ADLException {
		assert annotation instanceof CExport;
		if (node instanceof Component){
			final Component comp = (Component) node;
			final Definition compDef = ASTHelper.getResolvedComponentDefinition(comp,
					loaderItf, context);	
			String prefix;
			copyHeader("/mindcommon.h",context);

			if (phase == ADLLoaderPhase.ON_SUB_COMPONENT) {
				for (final Interface itf : ((InterfaceContainer) compDef).getInterfaces()) {
					final TypeInterface itfType = (TypeInterface) itf;
					final CExport thisAnnotation = AnnotationHelper.getAnnotation(itfType,
							CExport.class);


					//@CExport(prefix="prefix") is equivalent to @CExport("prefix")
					//@CExport(prefix="prefix",multiInstance=false) is equivalent to @CExport("prefix")
					//@CExport(prefix="prefix",multiInstance=true) is equivalent to @CExport("prefix",multiInstance=true)
					//@CExport("something",prefix="prefix",multiInstance=true) is equivalent to @CExport(prefix="prefix",multiInstance=true)

					if (thisAnnotation == annotation) {
						//Getting the annotation prefix as in @CExport(prefix="prefix")
						if (thisAnnotation.prefix == null){
							if (thisAnnotation.value == null)
								thisAnnotation.prefix = itf.getName();
							else 
								thisAnnotation.prefix = thisAnnotation.value;
						}
						if (thisAnnotation.multiInstance){
							prefix = thisAnnotation.prefix + "_inst" + count + "_";
						} else {
							prefix = thisAnnotation.prefix + "_";
						}
						if (itfType.getRole().equals(TypeInterface.SERVER_ROLE)) {
							insertProxyComp(prefix, itf, comp, definition, context);
						} else if (itfType.getRole().equals(TypeInterface.CLIENT_ROLE)) {
							insertStubComp(prefix, itf, comp, definition, context);
						} else {
							// Not client and not server ?
						}
						// remove annotation from node to avoid it to be reprocessed on a
						// definition that extends this one.
						AnnotationHelper.removeAnnotation(node, annotation);
						//Update the count
						count++;
						break;
					}

				}
			} else if (phase == ADLLoaderPhase.AFTER_EXTENDS) {
				final CExport thisAnnotation = AnnotationHelper.getAnnotation(comp,
						CExport.class);
				if (thisAnnotation == annotation) {
					for (final Interface itf : ((InterfaceContainer) compDef).getInterfaces()) {
						final TypeInterface itfType = (TypeInterface) itf;
						prefix = itf.getName() + "_";
						if (itfType.getRole().equals(TypeInterface.SERVER_ROLE)) {
							insertProxyComp(prefix, itf, comp, definition, context);
						} else if (itfType.getRole().equals(TypeInterface.CLIENT_ROLE)) {
							insertStubComp(prefix, itf, comp, definition, context);
						} else {
							// Not client and not server ?
						}
					}
				}
			}
		}
		return null;
	}

	private void copyHeader(String headerQualifiedName, Map<Object, Object> context) {
		URL url = idlLocator.findSourceHeader(headerQualifiedName,context);
		String mindCommonPath = url.getPath();
		File mindCommonFile = new File(mindCommonPath);
		File output = outFileLocator.getCSourceOutputFile(headerQualifiedName, context);
		if (mindCommonFile.exists()) {
			try {
				InputStream is = new FileInputStream(mindCommonFile);
				OutputStream os = new FileOutputStream(output);
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				is.close();
				os.close();
			} catch (IOException e) {
				System.err.println("IOException when copying " + mindCommonFile.getAbsolutePath() + " to " + output.getAbsolutePath());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Insert a singleton proxy component defining unmangled C symbols in front 
	 * of an interface to be exported
	 * 
	 * @param prefix A prefix to the interface methods for the exported C functions 
	 * @param itf The interface that will be exported as C functions
	 * @param comp The component that hold the exported interface 
	 * @param definition The definition of the component that contains comp
	 * @param context
	 */
	private void insertProxyComp(final String prefix, final Interface itf,
			final Component comp, final Definition definition,
			final Map<Object, Object> context) {

		//Name of the instance of the "to be created" proxy-component
		final String proxyCompInstName = prefix + "Comp";
		//Name of the client interface of the proxy-component 
		final String proxyCompCltInstName = prefix + "Clt";
		//Name of the definition of of the "to be created" proxy-component
		final String proxyCompName = "export." + prefix + "Comp";

		try {
			final TypeInterface itfType = (TypeInterface) itf;
			final InterfaceDefinition itfDef = itfSignatureResolverItf.resolve(itfType, definition, context);
			IDL idl = (IDL) itfDef;

			//Creating an interface with the same signature as the annotated one
			final MindInterface proxyClt = ASTHelper.newClientInterfaceNode(
					nodeFactory, proxyCompCltInstName, itfType.getSignature());
			final TypeInterface proxyCltType = proxyClt;
			InterfaceDefinitionDecorationHelper.setResolvedInterfaceDefinition(
					proxyCltType, itfDef);

			//Creating the definition
			final DefinitionReference proxyDefRef = ASTHelper.newDefinitionReference(
					nodeFactory, proxyCompName);
			final Definition proxyDef = ASTHelper.newPrimitiveDefinitionNode(
					nodeFactory, proxyCompName, proxyDefRef);
			//The definition must be singleton to be able to mix with C
			AnnotationHelper.addAnnotation(proxyDef,
					new org.ow2.mind.adl.annotation.predefined.Singleton());
			ASTHelper.setSingletonDecoration(proxyDef);
			//Adding the newly created interface to our created definition
			((InterfaceContainer) proxyDef).addInterface(proxyClt);

			//Creating C header to be used by legacy application
			final StringTemplate headerST = getTemplate(CEXPORTHEADER_TEMPLATE_NAME,
					"ProvidedSymbolsDeclaration");
			headerST.setAttribute("interfaceDefinition", itfDef);
			headerST.setAttribute("idl", idl);
			headerST.setAttribute("prefix", prefix);
			headerST.setAttribute("itfName", proxyCompCltInstName);
			//The file is located in the "output directory".
			final File headerFile = outFileLocator.getCSourceOutputFile("/" + prefix
					+ "export.h", context);
			if (headerFile.exists()) headerFile.delete();
			try {
				SourceFileWriter.writeToFile(headerFile, headerST.toString());
			} catch (final IOException e) {
				System.err.println("IOException on file " + headerFile.getAbsolutePath());
				e.printStackTrace();
			}

			//Creating the source code for our proxy-component
			final StringBuilder srcCode = new StringBuilder();
			final StringTemplate wrapperST = getTemplate(
					CEXPORTWRAPPER_TEMPLATE_NAME, "WrapperServerDefinition");
			wrapperST.setAttribute("interfaceDefinition", itfDef);
			wrapperST.setAttribute("prefix", prefix);
			wrapperST.setAttribute("itfName", proxyCompCltInstName);
			srcCode.append(wrapperST.toString());
			//Adding the source to the definition of our proxy-component
			final Source src = ASTHelper.newSource(nodeFactory);
			((ImplementationContainer) proxyDef).addSource(src);
			src.setCCode(srcCode.toString());

			//Instantiating a the proxy-component 
			final Component proxyComp = ASTHelper.newComponent(nodeFactory,
					proxyCompInstName, proxyDefRef);
			ASTHelper.setResolvedComponentDefinition(proxyComp, proxyDef);
			//Adding our proxy-component instance into the upper composite
			((ComponentContainer) definition).addComponent(proxyComp);

			//Creating and configuring a binding between our new proxy-component instance and the annotated interface
			final Binding binding = ASTHelper.newBinding(nodeFactory);
			binding.setFromComponent(proxyComp.getName());
			binding.setToComponent(comp.getName());
			binding.setFromInterface(proxyClt.getName());
			binding.setToInterface(itf.getName());
			//Adding the binding to the upper composite
			((BindingContainer) definition).addBinding(binding);

		} catch (final ADLException e) {
			System.err.println("ADLException when creating @CExport wrapper component");
			e.printStackTrace();
		}
	}

	/**
	 * Insert a singleton stub component requiring external unmangled C symbols  
	 * behind of an interface to be exported
	 * 
	 * @param prefix A prefix to the interface methods for the required C functions 
	 * @param itf The interface that will be exported as C functions
	 * @param comp The component that hold the exported interface 
	 * @param definition The definition of the component that contains comp
	 * @param context
	 */
	private void insertStubComp(final String prefix, final Interface itf,
			final Component comp, final Definition definition,
			final Map<Object, Object> context) {

		//Name of the instance of the "to be created" stub-component
		final String stubCompInstName = prefix + "Comp";
		//Name of the client interface of the stub-component 
		final String stubCompSrvInstName = prefix + "Srv";
		//Name of the definition of of the "to be created" stub-component
		final String stubCompName = "export." + prefix + "Comp";

		try {
			final TypeInterface itfType = (TypeInterface) itf;
			final InterfaceDefinition itfDef = itfSignatureResolverItf.resolve(itfType, definition, context);
			IDL idl = (IDL) itfDef;

			//Creating an interface with the same signature as the annotated one
			final MindInterface stubSrv = ASTHelper.newServerInterfaceNode(
					nodeFactory, stubCompSrvInstName, itfType.getSignature());
			final TypeInterface stubSrvType = stubSrv;
			InterfaceDefinitionDecorationHelper.setResolvedInterfaceDefinition(
					stubSrvType, itfDef);

			//Creating the definition
			final DefinitionReference stubDefRef = ASTHelper.newDefinitionReference(
					nodeFactory, stubCompName);
			final Definition stubDef = ASTHelper.newPrimitiveDefinitionNode(
					nodeFactory, stubCompName, stubDefRef);
			//The definition must be singleton to be able to mix with C
			AnnotationHelper.addAnnotation(stubDef,
					new org.ow2.mind.adl.annotation.predefined.Singleton());
			ASTHelper.setSingletonDecoration(stubDef);
			//Adding the newly created interface to our created definition
			((InterfaceContainer) stubDef).addInterface(stubSrv);

			//Creating C header to be used by legacy application
			final StringTemplate headerST = getTemplate(CEXPORTHEADER_TEMPLATE_NAME,
					"RequiredSymbolsDeclaration");
			headerST.setAttribute("interfaceDefinition", itfDef);
			headerST.setAttribute("idl", idl);
			headerST.setAttribute("prefix", prefix);
			headerST.setAttribute("itfName", stubCompSrvInstName);
			//The file is located in the "output directory".
			final File headerFile = outFileLocator.getCSourceOutputFile("/" + prefix
					+ "export.h", context);
			if (headerFile.exists()) headerFile.delete();
			try {
				SourceFileWriter.writeToFile(headerFile, headerST.toString());
			} catch (final IOException e) {
				System.err.println("IOException on file " + headerFile.getAbsolutePath());
				e.printStackTrace();
			}

			//Creating the source code for our stub-component
			final StringBuilder srcCode = new StringBuilder();
			final StringTemplate wrapperST = getTemplate(
					CEXPORTWRAPPER_TEMPLATE_NAME, "WrapperClientDefinition");
			wrapperST.setAttribute("interfaceDefinition", itfDef);
			wrapperST.setAttribute("prefix", prefix);
			wrapperST.setAttribute("itfName", stubCompSrvInstName);
			srcCode.append(wrapperST.toString());
			final Source src = ASTHelper.newSource(nodeFactory);
			//Adding the source to the definition of our stub-component
			((ImplementationContainer) stubDef).addSource(src);
			src.setCCode(srcCode.toString());

			//Instantiating a the stub-component 
			final Component proxyComp = ASTHelper.newComponent(nodeFactory,
					stubCompInstName, stubDefRef);
			ASTHelper.setResolvedComponentDefinition(proxyComp, stubDef);
			//Adding our proxy-component instance into the upper composite
			((ComponentContainer) definition).addComponent(proxyComp);

			//Creating and configuring a binding between the annotated interface and our new stub-component instance 
			final Binding binding = ASTHelper.newBinding(nodeFactory);
			binding.setToComponent(proxyComp.getName());
			binding.setFromComponent(comp.getName());
			binding.setToInterface(stubSrv.getName());
			binding.setFromInterface(itf.getName());
			//Adding the binding to the upper composite
			((BindingContainer) definition).addBinding(binding);

		} catch (final ADLException e) {
			// 
			e.printStackTrace();
		}
	}
}