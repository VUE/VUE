/*
 * VueType.java
 *
 * Created on November 15, 2003, 4:29 PM
 */

package tufts.oki.shared;

import tufts.oki.*;
/**
 *
 * @author  Daisuke Fujiwara
 */
public class VueType extends osid.shared.Type {
    
    public static final String VUE_TYPE_KEY = "osid.shared.VueType";
    
    /** Creates a new instance of VueType */
    public VueType() 
    {
        super ("osid.shared", OsidManager.AUTHORITY, VUE_TYPE_KEY, "This is a Vue Type");
    }
    
}
