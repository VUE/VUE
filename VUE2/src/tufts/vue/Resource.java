package tufts.vue;

/**
 * The Resource class is intended to handle a reference
 * to either a URL (local file: or http:) or a digital
 * repository reference, which is TBD.  This needs
 * more work.
 */

import osid.dr.*;
import tufts.dr.fedora.*;

public class Resource
{
    static final long SIZE_UNKNOWN = -1;
    
    long referenceCreated;
    long accessAttempted;
    long accessSuccessful;
    long size = SIZE_UNKNOWN;
    private Asset asset;
    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.
    String spec;

    public Resource() {   
    }
    
    public Resource(String spec)
    {
        this.spec = spec;
        this.referenceCreated = System.currentTimeMillis();
    }

    public void setAsset(Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this.asset = asset;
        this.spec = ((FedoraObject)asset).getDefaultViewURL();
        this.castorFedoraObject = new CastorFedoraObject((FedoraObject)asset);
    }
    
    public Asset getAsset() {
        
        return this.asset;
    }
    
    public void setCastorFedoraObject(CastorFedoraObject castorFedoraObject) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this.castorFedoraObject = castorFedoraObject; 
        this.asset = this.castorFedoraObject.getFedoraObject();
        this.spec =  ((FedoraObject)this.castorFedoraObject.getFedoraObject()).getDefaultViewURL();
    }
    
    public CastorFedoraObject getCastorFedoraObject() {
        return this.castorFedoraObject;
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
        System.out.println("displayContent for " + this);
        try {
            this.accessAttempted = System.currentTimeMillis();
            //if (getAsset() != null) {
                //AssetViewer a = new AssetViewer(getAsset());
                //a.setSize(600,400);
                //a.show();
            //} else
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

    public boolean isLocalFile()
    {
        String s = spec.toLowerCase();
        return s.startsWith("file:") || s.indexOf(':') < 0;
    }

    public String getExtension()
    {
        String ext = "xxx";
        if (spec.startsWith("http"))
            ext = "www";
        else if (spec.startsWith("file"))
            ext = "file";
        else
            ext = spec.substring(0, Math.min(spec.length(), 3));

        if (!spec.endsWith("/")) {
            int i = spec.lastIndexOf('.');
            if (i > 0 && i < spec.length()-1)
                ext  = spec.substring(i+1);
        }

        return ext;
    }

    public String toString()
    {
        return getSpec();
    }
    
}
