 /*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

 *  The Resource abstract class defines a set of methods which all VUE Resource objects
 *  must implement, and provides basic common functionality to all Resource types.
 *  This class create a uniform way to handle dragging and dropping of resource
 *  objects, displaying their content, and fetching their data.

 *
 * @version $Revision: 1.81 $ / $Date: 2008-07-16 15:17:33 $ / $Author: sfraize $
 */

public abstract class Resource implements Cloneable
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Resource.class);

    /** property keys with this prefix will be persisted, but are not meant for display to users */
    public static final String HIDDEN_PREFIX = "@";
    /** property keys with this prefix are for runtime display only: will not be persisted */
    public static final String RUNTIME_PREFIX = "~"; 
    /** property keys with this prefix are for internal runtime use only: will not be displayed or persisted */
    public static final String HIDDEN_RUNTIME_PREFIX = "@@"; 
    /** property keys with this prefix are both hidden and runtime-only */
    public static final String DEBUG_PREFIX = "##";
    
    public static final String PACKAGE_FILE = HIDDEN_RUNTIME_PREFIX + "package.file";
    public static final String PACKAGE_KEY_DEPRECATED = HIDDEN_PREFIX + "Packaged";
    //public static final String PACKAGE_FILE = DEBUG_PREFIX + "package.file";

    public static final String PACKAGE_ARCHIVE = RUNTIME_PREFIX + "Package";
    
    
    // Some standard property names
    public static final String CONTENT_SIZE = "Content.size";
    public static final String CONTENT_TYPE = "Content.type";
    public static final String CONTENT_MODIFIED = "Content.modified";
    public static final String CONTENT_ASOF = "Content.asOf";
    public static final String CONTENT_SOURCE = "Content.source";
    
    public static final String IMAGE_FORMAT = "image.format";
    public static final String IMAGE_WIDTH = "image.width";
    public static final String IMAGE_HEIGHT = "image.height";

    public static final Object MANAGED_UNMARSHALLING = "MANAGED-UNMARSHALLING";
    

    public static final java.awt.datatransfer.DataFlavor DataFlavor =
        tufts.vue.gui.GUI.makeDataFlavor(Resource.class);

    public static final String SPEC_UNSET = "<spec-unset>";

    public static boolean isHiddenPropertyKey(String key) {
        char c = 0;
        try {
            c = key.charAt(0);
        } catch (Throwable t) {}
               
        return c == '@' || c == '#';
    }


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
            if (DEBUG.RESOURCE) System.out.println(""); // for spacing out large sets of search results
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
        if (DEBUG.RESOURCE) dumpField("setByteSize", size);
        mByteSize = size;
    }
    
    
    /**
     * Set the given property value.
     * Does nothing if either key or value is null, or value is an empty String.
     */
    public void setProperty(String key, Object value) {

        if (key == null)
            return;

        if (value != null) {
            if (DEBUG.DATA) dumpKV("setProperty", key, value);
            if (!(value instanceof String && ((String)value).length() < 1))
                mProperties.put(key, value);
        } else {
            if (DEBUG.Enabled) {
                if (mProperties.containsKey(key)) {
                    Object o = mProperties.get(key);
                    dumpKV("setProperty(null)overwite?", key, o);
                }
            }
        }
    }

    public void setProperty(String key, long value) {
//         if (key.endsWith(".contentLength") || key.endsWith(".size")) {
//             // this is a hack to handle HTTP header info
//             setByteSize(value);
//         }
        setProperty(key, Long.toString(value));
    }

    
    protected void dumpField(String name, Object value) {
        out(String.format("%-31s: %s%s%s", name, Util.TERM_CYAN, Util.tags(value), Util.TERM_CLEAR));
    }

    private void dumpKV(String name, String key, Object value) {
        out(String.format("%-14s%17s: %s%s%s", name, key, Util.TERM_RED, Util.tags(value), Util.TERM_CLEAR));
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
        Object o = mProperties.remove(key);
        if (DEBUG.DATA && o != null) dumpKV("removeProperty", key, o);
        return o;
    }

    /**
     * Add a property with the given key.  If a key already exists
     * with this name, the key will be modified with an index.
     */
    public String addProperty(String desiredKey, Object value) {
        if (DEBUG.DATA) dumpKV("addProperty", desiredKey, value);
        return mProperties.addProperty(desiredKey, value);
    }

    public String addPropertyIfContent(String desiredKey, Object value) {
        if (DEBUG.DATA) dumpKV("addPropertyIf", desiredKey, value);
        return mProperties.addIfContent(desiredKey, value);
    }
    
    
    public Object getPropertyValue(String key) {
        final Object value = mProperties.getValue(key);
        if (DEBUG.DATA && value != null) dumpKV("getProperty", key, value);
        return value;
    }
    
    /**
     * This method returns a value for the given property name.
     * @param pName the property name.
     * @return Object the value
     **/
    public String getProperty(String key) {
        final Object value = getPropertyValue(key);
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
        if (DEBUG.RESOURCE) setDebugProperty("isImage", ""+ asImage);
    }

    /** init pass to run after de-serialization (optional until we know we want to keep the resource) */
    protected void initAfterDeserialize(Object context) {}
    /** final init pass to run after de-serialization (optional until we know we want to keep the resource) */
    protected void initFinal(Object context) {}

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

    /** @return a current local File, if there is one, that contains the data for this object.
     * E.g., could return an original local data file, an http: cache file, etc.
     * Return null if no such file exists.
     */
    protected File mDataFile;
    public File getActiveDataFile() {

        File sourceFile;
        
        if (mDataFile != null) {
            sourceFile = mDataFile;
        } else if (isImage()) {
            sourceFile = Images.findCacheFile(this);
        } else {
            sourceFile = null;
        }

        return sourceFile;
    }

    public void flushCache() {
        try {
            Images.flushCache(getActiveDataFile());
        } catch (Throwable t) {
            Log.warn(this, t);
        }
    }
    
    
    private boolean isCached;
    protected boolean isCached() {
        return isCached;
    }

    // todo: this should be computed internally (move code out of Images.java)
    public void setCached(boolean cached) {
        isCached = cached;
    }

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
    // todo: replace with more abstract call(s) -- as of packaging, callers
    // don't generally just want the "spec", they want a local file if we
    // have one (including a possible redirect to a package file) or a URL
    // reference, with different priorities in different cases.
    public abstract String getSpec();

//     /**
//      * If a reference to this resource can be provided as a URL, return it in that form,
//      * otherwise return null.
//      *
//      * @return default Resource class impl: returns null
//      */
//     public java.net.URL asURL() {
//         return null;
//     }


//     /**
//      * All Resource impls should be able to return something that fits into a URI.
//      */
//     public abstract java.net.URI toURI();
    
    
    /** 
     * Return the resource type.  This should be one of the types defined above.
     */
    public int getClientType() {
        return mType;
    }
    
    /** TODO:  need to remove this -- if we keep any type at all, it should at least be inferred
     * -- probably replace with a setClientType(Object) -- a general marker that clients / UI components can use
     */
    public void setClientType(int type) {
        mType = type;
        if (DEBUG.RESOURCE){
            if (DEBUG.META) dumpField("setClientType", TYPE_NAMES[type] + " (" + type + ")");
            try {
                setDebugProperty("clientType", TYPE_NAMES[type] + " (" + type + ")");
            } catch (Throwable t) {
                Log.warn(this + "; setClientType " + type, t);
            }
        }
    }
    
    /**
     *  Display the content associated with the resource.  For example, call
     *  VueUtil.open() using the spec information.
     */
    public abstract void displayContent();

    protected String mExtension = null;

    public void reset() {
        mExtension = null;
    }

    public void setDataType(String s) {
        mExtension = s;
    }
    
    private static final String NO_EXTENSION = "<no-ext>";
    public static final String EXTENSION_DIR = "dir";
    public static final String EXTENSION_HTTP = "web";
    public static final String EXTENSION_UNKNOWN = "---";
    public static final String EXTENSION_VUE  = "vue";

    /**
     * Return a filename extension / file type of this resource (if any) suitable for identify it
     * it's "type" to the local file system shell environement (e.g., txt, html, jpg).
     */
    public final String getDataType() {

        if (mExtension == null) {

            // This code is safe to run more than once / in multiple
            // threads as the cached result should always be the same,
            // so we don't need to synchronized this lazy-eval code.
            
            mExtension = determineDataType();
            
            if (DEBUG.RESOURCE) setDebugProperty("dataType", mExtension);
        }
        
        return mExtension;
    }
        
    protected String determineDataType()
    {
        String ext;

        if (getClientType() == DIRECTORY)
            ext = EXTENSION_DIR;
        else
            ext = extractExtension();
            
        if (ext == null || ext == NO_EXTENSION || ext.length() == 0) {
            final String spec = getSpec();
            if (spec == null || spec == SPEC_UNSET || spec.trim().length() < 1)
                ext = EXTENSION_UNKNOWN;
            else if (spec.startsWith("http:") || spec.startsWith("https:")) 
                ext = EXTENSION_HTTP;
            else if (getClientType() == Resource.FILE)
                ext = "txt";
            else
                ext = EXTENSION_DIR;
        } else {
            ext = ext.toLowerCase();
            
            if ("readme".equals(ext) || "msg".equals(ext)) {
                ext = "txt";
            } else if (getClientType() == URL || getClientType() >= ASSET_OKIDR) {
                if ("asp".equals(ext) || // microsoft web page
                    "php".equals(ext) ||
                    "pl".equals(ext) // commonly: a perl script
                    ) {
                    ext = EXTENSION_HTTP;
                }
            }
        }

        
        return ext;
    }

    protected String extractExtension() {
       return extractExtension(getSpec());            
    }

    /** @return the likely extension for the given string, or NO_EXTENSION if none found */
    protected String extractExtension(final String s) {

        String ext = NO_EXTENSION;
        
        final char lastChar;

        if (s == null || s.length() == 0)
            lastChar = 0;
        else
            lastChar = s.charAt(s.length()-1);
        
        if (lastChar == 0) {
            // default NO_EXTENSION
        } else if ( ! Character.isLetterOrDigit(lastChar)) {
            // assume some kind of a path element (e.g., /, \): e.g.: no extension available
        } else {
            final int lastDotIdx = s.lastIndexOf('.');
        
            // must have at least one char's worth of file-name, and one two chars worth of data after the dot
            if (lastDotIdx > 1 && (s.length() - lastDotIdx) > 2) {
                String extTest = s.substring(lastDotIdx + 1);

                // if first character of extension is not a letter or
                // digit, assome we have NOT found a real extension
                
                if (Character.isLetterOrDigit(extTest.charAt(0)))
                    ext = extTest;
            } 
        }

        if (DEBUG.RESOURCE) Log.debug("extractExtension " + this + "; [" + s + "] = [" + ext + "]");

        return ext;
    }

//     // was getExtension
//     // TODO: cache / allow setting (e.g. special data sources might be able to indicate type that's otherwise unclear
//     // e.g., a URL query part that requests "type=jpeg")
//     public String getDataType() {
// //         String type = null;
// //         if (getClientType() == DIRECTORY)
// //             type = EXTENSION_DIR;
// //         else
// //             type = extractExtension(getSpec());

//         String ext = extractExtension(getSpec());

//         if (ext == null || ext == NO_EXTENSION || ext.length() == 0) {
//             final String spec = getSpec();
//             if (spec == null || spec == SPEC_UNSET || spec.trim().length() < 1)
//                 ext = EXTENSION_UNKNOWN;
//             else if (spec.startsWith("http:")) // todo: https, etc...
//                 ext = EXTENSION_HTTP;
//             else
//                 ext = EXTENSION_DIR;
//         }
        
// //         if (type == NO_EXTENSION) {
// //             if (getClientType() == FILE) {
// //                 // todo: this really ought to be in a FileResource and/or a useful osid filing impl
// //                 type = EXTENSION_DIR;
// //                 // assume a directory for now...
// //             } 
// //         }

//         if (DEBUG.RESOURCE) out("extType=[" + ext + "] in " + this);
//         //if (DEBUG.RESOURCE) out(getSpec() + "; extType=[" + ext + "] in [" + this + "] type=" + TYPE_NAMES[getClientType()]);
//         return ext;
//     }
    


    private ImageIcon mTinyIcon;
    /** @return a 16x16 icon */
    public Icon getTinyIcon() {
        if (mTinyIcon == null)
            mTinyIcon = makeIcon(16, 16);
        return mTinyIcon;
    }


    private ImageIcon mLargeIcon;
    /** @return up to a 128x128 icon */
    public Icon getLargeIcon() {
        if (mLargeIcon == null)
            mLargeIcon = makeIcon(32, 128);
        return mLargeIcon;
    }

    private ImageIcon makeIcon(int size, int max)
    {
        final String ext = getDataType();
        Image image = tufts.vue.gui.GUI.getSystemIconForExtension(ext, size);
        
        if (image != null) {
            if (image.getWidth(null) > max) {
                // see http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
                // on how to do this better/faster -- happens very rarely on the Mac tho, but need to test PC.
                Log.warn("scaling for dataType [" + ext + "]: " + Util.tags(image) + "; from " + image.getWidth(null) + "x" + image.getHeight(null));
                image = image.getScaledInstance(max, max, 0); //Image.SCALE_SMOOTH);
            }
            return new javax.swing.ImageIcon(image);
        } else
            return null;
    }

    /** @return the image for our large icon if we have one, null otherwise */
    public Image getLargeIconImage() {
        if (mLargeIcon == null)
            getLargeIcon();
        if (mLargeIcon != null)
            return mLargeIcon.getImage();
        else
            return null;
    }
    public Image getTinyIconImage() {
        if (mTinyIcon == null)
            getTinyIcon();
        if (mTinyIcon != null)
            return mTinyIcon.getImage();
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

    /**
     * @return true if the data for this resource is normally obtained by making use the
     * the local file system (including attached network shares)
     *
     * This default impl always returns false.
     */
    public boolean isLocalFile() {
        return false;
    }
    
    public boolean isPackaged() {
        return hasProperty(PACKAGE_FILE);
    }
    
//     public abstract java.io.InputStream getByteStream();
//     public abstract void setCacheFile(java.io.File cacheFile);
//     public abstract java.io.File getCacheFile();

    /** if this resource is relative to the given root, record this in the resource in a persistant way */
    public abstract void recordRelativeTo(URI root);
    
    //public abstract void updateIfRelativeTo(URI root);    

    /** if this resource was relative when it was persisted, see if it can be found relative to the new location */
    public abstract void restoreRelativeTo(URI root);    

//     /** @eprecated - if possible, make this Resource relatve to the given root */
//     public abstract void makeRelativeTo(URI root);    
//     /** @eprecated -- cleanup / remove */
//     public void updateRootLocation(URI oldRoot, URI newRoot) {}
//     //public abstract void updateRootLocation(URI oldRoot, URI newRoot);

    /**
     *  Return tooltip information, if any.  Basic HTML tags are permitted.
     */
    public String getToolTipText() { return toString(); }

    protected String paramString() {
        return "";
    }

    private static final String _fmt0 = "%s@%07x[%s; %s%s]";
    private static final String _fmt1 = "%s@%08x[%s; %s%s]";
    private static final String _debugFmt = Util.getJavaVersion() > 1.5 ? _fmt1 : _fmt0;

    public String asDebug() {
        return String.format(_debugFmt,
                             getClass().getSimpleName(),
                             System.identityHashCode(this),
                             TYPE_NAMES[getClientType()],
                             paramString(),
                             mDataFile == null ? getSpec() : Util.tags(mDataFile)
                             //getLocationName() // may trigger property fetches during debug which is very messy
                             //(mDataFile != null && hasProperty(PACKAGE_FILE)) ? mDataFile.getName() : getSpec()
                             );
    }

    public String getLocationName() {
        return getSpec();
    }
    
    public String getDescription() {
        return getSpec();
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
    
    
    protected void out(String s) {
        Log.debug(String.format("%s@%07x: %s", getClass().getSimpleName(), System.identityHashCode(this), s));
    }
    
    protected void out_info(String s) {
         Log.info(String.format("%s@%07x: %s", getClass().getSimpleName(), System.identityHashCode(this), s));
    }
    
    protected void out_warn(String s) {
         Log.warn(String.format("%s@%07x: %s", getClass().getSimpleName(), System.identityHashCode(this), s));
    }
    
    protected void out_error(String s) {
        Log.error(String.format("%s@%07x: %s", getClass().getSimpleName(), System.identityHashCode(this), s));
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

    //public static boolean isLikelyURLorFile(String s) {
    public static boolean looksLikeURLorFile(String s) {

        if (s == null)
            return false;

        final char c0 = s.length() > 0 ? s.charAt(0) : 0;
        final char c1 = s.length() > 1 ? s.charAt(1) : 0;
        
        return c0 == '/'
            || c0 == '\\'
            || (Character.isLetter(c0) && c1 == ':') // Windows style C:\path\file
            || s.startsWith(java.io.File.separator)
            || s.startsWith("http://")
            || s.startsWith("file:")
            ;
    }

    /**
     * @return true if the given string looks like it MAY represent a file on the local file system,
     * such that the given string would successfully init a java.io.File object (even if the file doesn't exist)
     */
    public static boolean looksLikeLocalFilePath(String s) {

        if (s == null)
            return false;

        final char c0 = s.length() > 0 ? s.charAt(0) : 0;

        if (Util.isWindowsPlatform()) {
        
            final char c1 = s.length() > 1 ? s.charAt(1) : 0;
            
            return c0 == '/'
                || c0 == '\\'
                || (Character.isLetter(c0) && c1 == ':') // Windows style C:\path\file
                || s.startsWith(java.io.File.separator)
                //|| s.startsWith("file:")
                ;
            
        } else {

            return c0 == '/'
                || s.startsWith(java.io.File.separator)
                //|| s.startsWith("file:")
                ;
            
        }

    }

    public static String toCanonical(File file)
    {
        String canonical = null;

        try {
            canonical = file.getCanonicalPath();
        } catch (Throwable t) { Log.warn(file, t); }

        return canonical == null ? file.getAbsolutePath() : canonical;
    }
    
    public static File toCanonicalFile(File file)
    {
        String canonical = toCanonical(file);

        if (file.getPath().equals(canonical))
            return file;
        else
            return new File(canonical);
    }
    

    /** @return a File object if one can be found (and if it exists) */
    public static File getLocalFileIfPresent(String urlOrPath) {

        // todo: make semantics determinisitic: should this only return files
        // that already exist or not?  
        
        File file = null;

        if (urlOrPath.startsWith("#")) { 
            return null; // RDF code sometimes hands us these
        }
        else if (urlOrPath.startsWith("file:")) {
            
            file = new File(urlOrPath.substring(5));
            
        } else if (looksLikeLocalFilePath(urlOrPath)) {
            
            file = new File(urlOrPath);
            
        } 

        //if (DEBUG.IO && file != null) Log.debug("getLocalFileIfPresent(Str): testing " + file);
        //if (file == null || !file.exists())
        //    file = getLocalFileIfPresent(makeURL(urlOrPath));

        if (file == null) {
            file = getLocalFileIfPresent(makeURL(urlOrPath));
        } else {
            if (DEBUG.IO) Log.debug(Util.tags(file) + "; GLFIP testing");
            if (!file.exists()) 
                file = getLocalFileIfPresent(makeURL(urlOrPath));
        }
        
        return file;
    }
    

    // this will only return files that already exist
    public static File getLocalFileIfPresent(URL url)
    {
        if (url == null || !"file".equals(url.getProtocol()))
            return null;

        if (DEBUG.RESOURCE) dumpURL(url, "getLocalFileIfPresent; from:");

        File file = null;

        if (false) {

//             // Sometimes Win32 C:/foo/bar.jpg URI's will wind up with the entire path in
//             // the scheme-specifc part, not the path, which will be null, so we have
//             // nothing to create the file from.  We could pull the scheme-specific if
//             // path is empty if we need to, but for now we're going to try woring with
//             // pure URL paths...

//             final URI uri = makeURI(url);
//             if (uri == null)
//                 return null;
//             if (DEBUG.RESOURCE) dumpURI(uri, "made URI from " + Util.tags(url));
        
//             try {
                
//                 file = new File(uri.getPath());
//                 if (!file.exists())
//                     throw new RuntimeException("doesn't exist: " + file);
//                 //file = new File(uri);
//             } catch (Throwable t) {
//                 Log.warn("failed to create File from URI " + uri, t);
//                 dumpURIError(uri, "unable to convert 'file:' URL");
//             }
            
        } else {

            // The advantage of URI over URL here is that for paths such as
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

                if (DEBUG.RESOURCE && file != null) dumpFile(file);

                if (!file.isAbsolute()) {
                    // We could handle checking for relative files (relative to the map)
                    // if we had a ref to the ResourceFactory, and we added a method
                    // there for finding files relative to the map.
                    if (DEBUG.Enabled) Log.debug("ignoring non-absolute: " + Util.tags(file));
                    return null;
                }
                

                if (DEBUG.IO) Log.debug(Util.tags(file) + "; getLocalFileIfPresent(URL): testing");
                if (!file.exists()) {
                    Log.info(Util.tags(file) + "; ignoring non-existent");
                    return null;
                }
                

                //if (DEBUG.Enabled) Log.debug("got canonical path: " + file.getCanonicalPath());
                
                //if (!file.exists()) throw new RuntimeException("doesn't exist: " + file);

            } catch (Throwable t) {
                Log.warn("failed to create File from URL " + url, t);
                dumpURLError(url, "unable to convert 'file:' URL");
            }
        }
        
        

        //if (DEBUG.RESOURCE) Log.debug("got File from URL: " + Util.tags(file) + "; from " + Util.tags(url));
        return file;
    }

    protected static String encodeForURL(String s)
    //throws java.io.UnsupportedEncodingException
    {
        //String encoded;

        try {
            //encoded = java.net.URLEncoder.encode(s, "UTF-8");
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Throwable t) {
            Log.error("Failed to encode [" + s + "]", t);
            return java.net.URLEncoder.encode(s);
        }
        //return encoded;
    }

// //     public static String URLEncode(URI uri)
// //     }

    
    protected static String decodeURI(String s)
        throws java.io.UnsupportedEncodingException
    {
        String decoded = java.net.URLDecoder.decode(s, "UTF-8");
        return decoded;
    }
    
    protected static String decodeForURL(String s)
        throws java.io.UnsupportedEncodingException
    {
        return decodeURI(s);
    }

    private static String encodeForURI(String s) {

        //if (true) return s;
        
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

    protected static String decodeForFile(String s)
    {
        String decoded = s;
        try {
            decoded = decodeURI(s);
        } catch (Throwable t) {
            Log.warn("decodeForFile: " + t + "; " + s);
        }
        return decoded;
    }

    protected static File toFile(URI uri)
    {
        return new File(uri.getPath());
    }
    
    


    /** @return a URL for the given file, otherwise null */
    public static java.net.URL makeURL(final File file)
    {
        try {
            return file.toURL();
        } catch (Throwable t) {
            Log.warn(Util.tags(file) + " failed to convert itself to a URL", new Throwable());
            return makeURL(file.toString());
        }
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

            URL url = null;

            try {
                url = new URL(s);
            } catch (java.net.MalformedURLException e) {
                if (DEBUG.Enabled) Log.info("makeURL: " + s + "; " + e);
            }

            if (url != null)
                return url;

            final URI uri = makeURI(s);

            String decoded = null;
            
            try {
                //decoded = URLDecode(uri.toString());
                decoded = java.net.URLDecoder.decode(uri.toString(), "UTF-8");
                //decoded = replaceAll("+", 
                url = new URL(decoded);
            } catch (Throwable t) {
                String msg = "couldn't make URL from decoded URI: " + Util.tags(decoded == null ? uri : decoded);
                if (DEBUG.Enabled)
                    Log.info(msg, t);
                else
                    Log.info(msg + "; " + t);

                //if (decoded != null && !decoded.equals(uri.toString())
                
                // URI.toURL leaves the URL in encoded form: local file paths need decoding to be useful to java.io.File
                url = uri.toURL();
           }

            //final URL url = uri.toURL(); 
            //final URL url = new URL(java.net.URLDecoder.decode(uri.toString(), "UTF-8"));

            //if (DEBUG.RESOURCE && url != null) dumpURL(url, "MADE URL FROM " + Util.tags(s) + "; via " + Util.tags(uri));
            if (DEBUG.Enabled && url != null) dumpURL(url, "Made URL FROM " + Util.tags(uri));

            return url;
            
        } catch (Throwable t) {
            Log.warn("couldn't make URL from: " + Util.tags(s) + "; " + t);
            return null;
        }
    }

        
    public static URI makeURI(String s)
    {
        final char c0 = s.length() > 0 ? s.charAt(0) : 0;
        final char c1 = s.length() > 1 ? s.charAt(1) : 0;
        final String txt;
        
        URI uri = null;

        try {

            if (c0 == '#' || s.startsWith("rdf:#")) {
                
                // Our current RDF code sometimes tries to make Resources from random
                // non string fragments, which will always fail to create a URI unless
                // both a URI scheme and a URI scheme-specific-part are also specified.

                final String schemeSpecific = "fragment";
                
                if (c0 == '#')
                    uri = new java.net.URI("rdf", schemeSpecific, s.substring(1));
                else
                    uri = new java.net.URI("rdf", schemeSpecific, s.substring(5));
                Log.warn("makeURI: guessed at creating " + Util.tags(uri) + " from " + Util.tags(s));
            }
            else if (c0 == '/' || c0 == '\\' || (Character.isLetter(c0) && c1 == ':')) {

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
            Util.printStackTrace(t, "makeURI: " + Util.tags(s));
        }

        
        if (DEBUG.RESOURCE) {
            if (uri != null) dumpURI(uri, "Made URI FROM " + Util.tags(s));
            //if (uri != null) dumpURI(uri, "   MADE FROM STRING: " + s);
//             if (uri != null && uri.toString().equals(s))
//                 System.err.println("            MADE URI: " + uri);
//             else
//                 System.err.println("            MADE URI: " + uri + " src=[" + s + "]");
        }
        
        return uri;
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
    
    protected static Object debugURI(String s) {
        try {
            return new URI(s);
        } catch (Throwable t) {
            return t;
            //return t.toString() + "; " + Util.tags(s);
        }
    }
    protected static String debugURL(String s) {
        try {
            return new URL(s).toString();
        } catch (Throwable t) {
            return t.toString();
            //return t.toString() + "; " + Util.tags(s);
        }
    }
    
    

    public static boolean canDump(Object o) {
        return o instanceof File || o instanceof URL || o instanceof URI;
    }

    public static String getDump(Object o) {
        return getDump(o, null);
    }
    
    /** @return a dump of either a URI, URL or File object */
    public static String getDump(Object o, String msg)
    {
        final StringWriter buf = new StringWriter(256);
        final PrintWriter w = new PrintWriter(buf);
        
        if (msg != null) w.println(msg);
        
        if (o instanceof URI)
            writeURI(w, (URI) o, msg);
        else if (o instanceof URL)
            writeURL(w, (URL) o, msg);
        else if (o instanceof File)
            writeFile(w, (File) o, msg);
        else
            w.print("\tResource: unhandled getDump for object of type: " + Util.tags(o));

        return buf.toString();
    }
    
    private static void dumpOut(Object o, String msg, boolean toError)
    {
        if (msg == null) msg = Util.TERM_RED + "Made " + o.getClass().getName() + ";" + Util.TERM_CLEAR;

        final String txt = getDump(o, msg);

        if (toError)
            Log.error(txt);
        else
            Log.debug(txt);
    }

    public static void dumpURI(URI u, String msg, boolean error) {
        dumpOut(u, msg, error);
    }
    public static void dumpURI(URI u, String msg) {
        dumpURI(u, msg, false);
    }
    public static void dumpURIError(URI u, String msg) {
        dumpURI(u, msg, true);
    }
    public static void dumpURI(URI u) {
        dumpURI(u, null, false);
    }

    public static void dumpURL(URL u, String msg, boolean error) {
        dumpOut(u, msg, error);        
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
    


    public static void dumpFile(File u, String msg, boolean error) {
        dumpOut(u, msg, error);
    }
    public static void dumpFile(File u) {
        dumpFile(u, null, false);
    }
    
    
//     public static void dumpURI(URI u, String msg, boolean error) {
//         final StringWriter buf = new StringWriter(256);
//         final PrintWriter w = new PrintWriter(buf);
        
//         if (msg == null) msg = "Made URI;";
//     }
    
    private static void writeURI(PrintWriter w, URI u, String msg) {
        
        w.printf("%20s: %s (@%x) %s %s",
                 "URI",
                 u,
                 System.identityHashCode(u),
                 u.isAbsolute() ? "ABSOLUTE" : "RELATIVE",
                 u.isOpaque() ? "OPAQUE" : ""
                 );

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
    }
    
    
    private static void writeFile(PrintWriter w, File u, String msg)
    {
        w.printf("%20s: %s (@%x)", "File", u, System.identityHashCode(u));

        if (DEBUG.META) writeField(w, "hashCode",       Integer.toHexString(u.hashCode()));
        writeField(w, "path",            u.getPath());
        writeField(w, "absolutePath",    u.getAbsolutePath());

        try {
            writeField(w, "canonicalPath",   u.getCanonicalPath());
        } catch (Throwable t) { t.printStackTrace(); }
        try {
            writeField(w, "toURI",   u.toURI());
        } catch (Throwable t) { t.printStackTrace(); }
        try {
            writeField(w, "toURL",   u.toURL());
        } catch (Throwable t) { t.printStackTrace(); }
        
        writeField(w, "name",       u.getName());
        writeField(w, "parent",       u.getParent());
        writeField(w, "isAbsolute",       u.isAbsolute());
        writeField(w, "isNormalFile",       u.isFile());
        writeField(w, "isDirectory",       u.isDirectory());
        writeField(w, "exists",       u.exists());
        writeField(w, "canRead",       u.canRead());
    }

    
    private static void writeURL(PrintWriter w, URL u, String msg)
    {
        w.printf("%20s: %s (@%x)", "URL", u, System.identityHashCode(u));
        
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

