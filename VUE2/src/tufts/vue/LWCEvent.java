package tufts.vue;

import java.util.ArrayList;

public class LWCEvent
{

    /** Some pre-defined event types.  Any string may be used as an
     * event identifier, but you must be sure to use the constant
     * object here for any of these events or they may not be
     * recognized */

    public static final String Location = "location"; 
    public static final String Size = "size";
    
    public static final String Label = "label"; 
    public static final String Notes = "notes"; 
    public static final String Scale = "scale"; 
    public static final String Resource = "resource"; 
    public static final String FillColor = "fillColor"; 
    public static final String TextColor = "textColor"; 
    public static final String StrokeColor = "strokeColor"; 
    public static final String StrokeWidth = "strokeWidth"; 
    public static final String Font = "font";

    public static final String Added = "added"; // a child components add-notify
    //public static final String ChildAdded = "childAdded";// the parent component's add-notify
    public static final String ChildrenAdded = "childrenAdded";// the parent component's group add-notify
    public static final String ChildRemoved = "childRemoved";// the parent component's remove-notify
    public static final String ChildrenRemoved = "childrenRemoved";// the parent component's group remove-notify

    public static final String Deleting = ":deleting"; // the component's just-before-delete notify
    public static final String Deleted = "deleted"; // the component's after-delete notify

    public static final String Repaint = "repaint"; // general: visual change but no permanent data change

    private Object source;
    
    //a LWCevent can either hold a single component or an array of components
    //one of them is always null
    private LWComponent component = null;
    private ArrayList components = null;
    private Object oldValue = null;
    
    private String what;
    
    // todo: we still using both src & component?
    public LWCEvent(Object source, LWComponent c, String what, Object oldValue)
    {
        this.source = source;
        this.component = c;
        this.what = what;
        this.oldValue = oldValue;
    }

    public LWCEvent(Object source, LWComponent c, String what) {
        this (source, c, what, null);
    }

    public LWCEvent(Object source, ArrayList components, String what)
    {
        this.source = source;
        this.components = components;
        this.what = what;
    }
    
    public Object getSource()
    {
        return this.source;
    }
    
    public LWComponent getComponent()
    {
        if (component == null && components != null && components.size() > 0) {
            if (/*DEBUG.EVENTS &&*/ components.size() > 1)
                System.out.println(this + " *** RETURNING FIRST IN LIST IN LIU OF LIST OF LENGTH " + components.size());
            return (LWComponent) components.get(0);
        } else
            return component;
    }
    
    public ArrayList getComponents()
    {
        return this.components;
    }
    
    public String getWhat()
    {
        return this.what;
    }

    public Object getOldValue()
    {
        return this.oldValue;
    }

    public String toString()
    {
        String s = "LWCEvent[" + what
            + " s=" + source;
        
        if (component != null && component != source)
            s += " c:" + component;
        //basic information.. if more information wants to be stringfied, need to code this part
        else if (components != null)
            s += " cl:" + components;
        //s += " ArrayList";
              
        return s + "]";
    }
}

