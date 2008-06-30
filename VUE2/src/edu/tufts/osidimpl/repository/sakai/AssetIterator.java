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
package edu.tufts.osidimpl.repository.sakai;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import javax.xml.namespace.QName;

public class AssetIterator
implements org.osid.repository.AssetIterator
{
    private java.util.Iterator iterator = null;
	private String siteString = null;
	private String key = null;
	
	public static final String LIST_TAG = "list";
	public static final String RESOURCE_TAG = "resource";
	public static final String ID_TAG = "id";
	public static final String NAME_TAG = "name";
	public static final String TYPE_TAG = "type";
	public static final String URL_TAG = "url";

    public AssetIterator(java.util.Vector vector)
		throws org.osid.repository.RepositoryException
    {
        this.iterator = vector.iterator();
    }
	
	public AssetIterator(String siteString,
						 String key,
						 String xml)
	{
		this.siteString = siteString;
		this.key = key;
		
		java.util.Vector result = new java.util.Vector();
		try {
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			org.w3c.dom.Document document = db.parse(new java.io.ByteArrayInputStream(xml.getBytes()));
			
			org.w3c.dom.NodeList nl = document.getElementsByTagName(LIST_TAG);
			org.w3c.dom.Element listElement = (org.w3c.dom.Element)nl.item(0);
			nl = document.getElementsByTagName(RESOURCE_TAG);
			int numResources = nl.getLength();
			for (int i=0; i < numResources; i++) {
				org.w3c.dom.Element resourceElement = (org.w3c.dom.Element)nl.item(i);
				String id = Utilities.expectedValue(resourceElement,ID_TAG);
				String name = Utilities.expectedValue(resourceElement,NAME_TAG);
				String type = Utilities.expectedValue(resourceElement,TYPE_TAG);
				String url = Utilities.expectedValue(resourceElement,URL_TAG);
				
				//System.out.println("Next Resource");
				//System.out.println("\tId: " + id);
				//System.out.println("\tName: " + name);
				//System.out.println("\tType: " + type);
				//System.out.println("\tURL: " + url);
				
				org.osid.shared.Type assetType = null;
				if (type.equals("collection")) assetType = Utilities.getCollectionAssetType();
				if (type.equals("resource")) assetType = Utilities.getResourceAssetType();
				result.addElement(new Asset(id,assetType,key,name,url));
			}
		} catch (Throwable t) {
			Utilities.log(t);
		}
		this.iterator = result.iterator();
	}
	
    public boolean hasNextAsset()
		throws org.osid.repository.RepositoryException
    {
		return iterator.hasNext();
    }
	
    public org.osid.repository.Asset nextAsset()
		throws org.osid.repository.RepositoryException
    {
        if (iterator.hasNext())
        {
            return (org.osid.repository.Asset)iterator.next();
        }
        else
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
    }
}
