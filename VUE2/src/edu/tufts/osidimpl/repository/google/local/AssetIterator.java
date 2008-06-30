/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.tufts.osidimpl.repository.google.local;

import com.google.soap.search.*;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;
import java.io.*;
import java.net.*;
import tufts.Util;
import tufts.vue.*;
import org.xml.sax.InputSource;

public class AssetIterator
implements org.osid.repository.AssetIterator
{
    private java.util.Iterator iterator = new java.util.Vector().iterator();
	private int currentIndex = 0;
	private String searchURL = null;
	private String criteria = null;
	private static Unmarshaller unmarshaller = null;
	private edu.tufts.vue.dsm.OsidFactory factory = null;
	private static URL XML_MAPPING;
	private static URL url;
	private boolean initializedByVector = false;
	private static String result = "";

    protected AssetIterator(String searchURL,
							String criteria)
    throws org.osid.repository.RepositoryException
    {
		initializedByVector = false;
		this.searchURL = searchURL;
		this.criteria = criteria;

		try {
			factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
			String path = factory.getResourcePath("google.xml");
                        
                        String urlString = path;
                        
                        // Windows requires extra "/" for start of path
                        // see for example: http://blogs.msdn.com/ie/archive/2006/12/06/file-uris-in-windows.aspx
                        
                        if(Util.isWindowsPlatform())
                        {
                            urlString = "/" + urlString;
                        }
                        
                        urlString = "file://" + urlString;
                        
			XML_MAPPING = new URL(urlString);
		} catch (Throwable t) {
			Utilities.log(t);
		}
		performSearch();    
	}

    protected AssetIterator(java.util.Vector v)
		throws org.osid.repository.RepositoryException
    {
		initializedByVector = true;
		this.iterator = v.iterator();
	}
	
    public boolean hasNextAsset()
    throws org.osid.repository.RepositoryException
    {
		if (this.iterator.hasNext()) {
			return true;
		} else if (!initializedByVector) {
			performSearch();
			return (this.iterator.hasNext());
		} else {
			return false;
		}
    }

    public org.osid.repository.Asset nextAsset()
    throws org.osid.repository.RepositoryException
    {
		try {
			if (hasNextAsset()) {
				return (org.osid.repository.Asset)this.iterator.next();
			}
		} catch (Throwable t) {
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
    }
	
	private void performSearch()
	{
		try {
			if (this.currentIndex > 100) {
				this.iterator = (new java.util.Vector()).iterator();
			} else {
				url = new URL(this.searchURL+"&num=10&start="+this.currentIndex+"&q="+this.criteria);
				//System.out.println("Google search = " + url);
				InputStream input = url.openStream();
				int c;
				while((c=input.read())!= -1) {
					result = result + (char) c;
				}
				String googleResultsFile = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.google.results");
				FileWriter fileWriter = new FileWriter(googleResultsFile);
				fileWriter.write(result);
				fileWriter.close();
				result = "";
				
				GSP gsp = loadGSP(googleResultsFile);
				java.util.Iterator i = gsp.getRES().getResultList().iterator();
				java.util.Vector resultVector = new java.util.Vector();
				
				while(i.hasNext()) {
					Result r = (Result)i.next();
					Resource resource = Resource.getFactory().get(r.getUrl());
					if (r.getTitle() != null) 
                                            resource.setTitle(r.getTitle().replaceAll("</*[a-zA-Z]>",""));
                                        else 
                                            resource.setTitle(r.getUrl().toString());
					 resultVector.add(new Asset(r.getTitle(),"",r.getUrl()));
					 //System.out.println(r.getTitle()+" "+r.getUrl());
				}
				this.iterator = resultVector.iterator();
				this.currentIndex += (resultVector.size() - 1);
			}
		} catch (Throwable t) {
			Utilities.log("cannot connect google");
		}
	}
				 
	// Functions to support marshalling and unmarshalling of the reults generated through search using castor.
	private static GSP loadGSP(String filename) 
	{
		try {
			Unmarshaller unmarshaller = getUnmarshaller();
			unmarshaller.setValidation(false);
			GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource(new FileReader(filename)));
			return gsp;
		} catch (Exception e) {
			System.out.println("loadGSP[" + filename + "]: " + e);
			e.printStackTrace();
			return null;
		}
	}
				 
	private static GSP loadGSP(URL url) 
	{
		try {
			InputStream input = url.openStream();
			int c;
			while((c=input.read())!= -1) {
				result = result + (char) c;
			}
			
			Unmarshaller unmarshaller = getUnmarshaller();
			unmarshaller.setValidation(false);
			GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource());
			return gsp;
		} catch (Exception e) {
			System.out.println("loadGSP " + e);
			e.printStackTrace();
			return null;
		}
	}
				 
	private static Unmarshaller getUnmarshaller() 
	{
		if (unmarshaller == null) {
			unmarshaller = new Unmarshaller();
			Mapping mapping = new Mapping();
			try {
				mapping.loadMapping(XML_MAPPING);
				unmarshaller.setMapping(mapping);
			} catch (Exception e) {
				System.out.println("TuftsGoogle.getUnmarshaller: " + XML_MAPPING+e);
			}
		}
		return unmarshaller;
	}
}
