package tufts.vue;

public class LWCEvent
{
    private Object source;
    private LWComponent component;
    private String what;

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
        return "LWCEvent[" + what
            + " " + component
            + " src=" + source
            + "]";
    }
}

