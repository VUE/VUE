package edu.tufts.vue.dsm;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 Installing (or deinstalling) an implementation of an OSID (a plug-in) may involve 
 different operations depending on the user's environment and the type of provider.
 */

public interface OsidLocalInstaller
{
	public void installPlugin(String filename,
							  java.io.InputStream outputStream);
	
	public void deinstallPlugin(DataSource dataSource);
}
