package tufts.vue;

/** Some pre-defined property types.  Any string may be used as an
 * property or event identifier, but you must be sure to use the constant
 * object here for any of these events or they may not be
 * recognized */
public interface LWKey {

    public String Location = "location"; 
    public String Size = "size";
    public String Frame = "frame"; // location & size
    
    public String Label = "label"; 
    public String Notes = "notes"; 
    public String Scale = "scale"; 
    public String Resource = "resource"; 
    public String FillColor = "fillColor"; 
    public String TextColor = "textColor"; 
    public String StrokeColor = "strokeColor"; 
    public String StrokeWidth = "strokeWidth"; 
    public String Font = "font";
    public String Hidden = "hidden";

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

    public String LinkArrows = "linkArrows"; // link arrow state (0,1,2)
    
}
