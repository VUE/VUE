/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

public class SakaiContent implements SakaiContentObject
{
	private String _displayName = null;
	private String _description = null;
	private String _MIMEType = null;
	private byte[] _bytes = null;
	
	public String getDisplayName()
	{
		return _displayName;
	}
	
	public void setDisplayName(String displayName)
	{
		_displayName = displayName;
	}
	
	public String getDescription()
	{
		return _description;
	}
	
	public void setDescription(String description)
	{
		_description = description;
	}
	
	public String getMIMEType()
	{
		return _MIMEType;
	}
	
	public void setMIMEType(String MIMEType)
	{
		_MIMEType = MIMEType;
	}
	
	public byte[] getBytes()
	{
		return _bytes;
	}
	
	public void setBytes(byte[] bytes)
	{
		_bytes = bytes;
	}
}
	