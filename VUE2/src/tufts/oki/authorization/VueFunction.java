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
 * VueFunction.java
 *
 * Created on December 12, 2003, 9:20 AM
 */

package tufts.oki.authorization;
import tufts.oki.shared.*;
import osid.shared.*;

/**
 *  A function is some operation that can be performed in an application or service.
 *  Essentially, it is a tuple consisting of a name, description, type, and id.  In
 *  addition, the function can be associated with a qualifier hierarchy, if one
 *  exists.  Since this implementation doesn't depend on osid.hierarchy, the qualifier
 *  hierarchy refers to a root qualifier node.
 *
 * @author  Mark Norton
 */
public class VueFunction implements osid.authorization.Function {
    
    private String displayName = null;                  //  The function display name.
    private String description = null;                  //  The function description.
    private osid.shared.Type type = null;               //  The function type.
    private osid.shared.Id id = null;                   //  The function id.
    private osid.shared.Id qualifierHierarchyId = null; //  The qualifier hierarchy id.
    
    /** Creates a new instance of VueFunction given minimal parameters. */
    public VueFunction(osid.shared.Type type, osid.shared.Id id) {
        this.type = type;
        this.id = id;
    }
    
    /** Creates a new instance of VueFunction without a qualifier hierarchy association.  */
    public VueFunction(String dispName, String description, osid.shared.Type type, osid.shared.Id id) {
        this.displayName = dispName;
        this.description = description;
        this.type = type;
        this.id = id;
    }
    
    /** Creates a new instance of VueFunction with a qualifier hierarchy association.  */
    public VueFunction(String dispName, String description, osid.shared.Type type, osid.shared.Id id, osid.shared.Id hierId) {
        this.displayName = dispName;
        this.description = description;
        this.type = type;
        this.id = id;
        this.qualifierHierarchyId = hierId;
    }
    
    /**
     *  Return the description of this function.
     *
     *  @author Mark Norton
     */
    public String getDescription() {
        return description;
    }
    
    /**
     *  Return the display name of this function.
     *
     *  @author Mark Norton
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     *  Return the function type of this function.
     *
     *  @author Mark Norton
     */
    public osid.shared.Type getFunctionType() {
        return type;
    }
    
    /**
     *  Return the id of this function.
     *
     *  @author Mark Norton
     */
    public osid.shared.Id getId() {
        return id;
    }
    
    /**
     *  Return the qualifier hierarchy id associated with this function.  This
     *  Id may be null if no qualifier hierarchy is defined for this function.
     *
     *  @author Mark Norton
     */
    public osid.shared.Id getQualifierHierarchyId() {
        return qualifierHierarchyId;
    }
    
    /**
     *  Update the description of this function.
     *
     *  @author Mark Norton
     */
    public void updateDescription(String description) {
        this.description = description;
    }
    
    /**
     *  Update the display name of this function.
     *
     *  @author Mark Norton
     */
    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
}
