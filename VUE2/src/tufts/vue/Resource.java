package tufts.vue;

/**
 * The Resource class is intended to handle a reference
 * to either a URL (local file: or http:) or a digital
 * repository reference, which is TBD.  This needs
 * more work.
 */
public class Resource
{
    static final long SIZE_UNKNOWN = -1;
    
    long referenceCreated;
    long accessAttempted;
    long accessSuccessful;
    long size = SIZE_UNKNOWN;
    
    String spec;

    public Resource(String spec)
    {
        this.spec = spec;
        this.referenceCreated = System.currentTimeMillis();
    }

    public Object toDigitalRepositoryReference()
    {
        return null;
    }
    
    public String toString()
    {
        return spec;
    }
    
    public String toURLString()
    {
        String txt;
        
        // todo fixme: this pathname may have been created on another
        // platform, (meaning, a different separator char) unless
        // we're going to canonicalize everything ourselves coming
        // in...
        
        if (spec.startsWith(java.io.File.separator))
            txt = "file://" + spec;
        else
            txt = spec;
        return txt;
    }
    
    public java.net.URL toURL()
        throws java.net.MalformedURLException
    {
        return new java.net.URL(toURLString());
    }

    public void displayContent()
    {
        try {
            this.accessAttempted = System.currentTimeMillis();
            VueUtil.openURL(toURLString());
            this.accessSuccessful = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    

}
