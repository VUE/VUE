package tufts.vue;

import java.util.ArrayList;

public class LWCEvent
{
    public static final String NO_OLD_VALUE = "no_old_value";
    
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
        this (source, c, what, NO_OLD_VALUE);
    }

    public LWCEvent(Object source, ArrayList components, String what)
    {
        this.source = source;
        this.components = components;
        this.what = what;
        this.oldValue = NO_OLD_VALUE;
    }
    
    public Object getSource()
    {
        return this.source;
    }
    
    public LWComponent getComponent()
    {
        if (component == null && components != null && components.size() > 0) {
            if (/*DEBUG.EVENTS &&*/ components.size() > 1) {
                System.out.println(this + " *** RETURNING FIRST IN LIST IN LIU OF LIST OF LENGTH " + components.size());
                new Throwable().printStackTrace();
            }
            return (LWComponent) components.get(0);
        } else
            return component;
    }
    
    public ArrayList getComponents() {
        return this.components;
    }
    public String getWhat() {
        return this.what;
    }

    /**
     * Return an old value if one was given to us.  As null is a valid
     * old value, it's distinguished from having no old value set
     * by the @return value LWCEvent.NO_OLD_VALUE.  Or you can
     * check for the presence of an old value by calling hasOldValue().
     */
    public Object getOldValue() {
        return this.oldValue;
    }
    public boolean hasOldValue() {
        return this.oldValue != NO_OLD_VALUE;
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

