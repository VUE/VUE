package edu.tufts.osidimpl.test.repository;

public class Utilities
{
	private static org.osid.id.IdManager _idManager = null;
	
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
			} catch (java.lang.NullPointerException npe) {
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