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

package edu.tufts.osidimpl.testing.repository;

public class Utilities
{
	private static org.osid.id.IdManager _idManager = null;
	private static boolean _verbose = false;
	
	public static void setVerbose(boolean v)
	{
		_verbose = v;
	}
	
	public static boolean isVerbose()
	{
		return _verbose;
	}
	
	public static String typeToString(org.osid.shared.Type type) 
	{
		return type.getDomain() + "/" + type.getKeyword() + "@" + type.getAuthority();
	}
	
	public static org.osid.shared.Type stringToType(String typeString) {
		String authority = "_";
		String domain = "_";
		String keyword = "_";
		try {
			if (typeString != null) {
				int indexSlash = typeString.indexOf("/");
				if (indexSlash != -1) {
					domain = typeString.substring(0,indexSlash);
					int indexAt = typeString.indexOf("@");
					if (indexAt != -1) {
						keyword = typeString.substring(indexSlash+1,indexAt);
						authority = typeString.substring(indexAt+1);
					}
				}
			}
		} catch (Throwable t) {
			// ignore formatting error
		}
		return new Type(authority,domain,keyword);
	}
	
	public static String expectedValue(org.w3c.dom.Element element, String tag)
		throws org.xml.sax.SAXParseException
	{
		String expected = null;
		org.w3c.dom.NodeList nameNodeList = element.getElementsByTagName(tag);
		int numNodes = nameNodeList.getLength();
		if (numNodes > 0) {
			org.w3c.dom.Element e = (org.w3c.dom.Element)nameNodeList.item(0);
			try {
				expected = e.getFirstChild().getNodeValue();
				//System.out.println("before decode " + expected + " after " + java.net.URLDecoder.decode(expected,"ISO-8859-1"));
				expected = java.net.URLDecoder.decode(expected,"ISO-8859-1");
			} catch (Exception ex) {
			}
		}
		return expected;
	}
	
	public static org.osid.id.IdManager getIdManager()
	{
		if (_idManager == null) {
			try {
				_idManager = (org.osid.id.IdManager)org.osid.OsidLoader.getManager("org.osid.id.IdManager",
																				   "comet.osidimpl.id.no_persist",
																				   new org.osid.OsidContext(),
																				   new java.util.Properties());
			} catch (Throwable t) {
			}
		}
		return _idManager;
	}
}