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

/*
 * VueQualifierType.java
 *
 * Created on December 15, 2003, 1:58 PM
 */

package tufts.oki.authorization;
import java.util.*;

/**
 *  This class serves as a generic type for osid.authorization.Qualifier objects.  All
 *  keynames should be unique.  It is recommended that qualifier type keynames be created
 *  by concatenating a specific function type to the QUALIFIER_TYPE_KEY provided here.<br>
 *  <br>
 *  For example:  VueFunctionType.QUALIFIER_TYPE_KEY + ".Unlimited"
 *
 * @author  Mark Norton
 */
public class VueQualifierType extends osid.shared.Type {
     public static final String QUALIFIER_TYPE_KEY = "osid.authorization.Qualifier";
     public static Vector qualifierTypes = null;
   
    /** Creates a new instance of VueFunctionType  given a keyname.  */
    public VueQualifierType(String keyname) {
        super ("osid.shared", tufts.oki.OsidManager.AUTHORITY, keyname);
        
        //  Initialize the global list of function types and add the new one.
        if (qualifierTypes == null)
            qualifierTypes = new Vector(100);
        qualifierTypes.add (keyname);
    }
    
    /** Creates a new instance of VueFunctionType  given a keyname and description.  */
    public VueQualifierType(String keyname, String description) {
        super ("osid.shared", tufts.oki.OsidManager.AUTHORITY, keyname, description);
        
        //  Initialize the global list of function types and add the new one.
        if (qualifierTypes == null)
            qualifierTypes = new Vector(100);
        qualifierTypes.add (keyname);
    }
    
}
