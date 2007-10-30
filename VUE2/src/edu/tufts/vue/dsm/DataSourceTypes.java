/*
 * DataSourceTypes.java
 *
 * Created on October 12, 2007, 11:16 AM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author  akumar03
 * @version $Revision: 1.2 $ / $Date: 2007-10-30 15:04:20 $ / $Author: peter $
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.dsm;

public class DataSourceTypes {
    public static final org.osid.shared.Type FEDORA_REPOSITORY_TYPE = new edu.tufts.vue.util.Type("tufts.edu","repository","fedora_2_2");
    public static final org.osid.shared.Type SAKAI_REPOSITORY_TYPE = new edu.tufts.vue.util.Type("sakaiproject.org","repository","contentHosting");
    public static final org.osid.shared.Type SAKAI_COLLECTION_ASSET_TYPE = new edu.tufts.vue.util.Type("sakaiproject.org","asset","siteCollection");
   
    
}
