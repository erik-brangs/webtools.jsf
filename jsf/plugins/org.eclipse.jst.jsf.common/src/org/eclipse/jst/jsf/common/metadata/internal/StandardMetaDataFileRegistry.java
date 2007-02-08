/*******************************************************************************
 * Copyright (c) 2002, 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *   Jens Lukowski/Innoopract - initial renaming/restructuring
 * 	 Gerry Kessler/Oracle - code borrowed and repurposed for JSF subproject
 *
 *******************************************************************************/
package org.eclipse.jst.jsf.common.metadata.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jst.jsf.common.JSFCommonPlugin;
import org.eclipse.jst.jsf.common.metadata.internal.provisional.Model;


/**
 * Registry of standard metadata files
 */
public final class StandardMetaDataFileRegistry {
	private Map/*<String, List<IMetaDataSourceModelProvider>>*/ mdFilesMap 	= new HashMap/*<String, List<IMetaDataSourceModelProvider>>*/(1);
	private List/*<IMetaDataSourceModelProvider>*/ EMPTY_LIST 	= new ArrayList/*<IMetaDataSourceModelProvider>*/(0);

	private static StandardMetaDataFileRegistry reg;
	
	/**
	 * @return the singelton instance of the registry
	 */
	public static StandardMetaDataFileRegistry getInstance() {
		if (reg == null){
			reg = new StandardMetaDataFileRegistry();
		}
		return reg;
	}
	
	private StandardMetaDataFileRegistry() {
		new StandardMetaDataFileRegistryReader(this).readRegistry();
	}

	/**
	 * @param uri as String
	 * @return list of standard metadata sources as <code>IMetaDataSourceModelProvider</code>s.  
	 * Returns empty list of no standard metadata files are registered for the given uri.
	 */
	public synchronized List/*<IMetaDataSourceModelProvider>*/ getStandardMetaDataModelProviders(String uri) {
		List/*<IMetaDataSourceModelProvider>*/ theList = (List)mdFilesMap.get(uri);
		return theList != null ? theList : EMPTY_LIST ;
	}
		
	/**
	 * For use by registry reader only
	 * @param uri
	 * @param StandardMetaDataFileInfo
	 */
	public synchronized void addStandardMetaDataFileInfo(String uri, IStandardMetaDataSourceInfo fileInfo) {
		List/*<IMetaDataSourceModelProvider>*/ providers = (List) mdFilesMap.get(uri);
		if (providers == null) {
			providers = new ArrayList/*<IStandardMetaDataSourceInfo>*/();
			mdFilesMap.put(uri, providers);
		}
		providers.add(new StandardMetaDataFilesProvider(fileInfo, uri));
	}	
	
/**
 * Implementation of IMetaDataSourceModelProvider for "standard" metadata sources
 *
 */
class StandardMetaDataFilesProvider implements IMetaDataSourceModelProvider {
	
	private org.eclipse.jst.jsf.common.metadata.internal.IStandardMetaDataSourceInfo info;
	private Object model;
	private IMetaDataLocator locator;
	private StandardMetaDataSourceFileLocator fileLocator = null;
	
	StandardMetaDataFilesProvider(IStandardMetaDataSourceInfo info, String uri){
		this.info = info;
	}
	
	private StandardMetaDataSourceFileLocator getFileLocator(){
		if (fileLocator == null){		
			if (info.getLocatorClassname() == null){
				fileLocator = new PluginRelativeStandardMetaDataSourceFileLocator();
			}	
			else {		
				Class klass = JSFCommonPlugin.loadClass(info.getLocatorClassname(), info.getBundleId());
				try {
					fileLocator = (StandardMetaDataSourceFileLocator)klass.newInstance();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();				
				}			
			}
			if (fileLocator != null)
				fileLocator.setFileInfo(info);
		}
		return fileLocator;
	}
	
	private InputStream getInputStream() {		
//		URL url = FileLocator.find(Platform.getBundle(info.getBundleId()), Path.fromOSString(info.getLocation()), null); 
		
		if (getFileLocator() != null){
			try {
				return	getFileLocator().getInputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jst.jsf.common.metadata.internal.IMetaDataSourceModelProvider#getSourceModel()
	 */
	public Object getSourceModel() {
		if (model != null)
			return model;
		
		InputStream inputStream = getInputStream();
		try {
			if (inputStream != null){
				EList contents = StandardModelFactory.getInstance().loadStandardFileResource(inputStream);
				//check to see if this is a Model
				if (contents != null && contents.get(0) instanceof Model){				
					model = contents.get(0);
					((Model)model).setSourceModelProvider(this);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (inputStream != null){
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jst.jsf.common.metadata.internal.IMetaDataSourceModelProvider#getLocator()
	 */
	public IMetaDataLocator getLocator() {
		return locator;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jst.jsf.common.metadata.internal.IMetaDataSourceModelProvider#setLocator(org.eclipse.jst.jsf.common.metadata.internal.IMetaDataLocator)
	 */
	public void setLocator(IMetaDataLocator locator) {
		this.locator = locator;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jst.jsf.common.metadata.internal.IMetaDataSourceModelProvider#getClassLoader()
	 */
	public ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jst.jsf.common.metadata.internal.IMetaDataSourceModelProvider#getResourceBundle()
	 */
	private ResourceBundle internalGetResourceBundle() {
		if (getFileLocator() != null){
			try {
				return fileLocator.getResourceBundle();
			} catch (MissingResourceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public boolean canAdapt(Class klass) {
		if (klass == IImageDescriptorProvider.class)
			return true;
		
		if (klass == IResourceBundleProvider.class)
			return true;
		
		return false;
	}

	public Object getAdapter(Class klass) {
		final StandardMetaDataFilesProvider mdp = this;
		if (klass == IImageDescriptorProvider.class){			
			return new IImageDescriptorProvider(){

				public ImageDescriptor getImageDescriptor(String imagePath) {	
					String bundleID = mdp.getFileLocator().getFileInfo().getBundleId();
					URL url = FileLocator.find(Platform.getBundle(bundleID), new Path(imagePath), null);
					return ImageDescriptor.createFromURL(url);
				}
				
			};
		
		} else if (klass == IResourceBundleProvider.class) {
			return new IResourceBundleProvider(){

				public ResourceBundle getResourceBundle() {
					// TODO Auto-generated method stub
					return mdp.internalGetResourceBundle();
				}
				
			};
		}
		return null;
	}

}
}
