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
package edu.tufts.osidimpl.repository.google.global;

public class Utilities
{
	private static org.osid.id.IdManager idManager = null;
    private static org.osid.logging.WritableLog log = null;
	private static org.osid.OsidContext context = null;

	public static void setOsidContext(org.osid.OsidContext c)
	{
		context = c;
	}
	
	public static org.osid.OsidContext getOsidContext()
	{
		return context;
	}
	
	
	public static void setIdManager(org.osid.id.IdManager manager)
	{
		idManager = manager;
	}

	public static org.osid.id.IdManager getIdManager()
	{
		return idManager;
	}

	public static void setLog(org.osid.logging.WritableLog l)
	{
		log = l;
	}
	
	public static void log(String entry)
	{
		try {
			log.appendLog(entry);
		} catch (org.osid.logging.LoggingException lex) {
			// swallow exception since logging is a best attempt to log an exception anyway
		}   
	}

	public static void log(Throwable t)
	{
		try {
			t.printStackTrace();
			log.appendLog(t.getMessage());
		} catch (org.osid.logging.LoggingException lex) {
			// swallow exception since logging is a best attempt to log an exception anyway
		}   
	}	
	
	public static String typeToString(org.osid.shared.Type type)
	{
		return type.getDomain() + "/" + type.getKeyword() + "@" + type.getAuthority();
	}
}