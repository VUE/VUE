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
	