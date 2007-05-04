/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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
	
    public AssetIterator(java.util.Vector vector)
		throws org.osid.repository.RepositoryException
    {
        this.iterator = vector.iterator();
    }
	
	public AssetIterator(String siteString,
						 String key)
	{
		this.siteString = siteString;
		this.key = key;
		// temp shunt
		this.iterator = (new java.util.Vector()).iterator();
	}
	
    public boolean hasNextAsset()
		throws org.osid.repository.RepositoryException
    {
		if (iterator.hasNext()) {
			return true;
		} else {
			// look for more
			try {
				String endpoint = Utilities.getEndpoint();
				Service  service = new Service();
				
				//	Get the list of resources in the test collection.
				Call call = (Call) service.createCall();
				call.setTargetEndpointAddress (new java.net.URL(endpoint) );
				call.setOperationName(new QName(Utilities.getAddress(), "getResources"));
				String resString = (String) call.invoke( new Object[] {Utilities.getSessionId(this.key), this.siteString} );
				System.out.println("Sent ContentHosting.getAllResources(sessionId,testId), got '" + resString + "'");
				// how do we know whether there are more collections or resources?
				return false;
		    } 
			catch (Exception e) {
				System.err.println(e.toString());
				return false;
			}
		}
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
