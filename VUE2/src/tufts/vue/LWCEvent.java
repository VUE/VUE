package tufts.vue;

import java.util.ArrayList;

public class LWCEvent
{
    private Object source;
    
    //a LWCevent can either hold a single component or an array of components
    //one of them is always null
    private LWComponent component = null;
    private ArrayList components = null;
    
    private String what;
    
    // todo: we still using both src & component?
    public LWCEvent(Object source, LWComponent c, String what)
    {
        this.source = source;
        this.component = c;
        this.what = what;
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
        return this.component;
    }
    
    public ArrayList getComponents()
    {
        return this.components;
    }
    
    public String getWhat()
    {
        return this.what;
    }

    public String toString()
    {
        String s = "LWCEvent[" + what
            + " s:" + source;
        
        if (component != null && component != source)
            s += " c:" + component;
        //basic information.. if more information wants to be stringfied, need to code this part
        else if (components != null)
            s += " cl:" + components;
        //s += " ArrayList";
              
        return s + "]";
    }
}

