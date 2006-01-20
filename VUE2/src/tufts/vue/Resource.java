 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */


/*
 * Resource.java
 *
 * Created on January 23, 2004, 9:26 AM
 */

package tufts.vue;

/**
 *  The Resource interface defines a set of methods which all vue resource objects must
 *  implement.  Together, they create a uniform way to handle dragging and dropping of
 *  resource objects.
 *
 * @version $Revision: 1.36 $ / $Date: 2006-01-20 20:06:48 $ / $Author: sfraize $
 * @author  akumar03
 */
import java.util.Properties;
import javax.swing.JComponent;

// todo: consider adding an optional icon that can be set for the resource
// todo fix: type isn't always being set in VUE code (e.g., VueDragTree),
//      and type may not really belong here (e.g., ASSET & FAVORITE types too specific)

public interface Resource 
{
    public static final java.awt.datatransfer.DataFlavor DataFlavor =
        tufts.vue.gui.GUI.makeDataFlavor(Resource.class);
        
    /*  The follow type codes are defined for resources.
     */
    static final int NONE = 0;              //  Unknown type.
    static final int FILE = 1;              //  Resource is a Java File object.
    static final int URL = 2;               //  Resource is a URL.
    static final int DIRECTORY = 3;         //  Resource is a directory or folder.
    static final int FAVORITES = 4;         //  Resource is a Favorites Folder
    static final int ASSET_OKIDR  = 10;     //  Resource is an OKI DR Asset.
    static final int ASSET_FEDORA = 11;     //  Resource is a Fedora Asset.
    static final int ASSET_OKIREPOSITORY  = 12;     //  Resource is an OKI Repository OSID Asset.
    /**  
     *  Return the title or display name associated with the resource.
     *  (any length restrictions?)
     */
    public String getTitle();
    
    /**
     *  Return a resource reference specification.  This could be a filename or URL.
     */
    public String getSpec();
    
    /**
     *  Return the filename extension of this resource (if any).
     *  (What if it doesn't have an extension?  Unix files are not required to have one)
     */
    // get rid of this, and perhaps add a getType?  needs to encompass file/directory/URL/DR 
    public String getExtension();
    
    /**
     *  Return tooltip information or none.
     *  (should null be returned if no tool tip info?)
     */
    public String getToolTipInformation();
    
    /**
     *  Return any metadata associated with this resource as a collection of Java
     *  properties.  Dublin core metadata has defined keywords (where defined?)
     */
    public Properties getProperties();
    

    /**
     * @return the value for the given property key, or null if no such property.
     */
    public String getProperty(String key);

    /**
     * Set the property named by the given key to value.
     */
    public void setProperty(String key, Object value);

    
    /** 
     *  Return true if the resource is selected.  Initialize select flag to false.
     */
    public boolean isSelected();
    
    /**
     *  Set the selected flag to the value given.
     */
    public void setSelected(boolean selected);
    
    /** 
     *  Return the resource type.  This should be one of the types defined above.
     */
    public int getType();
    
    /**
     *  Display the content associated with the resource.  For example, call
     *  VueUtil.open() using the spec information.
     */
    public void displayContent();
    
    /**
     *Associate a asset viewer with a resource. 
     *
     */
    public JComponent getAssetViewer();
    
    /**
     * Get preview of the object such as thummbnail, small sized image or web-page
     *that can be displayed in the bowser.
     */
    public JComponent getPreview();
      
}
