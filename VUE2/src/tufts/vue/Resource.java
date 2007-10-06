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

import tufts.Util;
import java.util.Properties;
import java.net.URI;
import java.awt.Image;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *  The Resource interface defines a set of methods which all vue resource objects must
 *  implement.  Together, they create a uniform way to handle dragging and dropping of
 *  resource objects.
 *
 * @version $Revision: 1.48 $ / $Date: 2007-10-06 06:17:10 $ / $Author: sfraize $
 * @author  akumar03
 */

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

public abstract class Resource 
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Resource.class);

    public static final java.awt.datatransfer.DataFlavor DataFlavor =
        tufts.vue.gui.GUI.makeDataFlavor(Resource.class);

    /** interface for Resource factories.  All methods are "get" based as opposed to "create"
     * as the implementation may optionally provide resources on an atomic basis (e.g., all equivalent URI's / URL's
     * may return the very same object */
    public static interface Factory {
        Resource get(String spec);
        Resource get(java.net.URL url);
        Resource get(java.net.URI uri);
        Resource get(java.io.File file);
        Resource get(osid.filing.CabinetEntry entry);
    }

    /** A default resource factory: does basic delgation by type, but only handle's absolute resources (e.g., does no relativization) */
    public static class DefaultFactory implements Factory {
        public Resource get(String spec) {
            return postProcess(URLResource.create(spec), spec);
        }
        public Resource get(java.net.URL url) {
            return postProcess(URLResource.create(url), url);
        }
        public Resource get(java.net.URI uri) {
            return postProcess(URLResource.create(uri), uri);
        }
        public Resource get(java.io.File file) {
            // someday this may return something like a FileResource (which could make
            // use of a LocalCabinetResource, if we upgraded that API to be useful
            // and handle things like fetch file typed icon images, etc).
            return postProcess(URLResource.create(file), file);
        }
        public Resource get(osid.filing.CabinetEntry entry) {
            return postProcess(CabinetResource.create(entry), entry);
        }

        protected Resource postProcess(Resource r, Object source) {
            Log.debug("Created " + Util.tags(r) + " from " + Util.tags(source));
            return r;
        }
    }

    private static final Factory AbsoluteResourceFactory = new DefaultFactory();

    /** @return the default resource factory */
    // could allow installation of a new default factory (be sure to make installation threadsafe if do so)
    public static Factory getFactory() {
        return AbsoluteResourceFactory;
    }

        
    /*  Some client type codes defined for resources.  */

    /*
     * Todo: the set of types should ideally be defined / used by clients and subclass impl's,
     * not enumerated in the Resource class, but we're keeping this around as is
     * for old code and given that this info is actually persisted in save files.
     */

    static final int NONE = 0;              //  Unknown type.
    static final int FILE = 1;              //  Resource is a Java File object.
    static final int URL = 2;               //  Resource is a URL.
    static final int DIRECTORY = 3;         //  Resource is a directory or folder.
    static final int FAVORITES = 4;         //  Resource is a Favorites Folder
    static final int ASSET_OKIDR  = 10;     //  Resource is an OKI DR Asset.
    static final int ASSET_FEDORA = 11;     //  Resource is a Fedora Asset.
    static final int ASSET_OKIREPOSITORY  = 12;     //  Resource is an OKI Repository OSID Asset.

    protected static final String[] TYPE_NAMES = {
        "NONE", "FILE", "URL", "DIRECTORY", "FAVORITES",
        "unused5", "unused6", "unused7", "unused8", "unused9", 
        "ASSET_OKIDR", "ASSET_FEDORA", "ASSET_OKIREPOSITORY"
    };

//     // preview preference keys: use 
//     public static final Object SMALL = "small";
//     public static final Object MEDIUM = "medium";
//     public static final Object LARGE = "large";


    /** the metadata property map **/
    final protected PropertyMap mProperties = new PropertyMap();

    static final long SIZE_UNKNOWN = -1;
    private long mByteSize = SIZE_UNKNOWN;


    public long getByteSize() {
        return mByteSize;
    }
    
    protected void setByteSize(long size) {
        mByteSize = size;
    }
    
    
    /**
     * Set the given property value.
     * Does nothing if either key or value is null, or value is an empty String.
     */
    public void setProperty(String key, Object value) {
        if (DEBUG.DATA) out("setProperty " + key + " [" + value + "]");
        if (key != null && value != null) {
            if (!(value instanceof String && ((String)value).length() < 1))
                mProperties.put(key, value);
        }
    }

    /**
     * Add a property with the given key.  If a key already exists
     * with this name, the key will be modified with an index.
     */
    public String addProperty(String desiredKey, Object value) {
        return mProperties.addProperty(desiredKey, value);
    }
    

    public void setProperty(String key, long value) {
        if (key.endsWith(".contentLength") || key.endsWith(".size")) {
            // this kind of a hack
            setByteSize(value);
        }
        setProperty(key, Long.toString(value));
    }

    
    /**
     * This method returns a value for the given property name.
     * @param pName the property name.
     * @return Object the value
     **/
    public String getProperty(String key) {
        final Object value = mProperties.get(key);
        if (DEBUG.RESOURCE) out("getProperty[" + key + "]=" + value);
        return value == null ? null : value.toString();
    }

    public int getProperty(String key, int notFoundValue) {
        final Object value = mProperties.get(key);

        int intValue = notFoundValue;
        
        if (value != null) {
            if (value instanceof Number) {
                intValue = ((Number)value).intValue();
            } else if (value instanceof String) {
                try {
                    intValue = Integer.parseInt((String)value);
                } catch (NumberFormatException e) {
                    if (DEBUG.DATA) tufts.Util.printStackTrace(e);
                }
            }
        }
        
        return intValue;
    }
    
    public boolean hasProperty(String key) {
        return mProperties.containsKey(key);
    }
    
    public PropertyMap getProperties() {
        return mProperties;
    }

    
    /** @return true if this resource contains displayable image data */
    public abstract boolean isImage();

    /**
     * @return an object suitable to be handed to the Java ImageIO API that can
     * in some way be read and converted to an image: e.g., java.net.URL, java.io.File,
     * java.io.InputStream, javax.imageio.stream.ImageInputStream, etc.
     * If the object provides a convenient, unique, persisent key, such as URL or File,
     * the VUE Images code can use that to cache the result on disk.
     * May return null if no image is available.
     */
    public abstract Object getImageSource();
    
    /**  
     *  Return the title or display name associated with the resource.
     *  (any length restrictions?)
     */
    public abstract String getTitle();

    public abstract void setTitle(String title);    
    
    //public abstract java.net.URL asURL();
    
    //public abstract long getSize();

    /**
     *  Return a resource reference specification.  This could be a filename or URL.
     */
    public abstract String getSpec();
    
    /**
     *  Return the filename extension of this resource (if any).
     *  (What if it doesn't have an extension?  Unix files are not required to have one)
     */
    // get rid of this, and perhaps add a getType?  needs to encompass file/directory/URL/DR 
    public abstract String getExtension();
    
    /**
     *  Return tooltip information or none.
     *  (should null be returned if no tool tip info?)
     */
    public abstract String getToolTipInformation();
    
    
    /** 
     *  Return true if the resource is selected.  Initialize select flag to false.
     */
    //public abstract boolean isSelected();
    
    /**
     *  Set the selected flag to the value given.
     */
    //public abstract void setSelected(boolean selected);
    
    /** 
     * Return the resource type.  This should be one of the types defined above.
     */
    public abstract int getClientType();
    
    /**
     *  Display the content associated with the resource.  For example, call
     *  VueUtil.open() using the spec information.
     */
    public abstract void displayContent();


    private ImageIcon mTinyIcon;
    /** @return a 16x16 icon */
    public Icon getTinyIcon() {
        if (mTinyIcon != null)
            return mTinyIcon;
        
        Image image = tufts.vue.gui.GUI.getSystemIconForExtension(getExtension(), 16);
        if (image != null) {
            if (image.getWidth(null) > 16) {
                // see http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
                // on how to do this better/faster -- happens very rarely on the Mac tho, but need to test PC.
                image = image.getScaledInstance(16, 16, 0); //Image.SCALE_SMOOTH);
            }
            mTinyIcon = new javax.swing.ImageIcon(image);
        }
        return mTinyIcon;
    }

    public Image getTinyIconImage() {
        if (mTinyIcon == null)
            getTinyIcon();
        if (mTinyIcon != null)
            return mTinyIcon.getImage();
        else
            return null;
    }

    private ImageIcon mLargeIcon;
    /** @return up to a 128x128 icon */
    public Icon getLargeIcon() {
        if (mLargeIcon != null)
            return mLargeIcon;
        
        Image image = tufts.vue.gui.GUI.getSystemIconForExtension(getExtension(), 128);
        if (image != null) {
            if (image.getWidth(null) > 128) {
                image = image.getScaledInstance(128, 128, 0); //Image.SCALE_SMOOTH);
            }
            mLargeIcon = new javax.swing.ImageIcon(image);
        }
        return mLargeIcon;
    }

    public Image getLargeIconImage() {
        if (mLargeIcon == null)
            getLargeIcon();
        if (mLargeIcon != null)
            return mLargeIcon.getImage();
        else
            return null;
    }
    
    


//     /**
//      * Get preview of the object, e.g., a thummbnail.  Currently, this should be 32x32 pixels.
//      */
//     public javax.swing.Icon getIcon() {
//         return getIcon(null);
//     }
    
    private tufts.vue.ui.ResourceIcon mIcon;
    /**
     * @param repainter -- the component to request repainting on when
     * the icons loads if it's not immediately available.  This is required
     * to support cell rendererers -- e.g., the component the icon paints
     * on does not forward repaint requests.  May be null if cell renderers not in use.
     */
    public synchronized javax.swing.Icon getContentIcon(java.awt.Component repainter) {

        //if (!isImage())
        //  return null;
        
        if (mIcon == null) {
            //tufts.Util.printStackTrace("getIcon " + this); System.exit(-1);
            // TODO: cannot cache this icon if there is a freakin painter,
            // (because we'd only remember the last painter, and prior
            // users of this icon would stop getting updates)
            // -- this is why putting a client property in the cell renderer
            // is key, tho it's annoying it will have to be fetched
            // every time -- or could create an interface: Repaintable
            mIcon = new tufts.vue.ui.ResourceIcon(this, 32, 32, repainter);
        }
        return mIcon;
    }

    public javax.swing.Icon getContentIcon() {
        return getContentIcon(null);
    }
    
    //public abstract javax.swing.Icon getIcon();
    //public abstract javax.swing.Icon getIcon(java.awt.Component painter);
    //public abstract javax.swing.Icon getIcon(int width, int height);

    /**
     * Get preview of the object such as thummbnail / small sized image. Suggested return
     * types are something that can be converted to image data, or a java GUI component,
     * such as a java.awt.Component, javax.swing.JComponent, or javax.swing.Icon.
     */
    public abstract Object getPreview();

    /**
     * @param preferredSize: either SMALL, MEDIUM, or LARGE. This is a general hint only and may
     * not be respected.  If the Resource is image content, 
     */
    //public abstract Object getPreview(Object preferredSize);


    //public abstract boolean isCached();

    // todo: should be protected or not have it
    public abstract void setCached(boolean cached);

    public abstract void updateRootLocation(URI oldRoot, URI newRoot);
    public abstract String getPrettyString();

    //public abstract void setPreview(Object preview);
      
    
    /**
     *Associate a asset viewer with a resource. 
     *
     */
    //public abstract JComponent getAssetViewer();


    protected void out(String s) {
        Log.info(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + ": " + s);
    }
    
    
}
