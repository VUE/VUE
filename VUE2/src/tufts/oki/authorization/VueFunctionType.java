/*
 * VueFunctionType.java
 *
 * Created on December 15, 2003, 1:44 PM
 */

package tufts.oki.authorization;
import osid.shared.*;
import java.util.*;

/**
 *  This class serves as a generic type for osid.authorization.Function objects.  All
 *  keynames should be unique.  It is recommended that function type keynames be created
 *  by concatenating a specific function type to the FUNCTION_TYPE_KEY provided here.<br>
 *  <br>
 *  For example:  VueFunctionType.FUNCTION_TYPE_KEY + ".Fedora.AddNewEntry"
 *
 * @author  Mark Norton
 */
public class VueFunctionType extends osid.shared.Type {
     public static final String FUNCTION_TYPE_KEY = "osid.authorization.Function";
     public static Vector functionTypes = null;
   
    /** Creates a new instance of VueFunctionType  given a keyname.  */
    public VueFunctionType(String keyname) {
        super ("osid.shared", tufts.oki.OsidManager.AUTHORITY, keyname);
        
        //  Initialize the global list of function types and add the new one.
        if (functionTypes == null)
            functionTypes = new Vector(100);
        functionTypes.add (keyname);
    }
    
    /** Creates a new instance of VueFunctionType  given a keyname and description.  */
    public VueFunctionType(String keyname, String description) {
        super ("osid.shared", tufts.oki.OsidManager.AUTHORITY, keyname, description);
        
        //  Initialize the global list of function types and add the new one.
        if (functionTypes == null)
            functionTypes = new Vector(100);
        functionTypes.add (keyname);
    }
    
}
