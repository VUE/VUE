package tufts.vue;

/**
 * We need to figure this one out a bit more
 */
public class Resource
{
    String spec;

    public Resource(String spec)
    {
        this.spec = spec;
    }

    public java.net.URL toURL()
        throws java.net.MalformedURLException
    {
        String txt;
        if (spec.startsWith(java.io.File.separator))
            txt = "file://" + spec;
        else
            txt = spec;
        return  new java.net.URL(txt);
    }
    
    public Object toDigitalRepositoryReference()
    {
        return null;
    }
    
    public String toString()
    {
        return spec;
    }

}
