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

package tufts.vue;

/** Some pre-defined property types.  Any string may be used as an
 * property or event identifier, but you must be sure to use the constant
 * object here for any of these events or they may not be
 * recognized */
public interface LWKey {

    public String UserActionCompleted = "user.action.completed";
    
    public String Location = "location"; 
    public String Size = "size";
    public String Frame = "frame"; // location & size
    
    public String Label = "label"; 
    public String Notes = "notes"; 
    public String Scale = "scale"; 
    public String Resource = "resource"; 
    public String FillColor = "fill.color"; 
    public String TextColor = "text.color"; 
    public String StrokeColor = "stroke.color"; 
    public String StrokeWidth = "stroke.width"; 
    public String Font = "font";
    public String Hidden = "hidden";

    /** an instance of a RectangularShape */
    public String Shape = "node.shape"; 

    public String Created = "new.component"; // any LWComponets creation event
    //public String Added = "added"; // a child components add-notify
    //public String ChildAdded = "childAdded";// the parent component's add-notify
    public String ChildrenAdded = "hier.childrenAdded";// the parent component's group add-notify
    //public String ChildRemoved = "hier.childRemoved";// the parent component's remove-notify
    public String ChildrenRemoved = "hier.childrenRemoved";// the parent component's group remove-notify
    public String HierarchyChanging = "hier.changing"; // pre-change event for any hierarchy change
    public String HierarchyChanged = "hier.changed"; // post-change event for hierarchy changes during undo operations

    public String Deleting = ":deleting"; // the component's just-before-delete notify
    public String Deleted = "deleted"; // the component's after-delete notify

    public String Repaint = "repaint"; // general: visual change but no permanent data change

    public String LinkArrows = "link.arrows"; // link arrow state (0,1,2)
    
}
