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
