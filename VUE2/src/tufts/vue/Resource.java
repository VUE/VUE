package tufts.vue;

/**
 * The Resource class is intended to handle a reference
 * to either a URL (local file: or http:) or a digital
 * repository reference, which is TBD.  This needs
 * more work.
 */

import osid.dr.*;

public class Resource
{
    static final long SIZE_UNKNOWN = -1;
    
    long referenceCreated;
    long accessAttempted;
    long accessSuccessful;
    long size = SIZE_UNKNOWN;
    private Asset asset;
    String spec;

    public Resource() {   
    }
    
    public Resource(String spec)
    {
        this.spec = spec;
        this.referenceCreated = System.currentTimeMillis();
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }
    
    public Asset getAsset() {
        return this.asset;
    }
    
    public Object toDigitalRepositoryReference()
    {
        return null;
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

    public long getReferenceCreated() {
        return this.referenceCreated;
    }
    
    public void setReferenceCreated(long referenceCreated) {
        this.referenceCreated = referenceCreated;
    }
    
    public long getAccessAttempted() {
        return this.accessAttempted;
    }
    
    public void setAccessAttempted(long accessAttempted) {
        this.accessAttempted = accessAttempted;
    }
    
    public long getAccessSuccessful() {
        return this.accessSuccessful;
    }
    
    public void setAccessSuccessful(long accessSuccessful) {
        this.accessSuccessful = accessSuccessful;
    }
    public long getSize() {
        return this.size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }
    
    public String getSpec() {
       return this.spec;
    }

    public String toString()
    {
        return getSpec();
    }
    

}
