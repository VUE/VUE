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


package tufts.vue;

/**
 *  The Resource interface defines a set of methods which all vue resource objects must
 *  implement.  Together, they create a uniform way to handle dragging and dropping of
 *  resource objects.
 *
 * @version $Revision: 1.44 $ / $Date: 2006-04-26 20:52:36 $ / $Author: sfraize $
 * @author  akumar03
 */
import java.util.Properties;
import javax.swing.JComponent;

// todo: consider adding an optional icon that can be set for the resource
// todo fix: type isn't always being set in VUE code (e.g., VueDragTree),
//      and type may not really belong here (e.g., ASSET & FAVORITE types too specific)

// TODO:
// add getSource (or: get Location/Repository/Collection/DataSource/Where) (e.g. "ArtStor", "Internet/Web", "Local File")
//      may be a good enough replacement for type?  type currently mixes location with content
//      a bit by adding "directory", which along with "file" are really just both "Local File" locations,
//      and "Favorites" is a total odd man-out: any given resource that happens to be in the
//      favorites list may actually be of any of the other given types).
// get rid of spec, but maybe include a getPath, and include a hasURL/getURL as is too damn handy.
// add getReader, getContent (need something to get an image object, or do we handle that
//      via a cross-cutting concern?)
// get rid of getExtension, tho we may be trying to get a mime-type with that one.
//      it *would* be really nice if we could easily know if this is, say, an image or HTML content tho
//      maybe we do add mime-type and punt with "type/unknown" for most everything else
//      until we might someday be able to extract this info from the filesystem.  Still
//      might be useful enough to add a special case "isImage" tho (or maybe hasImage?)
//
// get rid of getToolTipInformation: getTitle will be our only special case
// mapping of information from either meta-data or the URL/file-name to something
// exposed in the API.
//
// Get rid if selection methods.
//
// Maybe even get rid of displayContent: make a cross-cutting concern handled
// by a ResourceHandler?  could theoretically put stuff like isImage/isHTML
// there also, altho we could leave both of this in the API and address
// the weight of it by having a good AbstractResource that handles all
// this stuff for you.
//
// Oh, we'll still need something for persistance tho, which spec was handling.
// Maybe getAddress?  getUniqueID (a GLOBAL id, which implies us coming up
// with protocol names for every OSID resource, but we had that problem
// anyway.  Hmm: we could actually use the class name of the OSID DR impl
// and let that handle it?  (or really the DataSource, unless we get
// rid of that).
//
// Maybe get rid of getPreview for now; may add in a set of getPreview / getViewer / getEditor later.
//
// getIcon: make getThumbnail?  this needs to be defined: is too generic: maybe get rid of.
//      the Asset/DataSource logo should come from the DataSource.  So maybe we need
//      a getDataSource, which is really what the getWhere ends up being, and returns
//      an object instead of a name.  Tho is of course handy for not just thumbnails,
//      but say type-based application icons (from OS meta-data), or say the favicon.ico
//      from a website.
//
// AND: if we ever get int getting "parts", maybe we should throw this whole thing out
//      use the DR API itself?  a bit heavy weight tho.
//
// And don't forget: altho we want to handle "any kind of data source", the majority
// of them are probably all going to be URL based, at least for the forseeabe future,
// which is a practical consideration worth keeping in mind.

// So the basic services being provided are:
//      1 - get data for VUE (provide a handle for it)
//      2 - get meta-data about the data (including the all important where this thing came from)
//      3 - provide persistance of the reference to the data for VUE
//      4 - provide some convenince wrapping of meta-data to tell us some very
//          useful things such as:
//              - a title
//              - a URL if available
//              - what we can do with this data (it's type), such is isImage, isHTML, and/or getMimeType
//
// Items 1-3 are an absolute requiment.  Items in 4 are for things useful
// enough to include in the API, but only priority items worth expanding
// the API for (these items are all computed from the meta-data, both properties based and DataSource based)
//
// Could also consider calling this an "Asset", tho that may be just too confusing given OKI & Fedora.
//
// Possibly add a "getName", to be the raw stub of a URL v.s. a massaged one for getTitle,
// or former v.s. the meta-data title of a properly title document/image from a repository.
// This is pure convience for a GUI / possibly "important" data for a user.  Maybe only
// really for local files tho.  Could opt out of it but putting it in title and just
// not having a massaged version that leaves (that does stuff like create spaces, removes
// the extension, etc).



// Note: the inclusion of "setProperty" has huge implications, which means
// writing back to the underlying repository, unless we're just going to
// provide local masking/extension of that data, which I recommend against.
// If we eventually want write-back, this could be provided via the DataSource.
// Let's get rid of this?

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

    // preview preference keys: use 
    public static final Object SMALL = "small";
    public static final Object MEDIUM = "medium";
    public static final Object LARGE = "large";


    /** @return true if this resource contains displayable image data */
    public boolean isImage();
    
    /**  
     *  Return the title or display name associated with the resource.
     *  (any length restrictions?)
     */
    public String getTitle();
    
    public java.net.URL asURL();
    
    public long getSize();

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
    //public Properties getProperties();
    public PropertyMap getProperties();
    

    /**
     * @return the value for the given property key, or null if no such property.
     */
    public String getProperty(String key);
    public int getProperty(String key, int notFoundValue);
    /**
     * Set the property named by the given key to value.
     */
    public void setProperty(String key, Object value);
    public void setProperty(String key, long value);

    /**
     * Add a property with the given key.  If a key already exists
     * with this name, the key will be modified with an index.
     */
    public void addProperty(String desiredKey, Object value);
    
    /** 
     *  Return true if the resource is selected.  Initialize select flag to false.
     */
    //public boolean isSelected();
    
    /**
     *  Set the selected flag to the value given.
     */
    //public void setSelected(boolean selected);
    
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
     * Get preview of the object, e.g., a thummbnail.  Currently, this should be 32x32 pixels.
     */
    public javax.swing.Icon getIcon();
    public javax.swing.Icon getIcon(java.awt.Component painter);
    //public javax.swing.Icon getIcon(int width, int height);

    /**
     * Get preview of the object such as thummbnail / small sized image. Suggested return
     * types are something that can be converted to image data, or a java GUI component,
     * such as a java.awt.Component, javax.swing.JComponent, or javax.swing.Icon.
     */
    public Object getPreview();

    /**
     * @param preferredSize: either SMALL, MEDIUM, or LARGE. This is a general hint only and may
     * not be respected.  If the Resource is image content, 
     */
    //public Object getPreview(Object preferredSize);


    public boolean isCached();

    // todo: should be protected or not have it
    public void setCached(boolean cached);

    //public void setPreview(Object preview);
      
    
    /**
     *Associate a asset viewer with a resource. 
     *
     */
    //public JComponent getAssetViewer();
    
}
