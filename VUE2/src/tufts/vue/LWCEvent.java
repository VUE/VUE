package tufts.vue;

public class LWCEvent
{
    private Object source;
    private LWComponent component;
    private String what;

    // todo: we still using both src & component?
    public LWCEvent(Object source, LWComponent c, String what)
    {
        this.source = source;
        this.component = c;
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
    
    public String getWhat()
    {
        return this.what;
    }

    public String toString()
    {
        String s = "LWCEvent[" + what
            + " " + component;
        if (source != component)
            s += " src=" + source;
        return s + "]";
    }
}

