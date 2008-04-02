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

import java.util.*;

import tufts.Util;
import static tufts.Util.*;
import tufts.vue.gui.GUI;

import java.net.*;
import java.io.*;
import java.util.regex.*;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

// TODO: tempting to split out a separate FileResource subclass of Resource to handle
// stuff that is just the local file system where code digging up local filesystem icons
// could reside, and we'd only need a single spec / file object (not all this crazy URL
// stuff).  Tho the icon-type stuff is actually generic to network resources: e.g., an
// .xls file on a remote server would still be nice if it showed the Excel icon...  Tho
// that code can probably stay residing in GUI, and doing this would still simplify lots
// of stuff.  E.g., the C:\Program stuff when it shows up on mac, the handling of ':' ->
// '/' for mac file names.  Oh, and of course, all of the local file resolution could be
// handled just in FileResource, tho technically, that could also be useful if the
// content was residing on a server, and it's location changed...

// Since FTP Resources are handled by CabinetResource, maybe we can also get away
// with making this an HttpResource... is there anything else this needs to do?
// What about FTP urls dragged from web browsers?  Well, they ought to be going
// thru a factory for creation anyway so we can do what we want -- they're
// probably creating URLResources right now tho...

// Biggest problem w/FileResource: still need URLResource support for files due to old
// save files, unless change to a model of using restored resources only for the
// data-portion, and recreate new (potentially atomic) resources after the data has been
// restored.  (Yet another reason to choose something more flexible than castor for XML
// persistance -- e.g. XStream)


/**
 * The Resource impl handles references to local files or single URL's, as well as
 * any underlying type of asset (OSID or not) that can obtain it's various parts via URL's.
 *
 * An "asset" is defined very generically as anything, that given some kind basic
 * key/meta-data (e.g., a file name, a URL, etc), can at some later point,
 * reliably and repeatably convert that name/key to underlyling data of interest.
 * This is basically what the Resource interface was created to handle.
 *
 * An "Asset" is a proper org.osid.repository.Asset.
 *
 * When this class is used for an asset with parts (e..g, Osid2AssetResource), it should
 * also be what allows us to completely throw away any underlying
 * org.osid.repository.Asset (using it only as a paramatizer for what is really a
 * factory constructor: should covert to that), because all the assets can be had via
 * URL's, and we've extracted the relvant information at construction time.  If the
 * asset part(s) CANNOT be accessed via URL, then we need a real, new subclass of
 * URLResource that handles the non URL cases, or even just a raw implementor of
 * Resource, if all the asset-parts need special I/O (e.g., non HTTP network traffic),
 * to be obtained.
 *
 * @version $Revision: 1.56 $ / $Date: 2008-04-02 03:19:15 $ / $Author: sfraize $
 */

public class URLResource extends Resource implements XMLUnmarshalListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(URLResource.class);
    
    //private static final String BROWSE_KEY = "@Browse";
    private static final String IMAGE_KEY = "@Image";
    private static final String THUMB_KEY = "@Thumb";

    //static final long SIZE_UNKNOWN = -1;
    
    //private long size = SIZE_UNKNOWN;
    private String spec = SPEC_UNSET;
    //private JComponent preview;
    //private tufts.vue.ui.ResourceIcon mIcon;
    //private Object mPreview;
    private boolean isImage;

    private URI mRelativeURI; // right now, I think this only used if it's RELATIVE -- kind of a marker for a short-name
    
    // TODO performance: store as strings or URI's and only do conversion when we ask for them.
    protected URL mURL_Browse;
    private URL mURL_Thumb;
    private URL mURL_Image;
    
    ///** if non-null, is local file */ // todo: FileResource...
    //private File mFile;
    
    /** an optional resource title */
    protected String mTitle;
    

    private boolean mXMLrestoreUnderway = false;
    private ArrayList<PropertyEntry> mXMLpropertyList;
    
    // for castor to save and restore
    
    // we REALLY need to get rid of this constructor: all the code
    // expects SPEC to be non-null!  Castor won't let us though...
    public URLResource() {
        init();
    }

    private URLResource(String spec) {
        init();
        setSpec(spec);
    }
    
//     private URLResource(URL url) {
//         init();
//         setSpec(url.toString());
//     }

//     private  URLResource(URI uri) {
//         init();
//         setSpec(uri.toString());
//     }
    
    static URLResource create(String spec) {
        return new URLResource(spec);
    }
    static URLResource create(URL url) {
        return new URLResource(url.toString());
    }
    static URLResource create(URI uri) {
        return new URLResource(uri.toString());
    }
    static URLResource create(File file) {
        return new URLResource(file.toString()); // toURL / toURI probably better
    }
    
    private void init() {
        if (DEBUG.RESOURCE || DEBUG.DR) {
            //out("init");
            String iname = getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
            //tufts.Util.printStackTrace("INIT " + iname);
            setDebugProperty("0INSTANCE", iname);
        }
    }

//     @Override
//     public String getContentType() {
//         if (mURL_Browse != null)
//             return extractExtension(mURL_Browse);
//         else
//             return super.getContentType();
//     }
    
    // todo: rename relativeName, and add a "shortName", for what CabinetResource provides
    // (which will also translate ':' to '/' on the mac)


    private volatile String mToolTipHTML;

    @Override
    public String getToolTipText() {
        if (mToolTipHTML == null || DEBUG.META)
            mToolTipHTML = buildToolTipHTML();
        return mToolTipHTML;
    }

    private void invalidateToolTip() {
        mToolTipHTML = null;
    }
    
    private static String deco(String s) {
        return "<i><b>"+s+"</b></i>";
    }

    private String buildToolTipHTML() {
        String pretty = "";

//         if (mURI != null) {
//             if (mURI.isAbsolute()) {
//                 pretty = deco(mURI.toString());
//                 if (DEBUG.Enabled) pretty += " (URI-FULL)";
//             } else {
//                 pretty = deco(mURI.getPath());
//                 if (DEBUG.Enabled) pretty += " (URIpath)";
//             }
//         } else {
        pretty = VueUtil.decodeURL(getSpec());
        if (pretty.startsWith("file://") && pretty.length() > 7)
            pretty = pretty.substring(7);
        if (DEBUG.Enabled) {
            if (pretty.equals(getSpec()))
                pretty += " (spec)";
            else
                pretty += " (decoded spec)";
        }
        pretty = deco(pretty);
            
            //}

        if (DEBUG.Enabled) {
            //final String nl = "<br>&nbsp;";
            final String nl = "<br>";

            pretty += nl + spec + " (spec)";
            if (mRelativeURI != null) 
                pretty += nl + "URI-RELATIVE: " + mRelativeURI;
            pretty += nl + String.format("%s ext=[%s]", asDebug(), getDataType());
//             pretty += nl + "type=" + TYPE_NAMES[getClientType()] + "(" + getClientType() + ")"
//                 + " impl=" + getClass().getName() + " ext=[" + getContentType() + "]";
            if (isLocalFile())
                pretty += " (isLocal)";
            //pretty += nl + "localFile=" + isLocalFile();
        }
        return pretty;
        
    }

    public static final boolean ALLOW_URI_WHITESPACE = false; // Not working yet

    @Override
    public void relativize(URI root)
    {

        if (true) {
            // TODO: this code is for backward compat with archive version #1.
            // We may be able to remove it in short order.  This is called
            // by the map after restoring.  Only archive version #1 resources
            // should ever have a PACKAGE_KEY property set tho, so it's
            // safe to leave this code in.
        
            // When dealing with a packaged resource, Resources that were originally
            // local-file will want to be re-written to point to the actual new local
            // package cache file.  But resources that we're NOT local will want to have
            // their resource spec's left alone, yet have their content actually pulled from
            // the local cache.  We can determine later if we want live updating from the
            // original web source of the data, or provide a user action for that.

            if (hasProperty(PACKAGE_KEY)) {
                String packageLocal = getProperty(PACKAGE_KEY);
                if (ALLOW_URI_WHITESPACE) {
                    // URI.create fails if there are spaces:
                    packageLocal = packageLocal.replaceAll(" ", "%20"); 
                }
                URI packaged = root.resolve(packageLocal);
                if (packaged != null) {
                    Log.debug("Found packaged: " + packaged);

                    //this.spec = SPEC_UNSET;
                    mRelativeURI = null;

                    // WE NO LONGER SET SPEC FOR WEB CONTENT: fetch PACKAGED_KEY when getting data (need new API for that..)                

                    if (isLocalFile()) {
                        // If the original was a local file (e.g., on some other user's machine),
                        // completely reset the spec, as it will have no meaning on the new
                        // users machine.
                        setSpec(packaged.toString());
                    }

                    if ("file".equals(packaged.getScheme())) {
                        setProperty(PACKAGE_FILE, packaged.getRawPath()); // be sure to use getRawPath, otherwise will decode octets
                        setCached(true); // will let thumbnail requests go to cache file instead
                    } else {
                        Log.warn("Non-file URI-scheme in resolved packaged URI: " + packaged);
                        setProperty(PACKAGE_FILE, packaged.toString());
                    }
                
                    return;
                }
            }
        
        }
        
        if (!isLocalFile()) {
            Log.debug("Remote, unpackaged file, skipping relativize: " + this);
            return;
        }

        if (false) {

            // incomplete

            if (DEBUG.Enabled) Log.debug("Relativize to " + root + "; " + this + "; curRelative=" + mRelativeURI);
            URI oldRelative = mRelativeURI;
            mRelativeURI = findRelativeURI(root);
            setDebugProperty("relative", mRelativeURI);
            if (oldRelative != mRelativeURI && !oldRelative.equals(mRelativeURI)) {
                invalidateToolTip();
            }
        }
    }
    
    private URI findRelativeURI(URI root)
    {
        if (DEBUG.Enabled) {
            System.out.println("\n=======================================================");
            Log.debug("attempting to relativize [" + this + "] against root " + root);
        }
        
        final URL url = asURL();
        if (url == null)
            return null;

        // absURI should always be absolute -- the way we persist them
        final URI absURI = makeURI(url.toString());
        
        if (!absURI.isAbsolute())
            Log.warn("Non absolute URI: " + absURI + "; from URL " + url);
        
        if (absURI == null) {
            System.out.println("URL INVALID FOR URI: " + url + "; in " + this);
            return null;
        }

        if (DEBUG.Enabled) Util.dumpURI(absURI, "ORIGINAL");
        final URI relativeURI = root.relativize(absURI);

        if (relativeURI == absURI) {
            // oldRoot was unable to relativize absURI -- this resource
            // was not relative to it's map in it's previous incarnation.
            return null;
        }
        
        if (relativeURI != absURI) {
            if (DEBUG.Enabled) Util.dumpURI(relativeURI, "RELATIVE");
        }

        if (DEBUG.Enabled) System.out.println(TERM_GREEN+"FOUND RELATIVE: " + relativeURI + TERM_CLEAR);        

        return relativeURI;

    }
    
    /**
     * If this resource can be made relative to the current map (is in a directory
     * below the current map), make sure we record it's relative location.
     * If oldRoot and newRoot are different (the map has moved), re-write
     * the resource to point to the new location if something is there.
     *
     * @param oldRoot - the root (parent directory) of the map the last time it was saved
     * @param newRoot - null if the same as oldRoot, otherwise, the newRoot
     */
    // ONLY USED FOR OLD STYLE AUTO-CONVERSION ON STARTUP
    @Override
    public void updateRootLocation(URI oldRoot, URI newRoot) {

        if (DEBUG.Enabled) {
            System.out.println();
            Log.debug("attempting to relativize [" + this + "] against curRoot " + oldRoot + "; newRoot " + newRoot);
        }
        
        final URL url = asURL();
        if (url == null)
            return;

        System.out.println("=======================================================");
        
        final URI absURI = makeURI(url.toString());
        // absURI should always be absolute -- the way we persist them

        if (!absURI.isAbsolute())
            Log.warn("Non absolute URI: " + absURI + "; from URL " + url);

        if (absURI == null) {
            System.out.println("URL INVALID FOR URI: " + url + "; in " + this);
            return;
        }

        Util.dumpURI(absURI, "ORIGINAL");
        final URI relativeURI = oldRoot.relativize(absURI);

        if (relativeURI == absURI) {
            // oldRoot was unable to relativize absURI -- this resource
            // was not relative to it's map in it's previous incarnation.

            // However, if newRoot is different from oldRoot,
            // it may be relative to the new map location (newRoot).

            if (newRoot == null) // was same as oldRoot
                return;
        }
        
        if (relativeURI != absURI)
            Util.dumpURI(relativeURI, "RELATIVE");
        //System.out.println(" RELATIVE URI: " + relativeURI);
        //System.out.println("RELATIVE PATH: " + relativeURI.getPath());
        

        if (newRoot != null) {

            if (relativeURI.isAbsolute()) { // meaning relativeURI == absURI
                //-------------------------------------------------------
                // was absolute: attempt to relativize against newRoot 
                //-------------------------------------------------------
                if (relativeURI != absURI) Log.warn("URLResource assertion failure: " + relativeURI + "; " + absURI);
                
                Log.debug("ATTEMPTING TO RELATIVIZE AGAINST NEW ROOT: " + relativeURI + "; " + newRoot);
                final URI newRelativeURI = newRoot.relativize(relativeURI);

                if (newRelativeURI != relativeURI) {
                    System.out.println(TERM_GREEN+"NOTICED NEW RELATIVE: " + newRelativeURI + TERM_CLEAR);
                    mRelativeURI = newRelativeURI;
                }
                
            } else {
                //-------------------------------------------------------
                // was relative: attempt to resolve against newRoot
                //-------------------------------------------------------
                Log.debug("ATTEMPTING RESOLVE AGAINST NEW ROOT: " + relativeURI + "; " + newRoot);
                final URI newAbsoluteURI = newRoot.resolve(relativeURI);
                final File newFile = new File(newAbsoluteURI.getPath());
                if (newFile.exists()) {
                    System.out.println(TERM_GREEN+"  FOUND NEW LOCATION: " + newFile + TERM_CLEAR);
                    spec = newAbsoluteURI.getRawPath();
                    // File was found at same relative location:
                    mRelativeURI = relativeURI;
                    mURL_Browse = null; // reset
                } else {
                    // File was NOT found same relative location --
                    // leave this Resource as it's old absolute value.
                    mRelativeURI = null;
                }
            }

        } else if (relativeURI != absURI) {
            mRelativeURI = relativeURI;
            System.out.println(TERM_GREEN+"  FOUND NEW RELATIVE: " + relativeURI + TERM_CLEAR);
            
        }

        invalidateToolTip();        

    }

    private static String encodeForURI(String s) {
        return s.replaceAll(" ", "%20");
    }

    public static URI makeURI(File f) {
        URI uri = f.toURI();
        if (DEBUG.RESOURCE) Util.dumpURI(uri, "NEW FILE URI FROM " + f);
        if (uri.getPath().startsWith("/C:"))
            return makeURI(uri.getPath().substring(3));
        else
            return uri;
        //return makeURI(f.toString());
    }
    
    public static URI makeURI(String s)
    {
        URI uri = null;
        final String encoded = encodeForURI(s);
        try {
            uri = new java.net.URI(encoded);
        } catch (java.net.URISyntaxException e) {
            Util.printStackTrace(e, s);
        }
        if (DEBUG.RESOURCE) {
            if (uri != null && uri.toString().equals(s))
                System.err.println("            MADE URI: " + uri);
            else
                System.err.println("            MADE URI: " + uri + " src=[" + s + "]");
        }
        
        return uri;
    }
    
//     public Object toDigitalRepositoryReference() {
//         return null;
//     }
    
    private String toURLString() {

        String s = this.spec.trim();

        
        final char c0 = s.length() > 0 ? s.charAt(0) : 0;
        final char c1 = s.length() > 1 ? s.charAt(1) : 0;
        final String txt;

        // In case there are any special characters (e.g., Unicode chars) in the
        // file name, we must first encode them for MacOSX (local files only?)
        // FYI, MacOSX openURL uses UTF-8, NOT the native MacRoman encoding.
        // URLEncoder encodes EVERYTHING other than alphas tho, so we need
        // to put it back.

        // But first we DECODE it, in case there are already any encodings,
        // we don't want to double-encode.
        //TODO: url = java.net.URLDecoder.decode(url, "UTF-8");
        //TODO: if (DEBUG) System.err.println("  DECODE UTF [" + url + "]");

        // TODO ALSO: cache file not being %20 normalized (Seeing %252520 !)

        

        if (c0 == '/' || c0 == '\\' || (Character.isLetter(c0) && c1 == ':')) {
            // first case: MacOSX path
            // second case: Windows path
            // third case: Windows "C:" style path
            // TODO: consider using URN's for this, which have some code
            // for this type of resolution.
            txt = "file://" + s;
        } else {
            txt = s;
        }
        //if (DEBUG.Enabled) out("toURLString[" + txt + "]");


        /*
          // from old LWImage code:
        if (s.startsWith("file://")) {

            // TODO: SEE Util.java: WINDOWS URL'S DON'T WORK IF START WITH FILE://
            // (two slashes), MUST HAVE THREE!  move this code to MapResource; find
            // out if can even force a URL to have an extra slash in it!  Report
            // this as a java bug.

            // TODO: Our Cup>>Chevron unicode char example is failing
            // here on Windows (tho it works for windows openURL).
            // (The image load fails)
            // Try ensuring the URL is UTF-8 first.
            
            s = s.substring(7);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("getImage " + s);
            image = java.awt.Toolkit.getDefaultToolkit().getImage(s);
        } else {
        */
        
        
        /*
        if (this.url == null) {
            // [old:this logic shouldn't be needed: if spec can be a a valid URL, this.url will already be set]
            if (spec.startsWith("file:") || spec.startsWith("http:") || spec.startsWith("ftp:")) {
                //System.err.println(getClass() + " BOGUS URLResource: is URL, but unrecognized! " + this);
                txt = spec;
            }
            // todo: handle "resource:" case
            else
                txt = "file:///" + spec;
            if (DEBUG.Enabled) out("toURLString[" + txt + "]");
        } else
            txt = this.url.toString();
        */

        //if (!spec.startsWith("file") && !spec.startsWith("http"))
        //    txt = "file:///" + spec;

        return txt;
    }
    
    // TODO: GIT RID OF ALL THIS LAZY CREATION CRAP, AND MERGE THIS INTO SET SPEC, INCLUDING toURLString crap
    private java.net.URL toURL() throws java.net.MalformedURLException
    {
        if (false) throw new java.net.MalformedURLException();
        if (mURL_Browse == null) {
            if (spec == SPEC_UNSET)
                return null;
            mURL_Browse = new java.net.URL(toURLString());
            if ("file".equals(mURL_Browse.getProtocol())) {
                setClientType(Resource.FILE);
                if (mTitle == null) {
                    String title;
                    title = mURL_Browse.getPath();
                    if (title != null) {
                        if (title.endsWith("/"))
                            title = title.substring(0, title.length() - 1);
                        title = title.substring(title.lastIndexOf('/') + 1);
                        if (tufts.Util.isMacPlatform()) {
                            // On MacOSX, file names with colon (':') in them display as slashes ('/')
                            title = title.replace(':', '/');
                        }
                        setTitle(title);
                    }
                }
            } else {
                setClientType(Resource.URL);
            }
            if (!hasProperty(CONTENT_TYPE))
                setProperty(CONTENT_TYPE, java.net.URLConnection.guessContentTypeFromName(mURL_Browse.getPath()));
        }
        return mURL_Browse;
        ///mURL = new java.net.URL(toURLString());
    }


    /*
    private java.net.URL toURL_OLD()
        throws java.net.MalformedURLException
    {
        if (spec.equals(SPEC_UNSET))
            return mURL;
            
        if (mURL == null) {
            

            if (spec.startsWith("resource:")) {
                final String classpathResource = spec.substring(9);
                //System.out.println("Searching for classpath resource [" + classpathResource + "]");
                mURL = getClass().getResource(classpathResource);
            } else
                mURL = new java.net.URL(toURLString());

            
            mURL = new java.net.URL(toURLString());

            setProperty("Content.type",
                        java.net.URLConnection.guessContentTypeFromName(mURL.getPath()));
            

            // This no longer makes sense as we're now more like an Asset, and
            // no single URL part need be singled out.
                
            mProperties.holdChanges();
            try {

                // todo: do this once on constrution of a URLResource
                setProperty("URL.protocol", mURL.getProtocol());
                setProperty("URL.userInfo", mURL.getUserInfo());
                setProperty("URL.host", mURL.getHost());
                setProperty("URL.authority", mURL.getAuthority());
                setProperty("URL.path", mURL.getPath());
                // setProperty("url.file", url.getFile()); // same as path (doesn't get us stub after last /)
                setProperty("URL.query", mURL.getQuery());
                setProperty("URL.ref", mURL.getRef());
                //setProperty("url.authority", url.getAuthority()); // always same as host?
                if (mURL.getPort() != -1)
                    setProperty("URL.port", mURL.getPort());

                //setProperty(CONTENT_TYPE,
                setProperty("Content.type",
                            java.net.URLConnection.guessContentTypeFromName(mURL.getPath()));

                
            } finally {
                mProperties.releaseChanges();
            }
            


            if ("file".equals(mURL.getProtocol())) {
                this.type = Resource.FILE;
                if (mTitle == null) {
                    String title;
                    title = mURL.getPath();
                    if (title != null) {
                        if (title.endsWith("/"))
                            title = title.substring(0, title.length() - 1);
                        title = title.substring(title.lastIndexOf('/') + 1);
                        if (tufts.Util.isMacPlatform()) {
                            // On MacOSX, file names with colon (':') in them display as slashes ('/')
                            title = title.replace(':', '/');
                        }
                        setTitle(title);
                    }
                }
            } else {
                this.type = Resource.URL;
            }
        }
        
        return mURL;
    }

*/

    private URL getPackagedURL() {

        final String propVal = getProperty(PACKAGE_FILE);

        if (propVal == null) {
            Log.info("getPackageURL returns null: " + this);
            return null;
        }
        
        String prop = propVal;
        if (!prop.startsWith("file:"))
            prop = "file:" + prop;
        final URL url = makeURL(prop);
        if (DEBUG.Enabled) Log.debug("Returning imageSource " + url + "; from property " + propVal);
        return url;
        
        //return makeURL(getProperty(PACKAGE_FILE));
    }

    
    /** @see tufts.vue.Resource -- todo: return URI */
    @Override
    public URL getImageSource() {

        // TODO: may be bootstrapping problem here... we're also calling this during the packaging code itself
        // And change this to return a URI!
        
        if (hasProperty(PACKAGE_FILE)) {

            return getPackagedURL();
            
//             final String propVal = getProperty(PACKAGE_FILE);
//             String prop = propVal;
//             if (!prop.startsWith("file:"))
//                 prop = "file:" + prop;
//             final URL url = makeURL(prop);
//             if (DEBUG.Enabled) Log.debug("Returning imageSource " + url + "; from property " + propVal);
//             return url;
//             //return makeURL(getProperty(PACKAGE_FILE));
            
        } else
            if (mURL_Image != null)
            return mURL_Image;
        else
            return asURL();
    }
    
    /** @return as a property URL, or null if unable to create one */
    // get this private/gone
    java.net.URL asURL()
    {
        try {
            return toURL();
        } catch (java.net.MalformedURLException e) {
            if (DEBUG.Enabled && DEBUG.META)
            //if (DEBUG.Enabled)
                Util.printStackTrace(e, "FYI: URLResource.asURL[" + this + "]");
        }
        return null;
    }

    @Override
    public int hashCode() {
        return spec == null ? super.hashCode() : spec.hashCode();
//         // TODO: this not safe long-term
//         if (mURL_Browse == null)
//             asURL();
//         if (mURL_Browse == null)
//             return super.hashCode();
//         else
//             return mURL_Browse.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Resource) {
            if (spec == SPEC_UNSET || spec == null)
                return false;
            final String spec2 = ((Resource)o).getSpec();
            if (spec2 == SPEC_UNSET || spec2 == null)
                return false;
            return spec.equals(spec2);
            //return getSpec().equals(((Resource)o).getSpec());
            // use URL?  Better URN eventually?
            //o.asURL().equals(asURL());
        }
        return false;
    }

    
    /** If given string is a valid URL, make one and return it, otherwise, return null. */
    private static java.net.URL makeURL(String s)
    {
        try {
            return new java.net.URL(s);
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }
    
    public void displayContent() {
        final String systemSpec;

        if (hasProperty(PACKAGE_FILE)) {
            systemSpec = getPackagedURL().toString();
        }
        else if (mURL_Browse != null) {
            systemSpec = mURL_Browse.toString();
        }
        else if (VueUtil.isMacPlatform()) {
            // toURL will fail if we have a Windows style "C:\Programs" url, so
            // just in case don't try and construct a URL here.
            systemSpec = toURLString();
        }
        else
            systemSpec = getSpec();
        
        try {
            markAccessAttempt();
            VueUtil.openURL(systemSpec);
            // access successful is not currently very meaningful,
            // as we don't know if the openURL failed or not.
            markAccessSuccess();
        } catch (Exception e) {
            //System.err.println(e);
            Log.error(systemSpec + "; " + e);
        }
    }


    public void setTitle(String title) {
        if (DEBUG.DATA || (DEBUG.RESOURCE && DEBUG.META)) out("setTitle " + title);
        mTitle = title;
    }
    
    public String getTitle() {
        return mTitle;
    }
    
//     // Preserved for comments: "Windows" here may be referring to Windows2000
    
//         String s = mr.getSpec();
//         if (s.startsWith("file://")) {

//             // TODO: SEE Util.java: WINDOWS URL'S DON'T WORK IF START WITH FILE://
//             // (two slashes), MUST HAVE THREE!  move this code to MapResource; find
//             // out if can even force a URL to have an extra slash in it!  Report
//             // this as a java bug.

//             // TODO: Our Cup>>Chevron unicode char example is failing
//             // here on Windows (tho it works for windows openURL).
//             // (The image load fails)
//             // Try ensuring the URL is UTF-8 first.
            
//             s = s.substring(7);
//             if (DEBUG.IMAGE || DEBUG.THREAD) out("getImage " + s);
//             image = java.awt.Toolkit.getDefaultToolkit().getImage(s);
//         } else {
//             if (DEBUG.IMAGE || DEBUG.THREAD) out("getImage");
//             image = java.awt.Toolkit.getDefaultToolkit().getImage(url);
//         }


    // TODO TODO TODO: resource's should be atomic: don't allow post construction setSpec,
    // or at least protected -- will need a dispatching factory to handle this properly,
    // as well as castor factories / creators
    
    /** @deprecated -- or perhaps, change to setLocalResource? setRawURL? setRawResource? */
    // Want this to be protected, but must be public for castor.
    // TODO: MAKE THIS JUST A PERSISTANCE RESTORE: record the spec and move on -- don't
    // process unless asked for something later...
    public void setSpec(final String spec) {

        if (mRelativeURI != null) {
            Log.warn(this + " setSpec w/URI set: " + mRelativeURI);
            //Util.printStackTrace(this + " setSpec w/URI set: " + mRelativeURI + " spec denied: " + spec);
            //return;
        }

        invalidateToolTip();
        
        if (DEBUG.RESOURCE/*&& DEBUG.META*/) {
            out("setSpec " + spec);
            //Util.printStackTrace("setSpec " + spec);
        }

        if (SPEC_UNSET.equals(spec)) {
            this.spec = SPEC_UNSET;
            return;
        }

                
        // TODO: will want generic ability to set the reference created
        // date lazily, as it doesn't make sense with CabinetResource, for example,
        // to set that until a user actually drag's one and makes use of it.
        // So a resource is going to become somewhat akin to a Transferable.
        
        this.spec = spec;
        //this.referenceCreated = System.currentTimeMillis();

        if (spec == null)
            throw new Error("Resource.setSpec can't be null");

        if (spec.startsWith("resource:")) {
            final String classpathResource = spec.substring(9);
            System.err.println("Searching for classpath resource [" + classpathResource + "]");
            mURL_Browse = getClass().getResource(classpathResource);
            System.err.println("Got classpath resource: " + mURL_Browse);
            
//             if (DEBUG.DR || DEBUG.RESOURCE) {
//                 setProperty(BROWSE_KEY, "" + mURL_Browse);
//             }
            
            //this.spec = mURL_Browse.toString();
        } else {
            if (!isImage) { // once an image, always an image (cause setURL_Image may be called before setURL_Browse)
                setAsImage(looksLikeImageFile(spec));
                if (!isImage && !mXMLrestoreUnderway) {
                    // if this is during a restore, wait for properties to come in
                    // so we can check for a Content.type property
                    checkForImageType();
                }
            }
            mURL_Browse = makeURL(spec);
            //setURL_Browse(spec);
        }


        if (DEBUG.DR || DEBUG.RESOURCE) {
            //setProperty("@<spec>", spec);
            if (mURL_Browse != null) setDebugProperty("browse.url", mURL_Browse);
        }

        /*
        try {
            
            if (spec.startsWith("resource:")) {
                final String classpathResource = spec.substring(9);
                //System.out.println("Searching for classpath resource [" + classpathResource + "]");
                url = getClass().getResource(classpathResource);
            }
            //else { url = new URL(spec); } // do lazily

            
//             String fname = url.getFile();
//             System.out.println("Resource [" + spec + "] has URL [" + url + "] file=["+url.getFile()+"] path=[" + url.getPath()+"]");
//             java.io.File file = new java.io.File(fname);
//             System.out.println("\t" + file + " exists=" +file.exists());
//             file = new java.io.File(spec);
//             System.out.println("\t" + file + " exists=" +file.exists());


        } catch (MalformedURLException e) {
            // Okay for url to be null: means local file
            if (DEBUG.Enabled) System.err.println(e);
            //System.out.println("Resource [" + spec + "] *** NOT A URL ***");
        } catch (Exception e) {
            //System.err.println(e);
            e.printStackTrace();
        }
        */

        //this.type = isLocalFile() ? Resource.FILE : Resource.URL;

        asURL();

        if (DEBUG.DR && spec != SPEC_UNSET) setDebugProperty("SPEC", spec);
        

        if (isLocalFile()) {
            if (spec.startsWith("file:"))
                setProperty("File", spec.substring(5));
            else
                setProperty("File", spec);
        } else {
            setProperty("URL", spec);
        }

        
        
        //this.preview = null;
    }

    /**
     * Search for meta-data: e.g.,
     *
     *  HTTP meta-data (contentType, size)
     *  HTML meta-data (title)
     *  FileSystem meta-data (e.g., Spotlight)
     *
     * Will set properties in the resource based on what's found,
     * and may update the title.
     *
     */
    /*
     * E.g. scan an initial chunk of our content for an HTML title
     * tag, and if one is found, set our title field to what we find
     * there.  RUNS IN IT'S OWN THREAD.  If the give LWC's label is
     * the same as the title at the start, that is updated also.
     *
     * TODO: resources need listeners so they can issue model
     * changes/signals, and we need to run this in an undoable thread.
     *
     * TODO: this may set the component label!  Either do that
     * in the caller instead of here, or rename this.  Altho,
     * one of the reasons it does that is that as this happens
     * async, it has to do that, as we can't return a value running async...
     */
    public void scanForMetaDataAsync(final LWComponent c) {
        scanForMetaDataAsync(c, false);
    }
    
    public void scanForMetaDataAsync(final LWComponent c, final boolean setLabelFromTitle) {
        //if (!isHTML() && !isImage()) {
        // [oops, "http://www.google.com/" doesn't initially appear as HTML]
            // don't bother with these for now: would only get us size & content-type
            // anyway, which if it's a file should come from the CabinetResource
            //return;
        //}
        new Thread("VUE-URL-MetaData") {
            public void run() {
                scanForMetaData(c, setLabelFromTitle);
            }
        }.start();
    }
    
    void scanForMetaData(final LWComponent c, boolean setLabelFromTitle) {

        if (true) {
            //System.out.println("SKIPPING META-DATA SCAN " + this);
            return;
        }
        
        URL _url = asURL();

        if (_url == null)  {
            if (DEBUG.Enabled) out("couldn't get URL");
            return;
        }

        final boolean forceTitleToLabel =
            setLabelFromTitle
            || c.getLabel() == null
            || c.getLabel().equals(mTitle)
            || c.getLabel().equals(getSpec());
        
        try {
            _scanForMetaData(_url);
        } catch (Throwable t) {
            Log.info(_url + ": meta-data extraction failed: " + t);
            if (DEBUG.Enabled) tufts.Util.printStackTrace(t, _url.toString());
        }

        if (forceTitleToLabel && getTitle() != null)
            c.setLabel(getTitle());

        if (DEBUG.Enabled) out("properties " + mProperties);
    }
    
    private void _scanForMetaData(URL _url) throws java.io.IOException {
        if (DEBUG.Enabled) System.out.println(this + " _scanForMetaData: xml props " + mXMLpropertyList);

        // TODO: split into scrapeHTTPMetaData for content type & size,
        // and scrapeHTML meta-data for title.  Tho really, we need
        // at this point to start having a whole pluggable set of content
        // meta-data scrapers.

        if (DEBUG.Enabled) System.out.println("*** Opening connection to " + _url);
        markAccessAttempt();
        
        Properties metaData = scrapeHTMLmetaData(_url.openConnection(), 2048);
        if (DEBUG.Enabled) System.out.println("*** Got meta-data " + metaData);
        markAccessSuccess();
        String title = metaData.getProperty("title");
        if (title != null && title.length() > 0) {
            setProperty("title", title);
            title = title.replace('\n', ' ').trim();
            setTitle(title);
        }
        try {
            setByteSize(Integer.parseInt((String) getProperty("contentLength")));
        } catch (Exception e) {}
    }

    
    // TODO: need to handle <title lang=he> example (is that legal HTML?) --
    //private static final Pattern HTML_Title = Pattern.compile(".*<\\s*title\\s*>\\s*([^<]+)", // did we need .* at end? 
   // need to ensure there is a space after title or the '>' immediately: don't want to match a tag that was <title-i-am-not> !

    private static final Pattern HTML_Title_Regex =
        Pattern.compile(".*<\\s*title[^>]*>\\s*([^<]+)", // hacked for lang=he constructs, but too broad
                        Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    private static final Pattern Content_Charset_Regex =
        Pattern.compile(".*charset\\s*=\\s*([^\">\\s]+)",
                        Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    
    // TODO: break out searching into looking for regex with each chunk of data we get in at least x size (e.g, 256)

    private Properties scrapeHTMLmetaData(URLConnection connection, int maxSearchBytes)
        throws java.io.IOException
    {
        Properties metaData = new Properties();
        
        InputStream byteStream = connection.getInputStream();

        if (DEBUG.DND && DEBUG.META) {
            System.err.println("Getting headers from " + connection);
            System.err.println("Headers: " + connection.getHeaderFields());
        }
        
        // note: be sure to call getContentType and don't rely on getting it from the HeaderFields map,
        // as sometimes it's set by the OS for a file:/// URL when there are no header fields (no http server)
        // (actually, this is set by java via a mime type table based on file extension, or a guess based on the stream)
        if (DEBUG.DND) System.err.println("*** getting contentType & encoding...");
        final String contentType = connection.getContentType();
        final String contentEncoding = connection.getContentEncoding();
        final int contentLength = connection.getContentLength();
        
        if (DEBUG.DND) System.err.println("*** contentType [" + contentType + "]");
        if (DEBUG.DND) System.err.println("*** contentEncoding [" + contentEncoding + "]");
        if (DEBUG.DND) System.err.println("*** contentLength [" + contentLength + "]");
        
        setProperty("url.contentType", contentType);
        setProperty("url.contentEncoding", contentEncoding);
        if (contentLength >= 0)
            setProperty("url.contentLength", contentLength);


        //if (contentType.toLowerCase().startsWith("text/html") == false) {
        if (!isHTML()) { // we only currently handle HTML
            if (DEBUG.Enabled) System.err.println("*** contentType [" + contentType + "] not HTML; skipping title extraction");
            return metaData;
        }
        
        if (DEBUG.DND) System.err.println("*** scanning for HTML meta-data...");

        try {
            final BufferedInputStream bufStream = new BufferedInputStream(byteStream, maxSearchBytes);
            bufStream.mark(maxSearchBytes);

            final byte[] byteBuffer = new byte[maxSearchBytes];
            int bytesRead = 0;
            int len = 0;
            // BufferedInputStream still won't read thru a block, so we need to allow
            // a few reads here to get thru a couple of blocks, so we can get up to
            // our maxbytes (e.g., a common return chunk count is 1448 bytes, presumably related to the MTU)
            do {
                int max = maxSearchBytes - bytesRead;
                len = bufStream.read(byteBuffer, bytesRead, max);
                System.out.println("*** read " + len);
                if (len > 0)
                    bytesRead += len;
                else if (len < 0)
                    break;
            } while (len > 0 && bytesRead < maxSearchBytes);
            if (DEBUG.DND) System.out.println("*** Got total chars: " + bytesRead);
            String html = new String(byteBuffer, 0, bytesRead);
            if (DEBUG.DND && DEBUG.META) System.out.println("*** HTML-STRING[" + html + "]");

            // first, look for a content encoding, so we can search for and get the title
            // on a properly encoded character stream

            String charset = null;

            Matcher cm = Content_Charset_Regex.matcher(html);
            if (cm.lookingAt()) {
                charset = cm.group(1);
                if (DEBUG.DND) System.err.println("*** found HTML specified charset ["+charset+"]");
                setProperty("charset", charset);
            }

            if (charset == null && contentEncoding != null) {
                if (DEBUG.DND||true) System.err.println("*** no charset found: using contentEncoding charset " + contentEncoding);
                charset = contentEncoding;
            }
            
            final String decodedHTML;
            
            if (charset != null) {
                bufStream.reset();
                InputStreamReader decodedStream = new InputStreamReader(bufStream, charset);
                //InputStreamReader decodedStream = new InputStreamReader(new ByteArrayInputStream(byteBuffer), charset);
                if (true||DEBUG.DND) System.out.println("*** decoding bytes into characters with official encoding " + decodedStream.getEncoding());
                setProperty("contentEncoding", decodedStream.getEncoding());
                char[] decoded = new char[bytesRead];
                int decodedChars = decodedStream.read(decoded);
                decodedStream.close();
                if (true||DEBUG.DND) System.err.println("*** " + decodedChars + " characters decoded using " + charset);
                decodedHTML = new String(decoded, 0, decodedChars);
            } else
                decodedHTML = html; // we'll just have to go with the default platform charset...
            
            // these needed to be left open till the decodedStream was done, which
            // although it should never need to read beyond what's already buffered,
            // some internal java code has checks that make sure the underlying stream
            // isn't closed, even it it isn't used.
            byteStream.close();
            bufStream.close();
            
            Matcher m = HTML_Title_Regex.matcher(decodedHTML);
            if (m.lookingAt()) {
                String title = m.group(1);
                if (true||DEBUG.DND) System.err.println("*** found title ["+title+"]");
                metaData.put("title", title.trim());
            }

        } catch (Throwable e) {
            System.err.println("scrapeHTMLmetaData: " + e);
            if (DEBUG.DND) e.printStackTrace();
        }

        if (DEBUG.DND || DEBUG.Enabled) System.err.println("*** scrapeHTMLmetaData returning [" + metaData + "]");
        return metaData;
    }

    private static void dumpBytes(String s) {
        try {
            dumpBytes(s.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.println("byte " + (i<10?" ":"") + i
                               + " (" + ((char)b) + ")"
                               + " " + pad(' ', 4, new Byte(b).toString())
                               + "  " + pad(' ', 2, Integer.toHexString( ((int)b) & 0xFF))
                               + "  " + pad('X', 8, toBinary(b))
                               );
        }
    }
    
    private static String toBinary(byte b) {
        StringBuffer buf = new StringBuffer(8);
        buf.append((b & (1<<7)) == 0 ? '0' : '1');
        buf.append((b & (1<<6)) == 0 ? '0' : '1');
        buf.append((b & (1<<5)) == 0 ? '0' : '1');
        buf.append((b & (1<<4)) == 0 ? '0' : '1');
        buf.append((b & (1<<3)) == 0 ? '0' : '1');
        buf.append((b & (1<<2)) == 0 ? '0' : '1');
        buf.append((b & (1<<1)) == 0 ? '0' : '1');
        buf.append((b & (1<<0)) == 0 ? '0' : '1');
	return buf.toString();
    }
    
    private static void dumpString(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int cv = (int) chars[i];
            System.out.println("char " + (i<10?" ":"") + i
                               + " (" + chars[i] + ")"
                               + " " + pad(' ', 6, new Integer(cv).toString())
                               + " " + pad(' ', 4, Integer.toHexString(cv))
                               + "  " + pad('0', 16, Integer.toBinaryString(cv))
                               );
        }
    }
    
    private static String pad(char c, int wide, String s) {
        int pad = wide - s.length();
        StringBuffer buf = new StringBuffer(wide);
        while (pad-- > 0) {
            buf.append(c);
        }
        buf.append(s);
        return buf.toString();
    }
    
    
    
    /** Return exactly whatever we were handed at creation time.  We
     * need this because if it's a local file (file: URL or just local
     * file path name), we need whatever the local OS gave us as a
     * reference in order to give that to give back to openURL, as
     * it's the most reliable string to give back to the underlying OS
     * for opening a local file.  */
    
    public String getSpec() {
        return this.spec;
        /*
        if (mURL_Browse == null)
            return SPEC_UNSET;
        else
            return mURL_Browse.toString();
        */
    }

    public String getRelativeURI() {
        if (mRelativeURI != null)
            return mRelativeURI.toString();
        else
            return null;
    }

    /** persistance only */
    public void setRelativeURI(String s) {
        if (!mXMLrestoreUnderway) {
            Util.printStackTrace("only allowed for persistance; setRelativeURI " + s);
            return;
        }
        mRelativeURI = makeURI(s);
    }
    
    
    /*
     * If isLocalFile is true, this will return a file name
     * suitable to be given to java.io.File such that it
     * can be found.  Note that this may differ from getSpec.
     * If isLocalFile is false, it will return the file
     * portion of the URL, although that may not be useful.

    public String getFileName() {
        if (mURL == null)
            return getSpec();
        else
            return url.getFile();
    }
    
     */
    
    /** this is only meaninful if this resource points to a local file */
    protected Image getFileIconImage() {
        return GUI.getSystemIconForExtension(getDataType(), 128);
    }
    
    @Override
    public boolean isLocalFile() {

        if (false) {
        //if (hasProperty(PACKAGE_FILE)) {
            // todo: make sure this isn't overkill...
            return true;
        } else {
            asURL();
            return mURL_Browse == null || mURL_Browse.getProtocol().equals("file");
            //String s = spec.toLowerCase();
            //return s.startsWith("file:") || s.indexOf(':') < 0;
        }

    }
    
    
//     public String getExtension() {
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
    
    
//     /**
//      * getPropertyNames
//      * This returns an array of property names
//      * @return String [] the list of property names
//      **/
//     public String [] getPropertyNames() {
        
//         if( (mPropertyNames == null) && (!mProperties.isEmpty()) ) {
//             Set keys = mProperties.keySet();
//             if( ! keys.isEmpty() ) {
//                 mPropertyNames = new String[ keys.size() ];
//                 Iterator it = keys.iterator();
//                 int i=0;
//                 while( it.hasNext()) {
//                     mPropertyNames[i] = (String) it.next();
//                     i++;
//                 }
//             }
//         }
//         return mPropertyNames;
//     }
    
    
    /** @deprecated */
    public void setProperties(Properties p) {
        tufts.Util.printStackTrace("URLResource.setProperties: deprecated " + p);
    }
    
    /*
    public Map getPropertyMap() {
        System.out.println(this + " *** getPropertyMap " + mProperties);
        return mProperties;
    }
    public void setPropertyMap(Map m) {
        System.out.println(this + " *** setPropertyMap " + m.getClass().getName() + " " + m);
        mProperties = (Properties) m;
    }
    */
    
    
    /** this is for castor persistance only */
    public java.util.List getPropertyList() {
        
        if (mXMLrestoreUnderway == false) {

            if (mProperties.size() == 0) // a hack for castor to work
                return null;

            mXMLpropertyList = new ArrayList(mProperties.size());

            Iterator i = mProperties.keySet().iterator();
            while (i.hasNext()) {
                final String key = (String) i.next();
                final PropertyEntry entry = new PropertyEntry();
                entry.setEntryKey(key);
                entry.setEntryValue(mProperties.get(key));
                mXMLpropertyList.add(entry);
            }
        }

        if (DEBUG.CASTOR) System.out.println(this + " getPropertyList " + mXMLpropertyList);
        return mXMLpropertyList;
    }
    
    public void XML_initialized() {
        if (DEBUG.CASTOR) System.out.println(getClass() + " XML INIT");
        mXMLrestoreUnderway = true;
        mXMLpropertyList = new ArrayList();
    }

    public void XML_fieldAdded(String name, Object child) {
        if (DEBUG.XML) out("XML_fieldAdded <" + name + "> = " + child);
    }
    
    public void XML_completed()
    {
        if (DEBUG.CASTOR) System.out.println(this + " XML COMPLETED");

        for (PropertyEntry entry : mXMLpropertyList) {
            
            final Object key = entry.getEntryKey();
            final Object value = entry.getEntryValue();

            // This comes via the SPEC (todo: merge these two?)
            //if (BROWSE_KEY.equals(key))
            //    setURL_Browse((String) value);
            // else
            // TODO: faster to do single hashed lookup a end
            if (IMAGE_KEY.equals(key))
                setURL_Image((String) value);
            else if (THUMB_KEY.equals(key))
                setURL_Thumb((String) value);
            else
                setProperty((String)key, value);
        }

        if (DEBUG.DR) {
            // note the restored values
            //if (spec != SPEC_UNSET) setDebugProperty("SPEC", spec);
            if (mTitle != null) setDebugProperty("TITLE", mTitle);
        }

        mXMLpropertyList = null;
        mXMLrestoreUnderway = false;
        checkForImageType();
    }
    
    public void XML_addNotify(String name, Object parent) {
        if (DEBUG.CASTOR) System.out.println(this + " XML ADDNOTIFY as \"" + name + "\" to parent " + parent);
    }

    /** @deprecated -- for backward compat with lw_mapping_1.0.xml only, where this never worked */
    public void setPropertyList(Vector propertyList) {
        // This actually never get's called, but the old mapping file demands that it's here.
        System.out.println("IGNORING OLD SAVE FILE DATA " + propertyList + " for " + this);
        /*
        System.out.println(this + " setPropertyList " + mXMLpropertyList);
        this.mXMLpropertyList = mXMLpropertyList;
        this.mProperties = new Properties();
        Iterator i = mXMLpropertyList.iterator();
        while(i.hasNext()) {
            PropertyEntry entry = (PropertyEntry) i.next();
            final Object key = entry.getEntryKey();
            final Object value = entry.getEntryValue();
            setProperty((String)key, value);
            //mProperties.put(entry.getEntryKey(),entry.getEntryValue());
        }
        */
    }

    
    private void setAsImage(boolean asImage) {
        isImage = asImage;
        if (DEBUG.DR || DEBUG.RESOURCE) setDebugProperty("isImage", ""+ asImage);
    }

    public boolean isImage() {
        //return isImage(this);
        return isImage;
    }

    private void checkForImageType() {
        if (!isImage) {
            if (hasProperty(CONTENT_TYPE)) {
                setAsImage(isImageMimeType(getProperty(CONTENT_TYPE)));
            } else {
                // TODO: on initial creation of resources with types unidentifiable from the spec,
                // this code will load CONTENT_TYPE (in getDataType), and determine isImage
                // with looksLikeImageFile, but then when saved/restored, the above case
                // will use isImageMimeType, which isn't the exact same test -- fix this.
                setAsImage(looksLikeImageFile('.' + getDataType()));
            }
        }
    }

    private static boolean isImageMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("image/");
    }

    private static boolean isHtmlMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("text/html");
    }

    
    @Override
    public String getDataType() {

        final String superType = super.getDataType();
        final String spec = getSpec();

//         if (type == EXTENSION_UNKNOWN || type == EXTENSION_DIR)
//             return type
        
        if (spec.endsWith("=jpeg")) {
            // special case for MFA data source -- TODO: MFA OSID should handle this
            return "jpeg";
        } else if (mimeType != UNSET) {
            return mimeType;
        } else if (spec != SPEC_UNSET && spec.startsWith("http") && spec.contains("fedora")) { // fix for fedora url
            try {
                final URL url = new URL(getSpec());
                // TODO: checking spec, which defaults to the "browse" URL, will not get
                // the real content-type in cases (such as fedora!) where the browse
                // url is always an HTML page that includes the image with some descrition text.
                //Log.info("opening URL " + url);
                final String type = url.openConnection().getHeaderField("Content-type");
                if (DEBUG.Enabled) {
                    out("got contentType " + url + " [" + type + "]");
                    //Util.printStackTrace("GOT CONTENT TYPE");
                }
                if (type != null && type.length() > 0)
                    setProperty(CONTENT_TYPE, type);
                if (type != null && type.contains("/")) {
                    mimeType = type.split("/")[1]; // returning the second part of mime-type
                    return mimeType;
                } else {
                    return superType;
                }
            } catch (Throwable t) {
                t.printStackTrace();
                return superType;
            }
            
        } else
            return superType;
    }

    private static final String UNSET = "<unset-mimeType>";
    private String mimeType = UNSET;

    

    /*
    private static boolean isImage(final Resource r)
    {
          // doesn't make sense to check these now?  Wouldn't the Images code will never have been
          // called unless we already considered this an image?
          // Oh, crap: backward compat with recently saved files?  But still...
          
        if (r.getProperty("image.format") != null)                      // we already know this is an image
            return true;

        if (isImageMimeType(r.getProperty(Images.CONTENT_TYPE)))        // check http contentType
            // || isImageMimeType(r.getProperty("format")))                // TODO: hack for FEDORA dublin-core mime-type
            return true;

        // todo: temporary hack for Osid2AssetResource w/Museum of Fine Arts, Boston
        // actually, it needs to set the spec for this to work
        //if (r.getProperty("largeImage") != null)
        //return true;

        String s = r.getSpec().toLowerCase();
        if    (s.endsWith(".gif")
            || s.endsWith(".jpg")
            || s.endsWith(".jpeg")
            || s.endsWith("=jpeg") // temporary hack for MFA until Resources become more Asset-like
            || s.endsWith(".png")
            || s.endsWith(".tif")
            || s.endsWith(".tiff")
            || s.endsWith(".fpx")
            || s.endsWith(".bmp")
            || s.endsWith(".ico")
               ) return true;

        return false;
    }
    */

    /*
    public java.net.URL getImageURL() {

        // TODO: temporary hack Hack for FEDORA images, as the image URL is different
        // than the double-click URL.
        String imageURL = getProperty("Medium Image");
        if (imageURL != null && getProperty("Identifier") != null) // Make sure it's FEDORA
            return makeURL(imageURL);
        else
            return asURL();
    }
    */


    private static boolean isHTML(final Resource r) {
        String s = r.getSpec().toLowerCase();

        if (s.endsWith(".html") || s.endsWith(".htm"))
            return true;

        // todo: why .vue files reporting as text/html on MacOSX to content scraper?

        return !s.endsWith(".vue")
            && isHtmlMimeType(r.getProperty("url.contentType"))
            //&& !isImage(r) // sometimes image files claim to be text/html
            ;
    }
        
    public boolean isHTML() {
        if (isImage)
            return false;
        else
            return isHTML(this);
    }

    // TODO: combine these into a constructor only for integrity (e.g., Osid2AssetResource is factory only)
    
    /** This is the "default" URL */
    protected void setURL_Browse(String s) {
        setSpec(s);// backward compat for now just in case
        /*
        mURL_Browse = makeURL(s);
        this.spec = s; // backward compat for now just in case
        if (DEBUG.DR || DEBUG.RESOURCE) addProperty("@@spec", spec);
        if (DEBUG.DR || DEBUG.RESOURCE) addProperty("@Browse", "" + mURL_Browse);
        */
    }

    protected void setURL_Thumb(String s) {
        mURL_Thumb = makeURL(s);
        //if (DEBUG.DR || DEBUG.RESOURCE)
        setProperty(THUMB_KEY, "" + mURL_Thumb);
        //mPreview = mURL_Thumb;
    }

    /** If given any valid URL, this resource will consider itself image content, no matter
     * what's at the other end of that URL, so care should be taken to ensure it's
     * valid image data (as opposed to say, an HTML page)
     */
    protected void setURL_Image(String s) {
        mURL_Image = makeURL(s);
        //if (DEBUG.DR || DEBUG.RESOURCE)
        setProperty(IMAGE_KEY, "" + mURL_Image);
        if (mURL_Image != null)
            setAsImage(true);
    }


    //private URL mThumbShot;

    /**
     * Either immediately return an Image object if available, otherwise return an
     * object that is some kind of valid image source (e.g., a URL or image Resource)
     * that can be fed to Images.getImage and fetch asynchronously w/callbacks if it
     * isn't already in the cache.
     */
    final String vueExtension = ".vue";
    public Object getPreview()
    {
        if (isCached && isImage())
            return this;
        else if (mURL_Thumb != null)
            return mURL_Thumb;
        else if (mURL_Image != null)
            return mURL_Image;
        else if (isImage)
            // TODO: this not a good idea... only doing it so Images can put meta-data back into it
            return this;
        else if (isLocalFile()) {
            return getFileIconImage();
        }
        else if (mURL_Browse != null && !isLocalFile()) 
        {
        	//System.out.println("mURL_Browse : " + mURL_Browse);
        	
        	if (mURL_Browse.toString().toLowerCase().endsWith(vueExtension))
        		return VueResources.getBufferedImage("vueIcon64x64");
        	else
        		return getThumbshotURL(mURL_Browse);
//             if (mThumbShot == null) {
//                 mThumbShot = fetchThumbshot(mURL_Browse);

//                 // If we don't assign this, it will keep trying, which
//                 // is bad, yet if we go from offline to online, we'd
//                 // like to start finding these, so we just keep trying for now...
//                 //if (mThumbShot == null) mThumbShot = GUI.NoImage32;
//             }
//             return mThumbShot;
        }
        else 
            return null;
        
        /*
        if (mPreview == null) {
            // TODO: this currently special case to Osid2AssetResource, tho names are somewhat generic..
            URL url = null;
            //url = makeURL(getProperty("mediumImage"));
            url = makeURL(getProperty("smallImage"));
            if (url == null)
                url = makeURL(getProperty("thumbnail"));

            if (url == null) { // TODO: Hack for MFA until Resource has API for setting Asset-like meta-data
                url = makeURL(getProperty("Preview Or Thumbnail"));
                if (DEBUG.RESOURCE) tufts.Util.printStackTrace("got MFA preview " + url);
            }
            
            if (url != null)
                mPreview = url;
        }
        return mPreview;
        */
    }

    public static final String THUMBSHOT_FETCH = "http://open.thumbshots.org/image.pxf?url=";

    private URL getThumbshotURL(URL url) {
        if (true)
            // I don't think thumbshots ever generate images for paths beyond the root host:
            return makeURL(String.format("%s%s://%s/",
                                         THUMBSHOT_FETCH,
                                         url.getProtocol(),
                                         url.getHost()));
        else
            return makeURL(THUMBSHOT_FETCH + url);
    }


//     // Could create an Images.Thumbshot class that can be a recognized special image
//     // source (just the thumbshot URL), which getPreview can return, so ResourceIcon /
//     // PreviewPane can feed it to Images.getImage and get the async callback when it's
//     // loaded instead of having to fetch the thumbshot on the AWT EDT.  (Also, Images
//     // can then manage caching the thumbshots, perhaps based on host only.  Also may not
//     // want to bother caching those to disk in case of expiration).

//     private Image fetchThumbshot(URL url)
//     {
//         if (url == null || !"http".equals(url.getProtocol()))
//             return null;

//         final String thumbShotURL = "http://open.thumbshots.org/image.pxf?url=" + url;
//         final URL thumbShot = makeURL(thumbShotURL);

//         if (thumbShot == null)
//             return null;

//         // TODO: if we're currently on the AWT event thread, this should NOT run synchronously...
//         // We should spawn a thread for this.  Otherwise, any delay in accessing thumbshots.org
//         // will result in the UI locking up until it responds with a result/error.

//         final boolean inUI_Thread = SwingUtilities.isEventDispatchThread();

//         if (inUI_Thread) {
//             // 2007-11-05 SMF -- okay, this not safe, turning off for now:
//             if (DEBUG.Enabled) Log.debug("skipping thumbshot fetch in AWT EDT: " + thumbShot);
//             return null;
//         }

//         if (inUI_Thread) {
//              Log.warn("fetching thumbshot in AWT; may lock UI: " + thumbShot);
//              if (DEBUG.Enabled && DEBUG.META) Util.printStackTrace("fetchThumbshot " + thumbShot);
//         } else {
//             if (DEBUG.IO) Log.debug("attempting thumbshot: " + thumbShot);
//         }

//         Image image = null;
//         boolean gotError = false;
//         try {
//             image = ImageIO.read(thumbShot);
//         } catch (Throwable t) {
//             if (inUI_Thread) {
//                 gotError = true;
//                 Log.warn("fetching thumbshot in AWT;   got error: " + thumbShot + "; " + t);
//             }
//             //if (DEBUG.Enabled) Util.printStackTrace(t, thumbShot.toString());
//         }
//         if (inUI_Thread && !gotError)
//             Log.warn("fetching thumbshot in AWT;         got: " + thumbShot);

//         if (image == null) {
//             if (DEBUG.WEBSHOTS) out("Didn't get a valid return from webshots : " + thumbShot);
//         } else if (image.getHeight(null) <= 1 || image.getWidth(null) <= 1) {
//             if (DEBUG.WEBSHOTS) out("This was a valid URL but there is no webshot available : " + thumbShot);
//             return null;
//         }
        
//         if (DEBUG.WEBSHOTS) out("Returning webshot image " + image);
        
//         return image;
//     }

        

    private boolean isCached;
//     public boolean isCached() {
//         return isCached;
//     }

    // todo: this should be computed internally (move code out of Images.java)
    public void setCached(boolean cached) {
        isCached = cached;
    }

//     public java.io.InputStream getByteStream() {
//         if (isImage())
//             ;
//         return null;
            
//     }

//     private File cacheFile;
//     public void setCacheFile(File file) {
//         cacheFile = file;
//         Log.debug(this + "; cache file set to: " + cacheFile);
//     }

//     public File getCacheFile() {
//         return cacheFile;
//     }
    

    /*
    public void setPreview(Object preview) {
        // todo: ignored for now (Osid2AssetResource putting gunk here)
        //mPreview = preview;
        //out("Ignoring setPreview " + preview);
    }
    */
    
        
    // TODO: calling with a different width/height only changes the size of
    // the existing icon, thus all who have reference to this will change!
    //public Icon getIcon(int width, int height) {
        //mIcon.setSize(width, height);

    
    /*
    public void setIcon(Icon i) {
        mIcon = i;
    }
    */

    
    

    /*
    public void setPreview(JComponent preview) {
        this.preview = preview;
    }

    public JComponent getPreview() {
        return this.preview;
    }
    */

    
    
      
    /*
    private JComponent viewer;
    public JComponent getAssetViewer(){
        return null;   
    }

    public void setAssetViewer(JComponent viewer){
        this.viewer = viewer;   
    }
    
    public Object getContent()
        throws IOException, MalformedURLException
    {
        tufts.Util.printStackTrace("DEPRECATED getContent " + this);
        
        if (isImage()) {
            return getImageIcon();
        } else {
            final Object content = getContentData();
            if (content instanceof ImageProducer) {
                // flag us as an image and/or pull that from contentType at other end of URL,
                // then re-try getContent to get the image.  We can get here if someone
                // manually edits a resource string inside a LWImage, which will try
                // and pull it's image, but the resource doesn't know it is one yet.
                setProperty("url.contentType", "image/unknown");
                return getImageIcon();
            } else
                return content;
        }
    }

    private ImageIcon getImageIcon()
        throws IOException, MalformedURLException
    {
        URL u = toURL();
        System.out.println(u + " fetching image");
        ImageIcon imageIcon = new ImageIcon(u);
        System.out.println(u + " got image size " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight());
        setProperty("DEBUG.icon.width", imageIcon.getIconWidth());
        setProperty("DEBUG.icon.height", imageIcon.getIconHeight());
        return imageIcon;
    }
    
    public Object getContentData()
        throws IOException, MalformedURLException
    {
        // in the case of an HTML page, this just gets us a sun.net.www.protocol.http.HttpURLConnection$HttpInputStream,
        // -- the same as we get from openConnection()
        return toURL().getContent();
    }


    public JComponent getPreview()
    {
        if (preview != null)
            return preview;
        
        try {
            // todo: cache the content type
            URL location = toURL();
            URLConnection conn = location.openConnection();
            if (conn == null)
                return null;
            String contentType = conn.getContentType();
            // if inaccessable (e.g., offline) contentType will be null
            if (contentType == null)
                return null;
            if (DEBUG.Enabled) System.out.println(this + " getPreview: contentType=" + contentType);
            if (contentType.indexOf("text") >= 0) {
                /**
                JEditorPane editorPane = new JEditorPane(location);
                Thread.sleep(5);
                editorPane.setEditable(false);
                JButton button = new JButton("Hello");
                editorPane.setSize(1000, 1000);
                Dimension size = editorPane.getSize();
                BufferedImage image = new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                g2.setBackground(Color.WHITE);
                g2.setColor(Color.BLACK);
                editorPane.printAll(g2);
                preview = new JLabel(new ImageIcon(image.getScaledInstance(75,75,Image.SCALE_FAST)));
                **
                //javax.swing.filechooser.FileSystemView view = javax.swing.filechooser.FileSystemView.getFileSystemView();
                //this.preview = new JLabel(view.getSystemIcon(File.createTempFile("temp",".html")));
                // absurd to create a tmp file during object selection!  Java doesn't give us the
                // real filesystem icon's anyway (check that on the PC -- this is OSX)
            } else if (contentType.indexOf("image") >= 0) {
                this.preview = new JLabel(new ImageIcon(location));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return preview;
    }

    */


//     protected void out(String s) {
//         System.err.println(getClass().getName() + "@" + Integer.toHexString(hashCode()) + ": " + s);
//     }

    public static void main(String args[]) {
        String rs = args.length > 0 ? args[0] : "/";

        VUE.parseArgs(args);
        
        DEBUG.Enabled = true;
        DEBUG.DND = true;

        URLResource r = (URLResource) Resource.instance(rs);
        System.out.println("Resource: " + r);
        System.out.println("URL: " + r.asURL());
        r.displayContent();
    }
    
    
}