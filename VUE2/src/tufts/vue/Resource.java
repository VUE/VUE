package tufts.vue;

/**
 * The Resource class is intended to handle a reference
 * to either a URL (local file: or http:) or a digital
 * repository reference, which is TBD.  This needs
 * more work.
 */

import java.util.*;

import osid.dr.*;
import tufts.oki.dr.fedora.*;
import org.w3c.dom.Document;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import fedora.server.types.gen.*;


public class Resource
{
    static final long SIZE_UNKNOWN = -1;
    // constats that define the type of resource
    static final int NONE = 0;
    static final int FILE = 1;
    static final int URL = 2;
    static final int DIRECTORY = 3;
    static final int ASSET_OKIDR  = 10;
    static final int ASSET_FEDORA = 11;
    
    static final String[] dcFields = {"dc:title","dc:creator","dc:subject","dc:date","dc:type","dc:format","dc:identifier","dc:collection","dc:coverage"};
    
    long referenceCreated;
    long accessAttempted;
    long accessSuccessful;
    long size = SIZE_UNKNOWN;
    private Asset asset;
    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.
    protected transient boolean selected = false;
    String spec;
    
    /** the metadata property map **/
 	private    Map mProperties = new HashMap();
 	
 	/** property name cache **/
 	private String [] mPropertyNames = null;

    public Resource() {   
    }
    
    public Resource(String spec)
    {
        this.spec = spec;
        this.referenceCreated = System.currentTimeMillis();
    }

    private void setPropertiesByAsset() {
         try { 
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            InputStream dublinCoreInputStream = new ByteArrayInputStream(((MIMETypedStream)(asset.getInfoField(new PID("getDublinCore")).getValue())).getStream());
            Document document = factory.newDocumentBuilder().parse(dublinCoreInputStream);
            for(int i=0;i<dcFields.length;i++) {
                NodeList list = document.getElementsByTagName(dcFields[i]);
                if(list != null && list.getLength() != 0) {
                     // only picks the first element 
                    if(list.item(0).getFirstChild() != null) 
                        mProperties.put(dcFields[i], list.item(0).getFirstChild().getNodeValue());
                }
             }
    
        } catch (Exception ex)  {ex.printStackTrace();}
        
    }
    
    public void setAsset(Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this.asset = asset;
        setPropertiesByAsset();
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
        else {
            ext = spec.substring(0, Math.min(spec.length(), 3));
            if (!spec.endsWith("/")) {
                int i = spec.lastIndexOf('.');
                if (i > 0 && i < spec.length()-1)
                    ext = spec.substring(i+1);
            }
        }
        if (ext.length() > 5)
            ext = ext.substring(0,5);

        return ext;
    }

    
    /**
     * getPropertyNames
     * This returns an array of property names
     * @return String [] the list of property names
     **/
    public String [] getPropertyNames() {
    	
    	if( (mPropertyNames == null) && (!mProperties.isEmpty()) ) {
	    	Set keys = mProperties.keySet();
			if( ! keys.isEmpty() ) {
				mPropertyNames = new String[ keys.size() ];
				Iterator it = keys.iterator();
				int i=0;
				while( it.hasNext()) {
					mPropertyNames[i] = (String) it.next();
					i++;
					}
				}
			}
    	return mPropertyNames;
    }
    
    /**
     * setPropertyValue
     * This method sets a property value
     * Note:  This method will add a new property if called.
     *        Since this is a small version of a VUEBean, only
     *        two property class values are supported:  String and Vector
     *        where Vector is a vector of String objects.
     * @param STring pName the proeprty name
     * @param Object pValue the value
     **/
     public void setPropertyValue( String pName, Object pValue) {
     	/** invalidate our dumb cache of names if we add a new one **/
     	if( !mProperties.containsKey( pName) ) {
     		mPropertyNames = null;
     		}	
     	mProperties.put( pName, pValue);
     }
     
     /**
      * getPropertyValue
      * This method returns a value for the given property name.
      * @param pname the property name.
      * @return Object the value
      **/
     public Object getPropertyValue( String pName) {
     	Object value = null;
     	value = mProperties.get( pName);
     	return value;
     }
     
     public boolean isSelected(){
         return selected;
     }
     
     public void setSelected(boolean selected) {
         this.selected = selected;
     }
    public String toString()
    {
        return getSpec();
    }
    
    
    
}
