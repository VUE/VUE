 /*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */


package tufts.vue;

import tufts.Util;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.awt.Image;
import java.util.Properties;
import tufts.vue.ui.ResourceIcon;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *  The Resource interface defines a set of methods which all vue resource objects must
 *  implement.  Together, they create a uniform way to handle dragging and dropping of
 *  resource objects.
 *
 * @version $Revision: 1.64 $ / $Date: 2008-04-09 07:12:46 $ / $Author: sfraize $
 */

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

public abstract class Resource implements Cloneable
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Resource.class);

    /** property keys with this prefix will be persisted, but are not meant for display to users */
    public static final String HIDDEN_PREFIX = "@";
    /** property keys with this prefix are for runtime display only: will not be persisted */
    public static final String RUNTIME_PREFIX = "~"; 
    /** property keys with this prefix are both hidden and runtime-only */
    public static final String DEBUG_PREFIX = "##";
    
    public static final String PACKAGE_KEY = HIDDEN_PREFIX + "Packaged";
    public static final String PACKAGE_FILE = RUNTIME_PREFIX + "Package.file";
    //public static final String PACKAGE_FILE = DEBUG_PREFIX + "package.file";
    
    // Some standard property names
    public static final String CONTENT_SIZE = "Content.size";
    public static final String CONTENT_TYPE = "Content.type";
    public static final String CONTENT_MODIFIED = "Content.modified";
    public static final String CONTENT_ASOF = "Content.asOf";
    public static final String CONTENT_SOURCE = "Content.source";
    
    public static final String IMAGE_FORMAT = "image.format";
    public static final String IMAGE_WIDTH = "image.width";
    public static final String IMAGE_HEIGHT = "image.height";
    
    public static final String FILE_RELATIVE = "file.relative";
    
    // VUE synthesized meta-data:
    // content.type:    (content-type / mime-type -- from URL & File)
    // content.size:    (file or URL on-disk content size)
    // content.modified: (file / URL last modified)
    // content.updated:  retrieved / URL "date", will always be the read time (image load) for local files
    //  (accessed? asOf? retrieved?)
    // content.source: e.g., "Local Disk", "Internet" (not Web, as confuses with FTP), "Black Ships"
            

    public static final java.awt.datatransfer.DataFlavor DataFlavor =
        tufts.vue.gui.GUI.makeDataFlavor(Resource.class);

    public static final String SPEC_UNSET = "<spec-unset>";


    /**
     * Interface for Resource factories.  All methods are "get" based as opposed to "create"
     * as the implementation may optionally provide resources on an atomic basis (e.g., all equivalent URI's / URL's
     * may return the very same object.
     *
     * TODO: handle Fedora AssetResource?  Are we still using that, or has it been implemented as a generic OSID?
     * If not being used, see if we can remove it from the codebase...
     *
     */
    public static interface Factory {

        Resource get(String spec);
        Resource get(java.net.URL url);
        Resource get(java.net.URI uri);
        Resource get(java.io.File file);
        Resource get(osid.filing.CabinetEntry entry);
        Resource get(org.osid.repository.Repository repository,
                     org.osid.repository.Asset asset,
                     org.osid.OsidContext context) throws org.osid.repository.RepositoryException;

    }

    /** A default resource factory: does basic delgation by type, but only handle's absolute resources (e.g., does no relativization) */
    public static class DefaultFactory implements Factory {
        public Resource get(String spec) {
            // if spec looks a URL/URI or File, could attempt to construct such and if succeed,
            // pass off to appropriate factory variant.  Wouldn't be worth anything at moment
            // as they all pretty much do the same thing for now...
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

        public Resource get(org.osid.repository.Repository repository,
                            org.osid.repository.Asset asset,
                            org.osid.OsidContext context)
            throws org.osid.repository.RepositoryException
        {
            Resource r = new Osid2AssetResource(asset, context);
            try {
                //if (DEBUG.DR && repository != null) r.addProperty("~Repository", repository.getDisplayName());
                if (repository != null) r.setHiddenProperty("Repository", repository.getDisplayName());
            } catch (Throwable t) {
                Log.warn(Util.tags(r), t);
            }
            return postProcess(r, asset);
        }
        

        protected Resource postProcess(Resource r, Object source) {
            r.setReferenceCreated(System.currentTimeMillis());
            if (DEBUG.DR) Log.debug(Util.tags(source) + " -> " + Util.tags(r));
            //Log.debug("Created " + Util.tags(r) + " from " + Util.tags(source));
            return r;
        }
    }

    private static final Factory AbsoluteResourceFactory = new DefaultFactory();

    /** @return the default resource factory */
    // could allow installation of a new default factory (be sure to make installation threadsafe if do so)
    public static Factory getFactory() {
        return AbsoluteResourceFactory;
    }

    // Convenience factory methods: guaranteed equivalent to getFactory().get(args...)
    // If an LWMap ResourceFactory is available, that should always be used instead of these.

    public static Resource instance(String spec)        { return getFactory().get(spec); }
    public static Resource instance(java.net.URL url)   { return getFactory().get(url); }
    public static Resource instance(java.net.URI uri)   { return getFactory().get(uri); }
    public static Resource instance(java.io.File file)  { return getFactory().get(file); }
    public static Resource instance(osid.filing.CabinetEntry entry) { return getFactory().get(entry); }
    public static Resource instance(org.osid.repository.Repository repository,
                                    org.osid.repository.Asset asset,
                                    org.osid.OsidContext context)
        throws org.osid.repository.RepositoryException
    {
        return getFactory().get(repository, asset, context);
    }
    

        
    /*
     * The set of types might ideally be defined / used by clients and subclass impl's,
     * not enumerated in the Resource class, but we're keeping this around
     * for old code and given that this info is actually persisted in save files
     * going back years. Tho given the way we're currently using these types,
     * we can probably get rid of them / ignore old persisted values if we
     * get time to clean this up.  SMF 2007-10-07
     */

    /*  Some client type codes defined for resources -- TODO: fix this -- there are mixed semantics here  */
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

    /** the metadata property map -- should be final, but not because of clone support **/
    /*final*/ protected PropertyMap mProperties = new PropertyMap();

    static final long SIZE_UNKNOWN = -1;

    private int mType = Resource.NONE;
    
    private long mByteSize = SIZE_UNKNOWN;
    private long mReferenceCreated;
    private long mAccessAttempted;
    private long mAccessSuccessful;

    //----------------------------------------------------------------------------------------
    // Standard methods for all Resources
    //----------------------------------------------------------------------------------------

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

        if (DEBUG.DATA) dumpKV("setProperty", key, value);
        
        if (key != null && value != null) {
            if (!(value instanceof String && ((String)value).length() < 1))
                mProperties.put(key, value);
        }
    }

    public void setProperty(String key, long value) {
        if (key.endsWith(".contentLength") || key.endsWith(".size")) {
            // this kind of a hack
            setByteSize(value);
        }
        setProperty(key, Long.toString(value));
    }

    
    protected void dumpField(String name, Object value) {
        out(String.format("%-31s: %s%s%s", name, Util.TERM_CYAN, Util.tags(value), Util.TERM_CLEAR));
    }

    private void dumpKV(String name, String key, Object value) {
        if (DEBUG.DATA) out(String.format("%-14s%17s: %s%s%s", name, key, Util.TERM_RED, Util.tags(value), Util.TERM_CLEAR));
    }
    

    /** runtime properties are for display while VUE is running only: they're not persisted */
    protected void setRuntimeProperty(String key, Object value) {
        setProperty(RUNTIME_PREFIX + key, value);
    }
    
    /** hidden properties are neither displayed at runtime, nor persisted */
    public void setHiddenProperty(String key, Object value) {
        setProperty(HIDDEN_PREFIX + key, value);
    }

    /** debug properties are neither displayed at runtime, nor persisted */
    protected void setDebugProperty(String key, Object value) {
        setProperty(DEBUG_PREFIX + key, value);
    }

    

    /** @return any prior value stored for this key, null otherwise */
    public Object removeProperty(String key) {
        if (DEBUG.DATA) dumpKV("removeProperty", key, "[" + mProperties.get(key) + "]");
        return mProperties.remove(key);
    }

    /**
     * Add a property with the given key.  If a key already exists
     * with this name, the key will be modified with an index.
     */
    public String addProperty(String desiredKey, Object value) {
        if (DEBUG.DATA) dumpKV("addProperty", desiredKey, value);
        return mProperties.addProperty(desiredKey, value);
    }
    

    /**
     * This method returns a value for the given property name.
     * @param pName the property name.
     * @return Object the value
     **/
    public String getProperty(String key) {
        final Object value = mProperties.get(key);
        if (DEBUG.DATA) dumpKV("getProperty", key, value);
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

    public long getReferenceCreated() { return mReferenceCreated; }
    public void setReferenceCreated(long created) { mReferenceCreated = created; }
    
    public long getAccessAttempted() { return mAccessAttempted; }
    public void setAccessAttempted(long attempted) { mAccessAttempted = attempted; }
    
    public long getAccessSuccessful() { return mAccessSuccessful; }
    public void setAccessSuccessful(long succeeded) { mAccessSuccessful = succeeded; }

    protected void markAccessAttempt() {
        setAccessAttempted(System.currentTimeMillis());
    }
    protected void markAccessSuccess() {
        setAccessSuccessful(System.currentTimeMillis());
    }

    
    //public abstract boolean isImage();
    
    private boolean isImage;
    /** @return true if this resource contains displayable image data */
    public boolean isImage() {
        return isImage;
    }

    protected void setAsImage(boolean asImage) {
        isImage = asImage;
        if (DEBUG.DR || DEBUG.RESOURCE) setDebugProperty("isImage", ""+ asImage);
    }

    /**
     * @return true if the data behind this component has recently changed
     *
     * This impl always returns false.  Override to provide desired semantics.
     */
    public boolean dataHasChanged() {
        return false;
    }

    

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

    /** @return some kind of reliable name: e.g., title if there is one, spec if not */
    public String getName() {
        final String title = getTitle();
        if (title == null || title.trim().length() < 1)
            return getSpec();
        else
            return title;
    }
    
    //public abstract long getSize();

    /**
     *  Return a resource reference specification.  This could be a filename or URL.
     */
    public abstract String getSpec();

    /**
     * If a reference to this resource can be provided as a URL, return it in that form,
     * otherwise return null.
     *
     * @return default Resource class impl: returns null
     */
    public java.net.URL asURL() {
        return null;
    }


    /**
     * All Resource impls should be able to return something that fits into a URI.
     */
    public abstract java.net.URI toURI();
    
    
    /** 
     * Return the resource type.  This should be one of the types defined above.
     */
    public int getClientType() {
        return mType;
    }
    
    /** TODO:  need to remove this -- if we keep any type at all, it should at least be inferred
     * -- probably replace with a setClientType(Object) -- a general marker that clients / UI components can use
     */
    protected void setClientType(int type) {
        mType = type;
        if (DEBUG.RESOURCE) dumpField("setClientType", Integer.valueOf(type));
        try {
            setDebugProperty("clientType", TYPE_NAMES[type] + " (" + type + ")");
        } catch (Throwable t) {
            Log.warn(this + "; setClientType " + type, t);
        }
    }
    
    /**
     *  Display the content associated with the resource.  For example, call
     *  VueUtil.open() using the spec information.
     */
    public abstract void displayContent();

//     public String getTypeIconText() {
//         final String r = getSpec();
//         String ext = "xxx";

//         if (r.startsWith("http"))
//             ext = "web";
//         else if (r.startsWith("file"))
//             ext = "file";
//         else {
//             ext = r.substring(0, Math.min(r.length(), 3));
//             if (!r.endsWith("/")) {
//                 int i = r.lastIndexOf('.');
//                 if (i > 0 && i < r.length()-1)
//                     ext = r.substring(i+1);
//             }            
//         }
//         if (ext.length() > 4)
//             ext = ext.substring(0,4);
        
//         return ext;
//     }
    
    

    public static final String NO_EXTENSION = "";
    public static final String EXTENSION_DIR = "dir";
    public static final String EXTENSION_HTTP = "web";
    public static final String EXTENSION_UNKNOWN = "---";
    public static final String EXTENSION_VUE  = "vue";

    /**
     * Return a filename extension / file type of this resource (if any) suitable for identify it
     * it's "type" to the local file system shell environement (e.g., txt, html, jpg).
     */
    // was getExtension
    // TODO: cache / allow setting (e.g. special data sources might be able to indicate type that's otherwise unclear
    // e.g., a URL query part that requests "type=jpeg")
    public String getDataType() {
//         String type = null;
//         if (getClientType() == DIRECTORY)
//             type = EXTENSION_DIR;
//         else
//             type = extractExtension(getSpec());

        String ext = extractExtension(getSpec());

        if (ext == null || ext == NO_EXTENSION || ext.length() == 0) {
            final String spec = getSpec();
            if (spec == null || spec == SPEC_UNSET || spec.trim().length() < 1)
                ext = EXTENSION_UNKNOWN;
            else if (spec.startsWith("http:")) // todo: https, etc...
                ext = EXTENSION_HTTP;
            else
                ext = EXTENSION_DIR;
        }
        
//         if (type == NO_EXTENSION) {
//             if (getClientType() == FILE) {
//                 // todo: this really ought to be in a FileResource and/or a useful osid filing impl
//                 type = EXTENSION_DIR;
//                 // assume a directory for now...
//             } 
//         }

        if (DEBUG.RESOURCE) out("extType=[" + ext + "] in " + this);
        //if (DEBUG.RESOURCE) out(getSpec() + "; extType=[" + ext + "] in [" + this + "] type=" + TYPE_NAMES[getClientType()]);
        return ext;
    }
    

    /** @return the likely extension for the given string, or NO_EXTENSION if none found */
    protected static String extractExtension(final String s) {

        final char lastChar = s.charAt(s.length()-1);
        String ext = NO_EXTENSION;
        
        //if (lastChar == '/' || lastChar == '\\' || lastChar == File.separatorChar)
        if (Character.isLetterOrDigit(lastChar) == false) {
            // assume some a path element or special file
        } else {
            final int lastDotIdx = s.lastIndexOf('.');
        
            // must have at least one char's worth of file-name, and one two chars worth of data after the dot
            if (lastDotIdx > 1 && (s.length() - lastDotIdx) > 2) {
                String txt = s.substring(lastDotIdx + 1);
                if (Character.isLetterOrDigit(txt.charAt(0))) {
                    ext = txt;
                } else {
                    // failsafe check in case somehow a separator wound up at the end
                    // ext = NO_EXTENSION;
                }
            } // else ext = NO_EXTENSION;
        }


        if (DEBUG.RESOURCE) Log.debug("extract[" + s + "]; ext=[" + ext + "]");
        return ext;
    }

//     protected static String extractExtension(java.net.URL url) {
//         // if HTTP, could check query for stuff like =jpeg at the end
//         final String ext = extractExtension(url.getPath());
//         if (ext == NO_EXTENSION && "http".equals(url.getProtocol()))
//             return "html"; // presume a web document
//         else
//             return ext;
//     }

//     protected static String extractExtension(java.net.URI uri) {
//         return extractExtension(uri.getPath());
//     }
    

    // TODO: create a multi-sized generic smart icon class that can cache / create well-sampled standard
    // sizes, and draw at any requested size.
    // may want to call these getShellIcon{small,large}, etc, tho that's REALLY what
    // we want implemented in LocalCabinetEntry (and move the GUI.getSystemIconForExtension code there),
    // for the beginning of a truely useful OKI filing API.

    private ImageIcon mTinyIcon;
    /** @return a 16x16 icon */
    public Icon getTinyIcon() {
        if (mTinyIcon != null)
            return mTinyIcon;
        Image image = tufts.vue.gui.GUI.getSystemIconForExtension(getDataType(), 16);
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
        
        Image image = tufts.vue.gui.GUI.getSystemIconForExtension(getDataType(), 32);
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

    /** @return an image to use when dragging this resource */
    public Image getDragImage() {
        Image image = null;
        if (!isLocalFile()) {
            Icon icon = getContentIcon();
            if (icon instanceof ResourceIcon)
                image = ((ResourceIcon)icon).getImage();
        }
        if (image == null)
            image = getLargeIconImage();

        return image;
    }

    /**
     * @return true if the data for this resource is normally obtained by making use the
     * the local file system (including attached network shares)
     *
     * This default impl always returns false.
     */
    public boolean isLocalFile() {
        return false;
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
    

    /**
     * Get preview of the object such as thummbnail / small sized image. Suggested return
     * types are something that can be converted to image data, or a java GUI component,
     * such as a java.awt.Component, javax.swing.JComponent, or javax.swing.Icon.
     */
    public abstract Object getPreview();

//     public static final Object SMALL = "small";
//     public static final Object MEDIUM = "medium";
//     public static final Object LARGE = "large";
//     /**
//      * @param preferredSize: either SMALL, MEDIUM, or LARGE. This is a general hint only and may
//      * not be respected.  If the Resource is image content, 
//      */
//     public abstract Object getPreview(Object preferredSize);

    private boolean isCached;
    
    protected boolean isCached() {
        return isCached;
    }

    // todo: this should be computed internally (move code out of Images.java)
    public void setCached(boolean cached) {
        isCached = cached;
    }
    
    //public abstract void setCached(boolean isCached);

    //public abstract java.io.InputStream getByteStream();
    
//     public abstract void setCacheFile(java.io.File cacheFile);
//     public abstract java.io.File getCacheFile();

    /** if possible, make this Resource relatve to the given root */
    public abstract void makeRelativeTo(URI root);    

    /** @deprecated -- cleanup / remove */
    public void updateRootLocation(URI oldRoot, URI newRoot) {}
    //public abstract void updateRootLocation(URI oldRoot, URI newRoot);

    /**
     *  Return tooltip information, if any.  Basic HTML tags are permitted.
     */
    public String getToolTipText() { return toString(); }

    public String asDebug() {
        return String.format("%s@%07x[%s; %s]",
                             getClass().getSimpleName(),
                             System.identityHashCode(this),
                             TYPE_NAMES[getClientType()],
                             getSpec());
    }

    /** @return true if the given path or filename looks like it probably contains image data in a format we understand
     * This just looks for common extentions (e.g., .gif, .jpg, etc).  This can be applied to filenames, full paths, URL's, etc.
     */
    public static boolean looksLikeImageFile(String path) {
        if (DEBUG.WORK) Log.debug("looksLikeImageFile [" + path + "]");
        String s = path.toLowerCase();
        if    (s.endsWith(".gif")
            || s.endsWith(".jpg")
            || s.endsWith(".jpeg")
            || s.endsWith(".png")
            || s.endsWith(".tif")
            || s.endsWith(".tiff")
            || s.endsWith(".fpx")
            || s.endsWith(".bmp")
            || s.endsWith(".ico")
          
               ) return true;
        return false;
    }
    
    public String toString() {
        return asDebug();
    }

    public Resource clone() {

        // TODO: properties need to be cloned also -- will need
        // to make mProperties non-final though (java limitation)
        
        try {
            final Resource clone = (Resource) super.clone();
            clone.mProperties = mProperties.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
    
//     public String toString() {
//         if (mProperties == null)
//             return getSpec();
//         else
//             return getSpec();+ " " + mProperties;
//     }

    
    protected void out(String s) {
        Log.debug(String.format("%s@%07x: %s", getClass().getSimpleName(), System.identityHashCode(this), s));
    }


    public static File getLocalFileIfPresent(String urlOrPath) {
        if (urlOrPath.startsWith("file:"))
            return new File(urlOrPath.substring(5));
        else
            return getLocalFileIfPresent(makeURL(urlOrPath));
    }
    

    public static File getLocalFileIfPresent(URL url)
    {
        if (url == null || !"file".equals(url.getProtocol()))
            return null;

        if (DEBUG.RESOURCE) dumpURL(url, "getLocalFileIfPresent");

        File file = null;

        if (false) {

            // Sometimes Win32 C:/foo/bar.jpg URI's will wind up with the entire path in
            // the scheme-specifc part, not the path, which will be null, so we have
            // nothing to create the file from.  We could pull the scheme-specific if
            // path is empty if we need to, but for now we're going to try woring with
            // pure URL paths...

            final URI uri = makeURI(url);
            if (uri == null)
                return null;
            if (DEBUG.RESOURCE) dumpURI(uri, "made URI from " + Util.tags(url));
        
            try {
                
                file = new File(uri.getPath());
                if (!file.exists())
                    throw new RuntimeException("doesn't exist: " + file);
                //file = new File(uri);
            } catch (Throwable t) {
                Log.warn("failed to create File from URI " + uri, t);
                dumpURIError(uri, "unable to convert 'file:' URL");
            }
            
        } else {

            // The advantage of URI of URL here is that for paths such as
            // //.host/foo/bar.jpg, ".host" winds up in the URL authority, and the path
            // doesn't contain it, so we can't create a proper File without knowing how
            // to properly prefix the authorty with "//" or however many slashes may be
            // appropriate, and combine with the path using another '/', wheras at least
            // with the URN, the entire thing winds up together in the scheme-specifc
            // part.
            //
            // For example, this is from dumpURL on WinXP: (unlisted URL fields are
            // empty) This example is from a VMWare XP client connecting back to the
            // host.  Similar may apply to network Win32 shares, tho the host will
            // probably start with a regular alphanumeric, instead of '.', in which case
            // everything might automatically wind up in the path -- need to test this.
            //
            //       URL: file://.host/Shared Folders/Images/asciifull.gif
            //  protocol: file
            // authority: .host
            //      host: .host
            //      path: /Shared Folders/Images/asciifull.gif
            //      file: /Shared Folders/Images/asciifull.gif
            
            try {

                if (url.getAuthority() != null) {
                    String fullPath = url.toString();
                    if (!fullPath.startsWith("file:"))
                        throw new IllegalStateException("URL should already have had a file: protocol; " + url);
                    fullPath = fullPath.substring(5);
                    file = new File(fullPath);
                } else {
                    file = new File(url.getPath());
                }

                if (!file.isAbsolute()) {
                    // We could handle checking for relative files (relative to the map)
                    // if we had a ref to the ResourceFactory, and we added a method
                    // there for finding files relative to the map.
                    if (DEBUG.RESOURCE) Log.debug("ignoring non-absolute: " + file);
                    return null;
                }
                
                if (DEBUG.RESOURCE && file != null) dumpFile(file, "getLocalFileIfPresent", false);

                //if (DEBUG.Enabled) Log.debug("got canonical path: " + file.getCanonicalPath());
                
                //if (!file.exists()) throw new RuntimeException("doesn't exist: " + file);

            } catch (Throwable t) {
                Log.warn("failed to create File from URL " + url, t);
                dumpURLError(url, "unable to convert 'file:' URL");
            }
        }
        
        

        if (DEBUG.RESOURCE) Log.debug("got File from URL: " + Util.tags(file) + "; from " + Util.tags(url));
        return file;
    }

    private static final String URL_FILE_PROTOCOL_PREFIX = "file://";
    
    /** If given string is a valid URL, make one and return it, otherwise, return null.
     *
     * @return the new URL -- returned URL's will be fully decoded
     * @see java.net.URLDecoder
     *
     **/
    public static java.net.URL makeURL(final String s)
    {
        try {
            final URI uri = makeURI(s);

            URL url = null;
            String decoded = null;

            try {
                decoded = java.net.URLDecoder.decode(uri.toString(), "UTF-8");
                url = new URL(decoded);
            } catch (Throwable t) {
               Log.info("couldn't make URL from decoded " + (decoded == null ? Util.tags(uri) : decoded), t);
               // URI.toURL leaves the URL in encoded form: local file paths need decoding to be useful to java.io.File
               url = uri.toURL();
           }

            //final URL url = uri.toURL(); 
            //final URL url = new URL(java.net.URLDecoder.decode(uri.toString(), "UTF-8"));

            if (DEBUG.RESOURCE && url != null) dumpURL(url, "MADE URL FROM " + Util.tags(s) + "; via " + Util.tags(uri));

            return url;
            
        } catch (Throwable t) {
            Log.warn("Failed to make URL from: " + s + "; " + t);
            return null;
        }
    }

        
    private static String encodeForURI(String s) {
        s = s.replaceAll(" ", "%20");

        if (s.indexOf('\\') >= 0 && !Util.isWindowsPlatform()) {
            //if (DEBUG.RESOURCE) Log.debug("reversing slashes in " + s);
            Log.warn("reversing slashes in " + s);
            
            // this is of marginal usefulness -- the source URI was presumably created
            // on another platform (and thus machine), referring to a resource we
            // undoubtably won't be able to access, but at least this lets us consistently
            // create URI's.
            
            s = s.replace('\\', '/');
        }

        return s;
        
        //return s.replace(' ', '+'); // no good
    }

    public static URI makeURI(URL url) {

        //URI uri = url.toURI(); // all this does is "new URI(toString())"

        final String encoded = encodeForURI(url.toString());
        URI uri = null;
        try {
            uri = new URI(encoded);
            uri = uri.normalize();
        } catch (Throwable t) {
            Log.debug("URI from " + Util.tags(url), t);
            dumpURL(url);
        }
        return uri;
    }
    
//     public static URI makeURI(File f) {
//         URI uri = f.toURI();
//         if (DEBUG.RESOURCE) dumpURI(uri, "NEW FILE URI FROM " + f);
        
//         Util.printStackTrace("makeURI from " + Util.tags(f) + "; manually checking for /C:");
//         // TODO: this "/C:" check isn't generic enough: is this code even being called anywhere?
        
//         if (uri.getPath().startsWith("/C:"))
//             return makeURI(uri.getPath().substring(3));
//         else
//             return uri;
//     }
    
    public static boolean isLikelyURLorFile(String s) {

        if (s == null)
            return false;

        final char c0 = s.length() > 0 ? s.charAt(0) : 0;
        final char c1 = s.length() > 1 ? s.charAt(1) : 0;
        
        return c0 == '/'
            || c0 == '\\'
            || (Character.isLetter(c0) && c1 == ':')
            || s.startsWith(java.io.File.separator)
            || s.startsWith("http://")
            || s.startsWith("file:")
            ;
    }
    
    public static URI makeURI(String s)
    {
        final char c0 = s.length() > 0 ? s.charAt(0) : 0;
        final char c1 = s.length() > 1 ? s.charAt(1) : 0;
        final String txt;
        
        URI uri = null;

        try {

            if (c0 == '/' || c0 == '\\' || (Character.isLetter(c0) && c1 == ':')) {

                // the above conditions test:
                //  first case: MacOSX / Linux / Unix path
                // second case: Windows path
                //  third case: Windows "C:" style path

                // This URI constructor will auto-encode (fully) the input string.
                // This will include, on unix platforms, encode windows '\' file
                // separators as "%5C".
                
                uri = new java.net.URI("file", s, null); // last argument is fragment: never needed for files
                
                //Util.printStackTrace("makeURI FILE:// -ified: " + txt);
                
            } else {
                
                final String encoded = encodeForURI(s);
                uri = new java.net.URI(encoded);
                if (uri != null)
                    uri = uri.normalize();
                
            }
            
            if (uri != null && uri.getScheme() == null)
                uri = new java.net.URI("file:" + uri.toString());


        } catch (Throwable t) {
            Util.printStackTrace(t, "makeURI: " + s);
        }

        
        if (DEBUG.RESOURCE) {
            if (uri != null) dumpURI(uri, "MADE URI FROM " + Util.tags(s));
            //if (uri != null) dumpURI(uri, "   MADE FROM STRING: " + s);
//             if (uri != null && uri.toString().equals(s))
//                 System.err.println("            MADE URI: " + uri);
//             else
//                 System.err.println("            MADE URI: " + uri + " src=[" + s + "]");
        }
        
        return uri;
    }
    
    public static void dumpURI(URI u) {
        dumpURI(u, null, false);
    }
    public static void dumpURI(URI u, String msg) {
        dumpURI(u, msg, false);
    }
    public static void dumpURIError(URI u, String msg) {
        dumpURI(u, msg, true);
    }
    public static void dumpURI(URI u, String msg, boolean error) {

        final StringWriter buf = new StringWriter(256);
        final PrintWriter w = new PrintWriter(buf);
        
        if (msg == null) msg = "Made URI;";
        
        if (DEBUG.META) writeField(w, "hashCode",       Integer.toHexString(u.hashCode()));

        writeField(w, "scheme",               u.getScheme());
        writeField(w, "scheme-specific",      u.getSchemeSpecificPart(), u.getRawSchemeSpecificPart());
        writeField(w, "authority",            u.getAuthority(), u.getRawAuthority());
        writeField(w, "userInfo",             u.getUserInfo(), u.getRawUserInfo());
        writeField(w, "host",                 u.getHost());
           
        if (u.getPort() != -1)
            writeField(w, "port",	u.getPort());

        writeField(w, "path",         u.getPath(), u.getRawPath());
        writeField(w, "query",        u.getQuery(), u.getRawQuery());
        writeField(w, "fragment",     u.getFragment(), u.getRawFragment());

        String txt = String.format("%s\n%20s: %s %s%s (@%x)%s",
                                   msg,
                                   "URI",
                                   u,
                                   u.isAbsolute() ? "ABSOLUTE" : "RELATIVE",
                                   u.isOpaque() ? " OPAQUE" : "",
                                   System.identityHashCode(u),
                                   buf.getBuffer()
                                   );
        if (error)
            Log.error(txt);
        else
            Log.debug(txt);
        
        //System.out.println("len=" + buf.getBuffer().length());
    }

    public static void dumpFile(File u, String msg, boolean error) {

        if (msg == null) msg = "Made File;";
        
        final StringWriter buf = new StringWriter(256);
        final PrintWriter w = new PrintWriter(buf);
            
        if (DEBUG.META) writeField(w, "hashCode",       Integer.toHexString(u.hashCode()));
        writeField(w, "path",            u.getPath());
        writeField(w, "absolutePath",    u.getAbsolutePath());
        try {
            writeField(w, "canonicalPath",   u.getCanonicalPath());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        writeField(w, "name",       u.getName());
        writeField(w, "parent",       u.getParent());
        writeField(w, "isAbsolute",       u.isAbsolute());
        writeField(w, "isNormalFile",       u.isFile());
        writeField(w, "isDirectory",       u.isDirectory());

        String txt = String.format("%s\n%20s: %s (@%x)%s",
                                   msg,
                                   "File",
                                   u,
                                   System.identityHashCode(u),
                                   buf.getBuffer()
                                   );
        
        if (error)
            Log.error(txt);
        else
            Log.debug(txt);
        
        //System.out.println("len=" + buf.getBuffer().length());
    }
    
    public static void dumpURL(URL u, String msg, boolean error) {

        if (msg == null) msg = "Made URL;";
        
        final StringWriter buf = new StringWriter(256);
        final PrintWriter w = new PrintWriter(buf);
            
        if (DEBUG.META) writeField(w, "hashCode",       Integer.toHexString(u.hashCode()));
        writeField(w, "protocol",       u.getProtocol());
        writeField(w, "userInfo",       u.getUserInfo());
        writeField(w, "authority",      u.getAuthority());
        writeField(w, "host",           u.getHost());
        if (u.getPort() != -1)
            writeField(w, "port",	u.getPort());
        writeField(w, "path",           u.getPath());
        writeField(w, "file",           u.getFile());
        writeField(w, "query",          u.getQuery());
        writeField(w, "ref",            u.getRef());

        String txt = String.format("%s\n%20s: %s (@%x)%s",
                                   msg,
                                   "URL",
                                   u,
                                   System.identityHashCode(u),
                                   buf.getBuffer()
                                   );

        //System.out.println("len=" + buf.getBuffer().length());
        
        if (error)
            Log.error(txt);
        else
            Log.debug(txt);
    }
    
    public static void dumpURL(URL u, String msg) {
        dumpURL(u, msg, false);
    }
    
    public static void dumpURLError(URL u, String msg) {
        dumpURL(u, msg, true);
    }

    public static void dumpURL(URL u) {
        dumpURL(u, null);
    }


    private static void writeField(PrintWriter w, String label, Object value) {
        if (value != null && !(value instanceof String && value.toString().length() == 0))
            w.printf("\n%20s: %s", label, value);
        //System.out.format("%20s: %s\n", label, value);
    }
    
    private static void writeField(PrintWriter w, String label, Object value, Object rawValue) {
        writeField(w, label, value);

        if (value != null && !value.equals(rawValue) || rawValue == null && value != null)
            writeField(w, "RAW-" +label, rawValue);
    }
    
}

// class FileResource extends Resource {
//     final java.io.File file;
//     FileResource(java.io.File file) {
//         this.file = file;
//     }

// //     public String getPrettyString() {
// //         return file.getName();
// //     }
// }
