/*
 * CabinetResource.java
 *
 * Created on January 23, 2004, 2:32 PM
 */

package tufts.vue;
import osid.filing.*;
import java.util.*;
import java.io.*;
import java.net.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;

/**
 *  A wrapper for CabinetEntry objects which can be used as the user object in a 
 *  DefaultMutableTreeNode.  It implements the Resource interface specification.
 *
 * @author  Mark Norton
 */
public class CabinetResource implements Resource {  
    private int type = Resource.NONE;
    private osid.filing.CabinetEntry entry = null;
    private boolean selected = false;
    
    /** Creates a new instance of CabinetResource */
    public CabinetResource(osid.filing.CabinetEntry entry) {
        this.entry = entry;
        this.type = Resource.URL;
    }
    
    /**
     *  Display the content associated with this resource.  For a cabinet resource,
     *  a call is made to show a URL.
     *
     *  @author Mark Norton
     */
    public void displayContent() {
        try {
            VueUtil.openURL(getSpec());
        }
        catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     *  Return the file extension for this resource.  In general, cabinets don't have
     *  an extension, since they are directories.  In those cases, the empty string is
     *  returned.
     *
     *  @author Mark Norton
     */
    public String getExtension() {
        String ext = null;
        URL url = null;
        try {
            url = new URL (getSpec());      //  Get the URL of this cabinet.
        }
        catch (java.net.MalformedURLException ex) {
            ex.printStackTrace();
        }
        
        File file =  new File (url.getFile());  //  Extract the file portion.
        if (file.isDirectory())
            ext = new String ("dir");              //  Directories don't have extensions.
        else {
            String name = file.getName();       //  Get the filename with out path.
            ext = name.substring (name.lastIndexOf ('.'));  //  Extract extention.
        }
        return ext;
    }
    
    /**
     *  Return the metadata properties associated with this object.  This is currently
     *  subbbed out to return an empty Properties set.
     *
     *  @author Mark Norton
     */
    public java.util.Properties getProperties() {
        return new Properties();
    }
    
    /**
     *  Return the resource specification.  For cabinet resources, this is URL of either
     *  a local or remote file.
     *
     *  @author Mark Norton
     */
    public String getSpec() {
        //  Check for each of the four possible cases.
        if (this.entry instanceof tufts.oki.remoteFiling.RemoteByteStore)
            return ((RemoteByteStore)this.entry).getUrl();
        if (this.entry instanceof tufts.oki.remoteFiling.RemoteCabinet)
            return ((RemoteCabinet)this.entry).getUrl();
        if (this.entry instanceof tufts.oki.localFiling.LocalByteStore)
            return ((LocalByteStore)this.entry).getUrl();
        if (this.entry instanceof tufts.oki.localFiling.LocalCabinet)
            return ((LocalCabinet)this.entry).getUrl();
        
        //  Shouldn't ever get here, but handle it anyways.
        return new String ("");
    }
    
    /**
     *  Return the title of this cabinet resource.  The cabinet entry display name is
     *  used as the title.
     *
     *  @author Mark Norton
     */
    public String getTitle() {
        String title = null;
        try {
            title = entry.getDisplayName();
        }
        catch (osid.filing.FilingException ex) {
            //  This can't fail.
        }
        return title;
    }
    
    /**
     *  Return the tool tip information for this resource.  This is currently stubbed
     *  to return an empty string.
     *
     *  @author Mark Norton
     */
    public String getToolTipInformation() {
        return new String ("");
    }
    
    /**
     *  Return the resource type.
     *
     *  @author Mark Norton
     */
    public int getType() {
        return this.type;
    }
    
    /**
     *  Return the selected flag.
     *
     *  @author Mark Norton
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     *  Set the selected flag to the value provided.
     *
     *  @author Mark Norton
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     *  Return the cabinet entry associated with this resource.
     *
     *  @author Mark Norton
     */
    public osid.filing.CabinetEntry getEntry() {
        return this.entry;
    }
    
}
