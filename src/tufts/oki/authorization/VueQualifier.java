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

/*
 * VueQualifier.java
 *
 * Created on December 12, 2003, 9:46 AM
 */

package tufts.oki.authorization;
import java.util.*;
import osid.shared.*;
import osid.authorization.*;

/**
 *  VueQualifier implements osid.authorization.Qualifier.  Qualifiers are the "what"
 *  part of an authentication rule (who can-do what).  Each qualifier basically consists
 *  of an Id and qualifier Type, which distinguishes them from other qualifiers.  A
 *  display name and description make qualifiers more human-understandable.
 *  <p>
 *  Qualifiers are designed to be organized in a hierary, which allows authorizations to
 *  be based on high-level, comprehensive qualifiers.  Thus, if a particular person 
 *  (represented by an agent) is authorized to edit course content in a particular section,
 *  that person is authorized to edit a particular course in that section.
 *  <p>
 *  This implementation of qualifiers defines its own hiearchical tree structure and doesn't
 *  require or rely on an external implementation of hierarchy, such as osid.hierarchy.
 *  Each qualifier may have muliple parents and multiple children.  The parent / child
 *  relationship is maintained in both qualifiers.
 *
 *  @author  Mark Norton
 */
public class VueQualifier implements osid.authorization.Qualifier {
    
    private Vector parents = null;                  //  The list of parent qualifiers.
    private Vector children = null;                 //  The list of children qualifiers.
    private String displayName = null;              //  The qualifier display name.
    private String description = null;              //  The qualifier description.
    private osid.shared.Id id = null;               //  The qualifier id.
    private osid.shared.Type type = null;           //  The qualifier type.
    private AuthorizationManager azMgr = null;   //  Cached AuthZMgr to resolve id's against objects.
    
    /** Creates a new instance of VueQualifier */
    public VueQualifier(String displayName, String description, osid.shared.Id id, osid.shared.Type type, AuthorizationManager mgr) {
        this.displayName = displayName;
        this.description = description;
        this.id = id;
        this.type = type;
        this.parents = new Vector(100);
        this.children = new Vector(100);
        this.azMgr = mgr;
    }
    
    /** Creates a new instance of VueQualifier given a parent. */
    public VueQualifier(String displayName, String description, osid.shared.Id id, osid.shared.Type type, AuthorizationManager mgr, VueQualifier parent) {
        this.displayName = displayName;
        this.description = description;
        this.id = id;
        this.type = type;
        this.parents = new Vector(100);
        this.children = new Vector(100);
        this.parents.add (parent);
        this.azMgr = mgr;
    }
    
    /**
     *  Add the qualifier passed to the list of children in this qualifier.  Called by
     *  addParent() to make parent/child relationship bidirectional.
     *  Note that this method is not part of the osid.authorization.Qualifier interface.
     *
     *  @author Mark Norton
     */
    private void addChild (osid.shared.Id childQualifierId) {
        VueQualifier qual = (VueQualifier) azMgr.getQualifier (childQualifierId);
        if (this.children.indexOf(qual) == -1)
            this.children.add(qual);
    }
    
    /**
     *  Add the qualifier passed as a parent to this qualifier.
     *
     *  @author Mark Norton
     */
    public void addParent(osid.shared.Id parentQualifierId) {
        VueQualifier qual = (VueQualifier) azMgr.getQualifier (parentQualifierId);
        if (this.parents.indexOf(qual) == -1)
            this.parents.add(qual);
        qual.addChild (parentQualifierId);
    }
    
    /**
     *  Replace the old parent indicated with the new one passed.
     *
     *  @author Mark Norton
     */
    public void changeParent(osid.shared.Id oldParentId, osid.shared.Id newParentId) {
        VueQualifier oldParent = (VueQualifier) azMgr.getQualifier (oldParentId);
        VueQualifier newParent = (VueQualifier) azMgr.getQualifier (newParentId);
        int off = parents.indexOf (oldParent);
        parents.set(off, newParent);
        newParent.addChild (this.id);
    }
    
    /**
     *  Return an iterator listing the children of this qualifier.
     *
     *  @author Mark Norton
     */
    public osid.authorization.QualifierIterator getChildren() {
        return (osid.authorization.QualifierIterator) new VueQualifierIterator (children);
    }
    
    /**
     *  Return the description of this qualifier.
     *
     *  @author Mark Norton
     */
    public String getDescription() {
        return description;
    }
    
    /**
     *  Return the display name of this qualfier.
     *
     *  @author Mark Norton
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     *  Return the Id of this qualifier.
     *
     *  @author Mark Norton
     */
    public osid.shared.Id getId() {
        return id;
    }
    
    /**
     *  Return an iterator listing the parents of this qualifier.
     *
     *  @author Mark Norton
     */
    public osid.authorization.QualifierIterator getParents() {
        return (osid.authorization.QualifierIterator) new VueQualifierIterator (children);
    }
    
    /**
     *  Return the qualifier type.
     *
     *  @author Mark Norton
     */
    public osid.shared.Type getQualifierType() {
        return type;
    }
    
    /**
     *  Return true if this qualifier is a child of the id given as a parent.
     *
     *  @author Mark Norton
     */
    public boolean isChildOf(osid.shared.Id parentId) {
        try {
            for (int i = 0; i < parents.size(); i++) {
                VueQualifier qual = (VueQualifier) parents.elementAt(i);
                if (this.id.isEqual(parentId))
                    return true;
            }
        }
        catch (osid.shared.SharedException ex) {}
        return false;
    }
    
    /**
     *  Return true if the qualifier Id passed identifies an ancestor of this qualifier.
     *  This is accomplished by recursively walking up the parent references.
     *  <p>
     *  Warning:  This method is called recursively.
     *
     *  @author Mark Norton
     */
    public boolean isDescendantOf(osid.shared.Id ancestorId) {
        //  If parents list is empty, this will fall through to returning false.
        for (int i=0; i<parents.size(); i++) {
            VueQualifier parent = (VueQualifier) parents.elementAt(i);
            
            //  Check each parent in the list to see if it is the ancestor desired.
            try {
                if (ancestorId.isEqual(parent.getId()))
                    return true;
                else
                    return parent.isDescendantOf(ancestorId);
            }
            catch (osid.shared.SharedException ex) {}
        }
        return false;   //  stubbed
    }
    
    /**
     *  Return true if this qualifer is a parent.
     *
     *  @author Mark Norton
     */
    public boolean isParent() {
        if (children.isEmpty())
            return false;
        else
            return true;
    }
    
    /**
     *  Remove the child given by the idenfier passed from the list of children.  This
     *  is called by removeParent to maintain the bidirectional parent/child links.
     *
     *  @author Mark Norton
     */
    private void removeChild (osid.shared.Id childQualifierId) {
        VueQualifier qual = (VueQualifier) azMgr.getQualifier (childQualifierId);
        if (this.children.indexOf(qual) != -1)
            this.children.remove(qual);
    }
    
    /**
     *  Remove the parent qualifer from the list of parents of this qualifier.
     *
     *  @author Mark Norton
     */
    public void removeParent(osid.shared.Id parentQualifierId) {
        VueQualifier qual = (VueQualifier) azMgr.getQualifier (parentQualifierId);
        parents.remove(qual);
    }
    
    /**
     *  Update the description to the new one given.
     *
     *  @author Mark Norton
     */
    public void updateDescription(String description) {
        this.description = description;
    }
    
    /**
     *  Update the display name to the new one given.
     *
     *  @author Mark Norton
     */
    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
}
