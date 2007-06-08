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

import java.util.*;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import javax.swing.JComponent;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;


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
 * @version $Revision: 1.23 $ / $Date: 2007-06-08 19:12:28 $ / $Author: anoop $
 */

// TODO: this class currently a humongous mess...

public class URLResource implements Resource, XMLUnmarshalListener
{
    public static final String SPEC_UNSET = "<spec-unset>";

    private static final String BROWSE_KEY = "@Browse";
    private static final String IMAGE_KEY = "@Image";
    private static final String THUMB_KEY = "@Thumb";

    static final long SIZE_UNKNOWN = -1;
    
    private long referenceCreated; // this currently meaningless -- gets set every time -- is there anything meaningful here?
    private long accessAttempted;
    private long accessSuccessful;
    private long size = SIZE_UNKNOWN;
    //protected transient boolean selected = false;
    private String spec = SPEC_UNSET;
    private int type = Resource.NONE;
    private JComponent viewer;
    //private JComponent preview;
    private tufts.vue.ui.ResourceIcon mIcon;
    //private Object mPreview;
    private boolean isCached;
    private boolean isImage;

    //private URL mURL = null;
    // TODO performance: store as strings and only do conversion when
    // we ask for them.
    protected URL mURL_Browse;
    private URL mURL_Thumb;
    private URL mURL_Image;
    
    /** the metadata property map **/
    private PropertyMap mProperties = new PropertyMap();
    
    /** property name cache **/
    private String [] mPropertyNames = null;
    
    /** an optional resource title */
    protected String mTitle;
    

    private boolean mXMLrestoreUnderway = false;
    private ArrayList mXMLpropertyList;
    
    // for castor to save and restore
    
    // we REALLY need to get rid of this constructor: all the code
    // expects SPEC to be non-null!  Castor won't let us though...
    public URLResource() {
        init();
    }
    
    public URLResource(String spec) {
        init();
        setSpec(spec);
    }
    
    public URLResource(URL url) {
        init();
        setSpec(url.toString());
    }

    private void init() {
        if (DEBUG.RESOURCE || DEBUG.DR) {
            //out("init");
            String iname = getClass().getName() + "@" + Integer.toHexString(hashCode());
            //tufts.Util.printStackTrace("INIT " + iname);
            setProperty("@ instance", iname);
        }
    }
    
    
    public Object toDigitalRepositoryReference() {
        return null;
    }
    
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
            mURL_Browse = new java.net.URL(toURLString());
            if ("file".equals(mURL_Browse.getProtocol())) {
                this.type = Resource.FILE;
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
                this.type = Resource.URL;
            }
            setProperty("Content.type", java.net.URLConnection.guessContentTypeFromName(mURL_Browse.getPath()));
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
    
    /** @see tufts.vue.Resource */
    public Object getImageSource() {
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
                tufts.Util.printStackTrace(e, "FYI: URLResource.asURL[" + this + "]");
        }
        return null;
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

        if (mURL_Browse != null) {
            systemSpec = mURL_Browse.toString();

        } else if (VueUtil.isMacPlatform()) {
            // toURL will fail if we have a Windows style "C:\Programs" url, so
            // just in case don't try and construct a URL here.
            systemSpec = toURLString();
        } else
            systemSpec = getSpec();
        
        try {
            //this.accessAttempted = System.currentTimeMillis();
            VueUtil.openURL(systemSpec);
            this.accessSuccessful = System.currentTimeMillis();
        } catch (Exception e) {
            //System.err.println(e);
            VUE.Log.error(systemSpec + "; " + e);
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
    
    public void setTitle(String title) {
        if (DEBUG.DATA || (DEBUG.RESOURCE && DEBUG.META)) out("setTitle " + title);
        mTitle = title;
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    /*
      
    public static Image load(URL url)
    {
        if (url == null)
            return null;

        Image image = null;
        
        try {
            image = ImageIO.read(url);
        } catch (Throwable t) {
            if (DEBUG.Enabled) Util.printStackTrace(t);
            VUE.Log.info(url + ": " + t);
        }

        if (image != null)
            return image;

        // If the host isn't responding, Toolkit.getImage will block for a while.  It
        // will apparently ALWAYS eventually get an Image object, but if it failed, we
        // eventually get callback to imageUpdate (once prepareImage is called) with an
        // error code.  In any case, if you don't want to block, this has to be done in
        // a thread.
        
        String s = mr.getSpec();

            
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
            if (DEBUG.IMAGE || DEBUG.THREAD) out("getImage");
            image = java.awt.Toolkit.getDefaultToolkit().getImage(url);
        }

        if (image == null) Util.printStackTrace("image is null");


        return image;
    }
    */

    private void setAsImage(boolean asImage) {
        isImage = asImage;
        if (DEBUG.DR || DEBUG.RESOURCE) setProperty("@isImage", ""+ asImage);
    }

    
    // TODO TODO TODO: resource's should be atomic: don't allow post construction setSpec,
    // or at least protected
    /** @deprecated -- or perhaps, change to setLocalResource? setRawURL? setRawResource? */
    public void setSpec(final String spec) {
        if (DEBUG.RESOURCE/*&& DEBUG.META*/) {
            out("setSpec " + spec);
            //tufts.Util.printStackTrace("setSpec " + spec);
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
            //this.spec = mURL_Browse.toString();
        } else {
            if (!isImage) // once an image, always an image (cause setURL_Image may be called before setURL_Browse)
                setAsImage(looksLikeImageFile(spec));
            mURL_Browse = makeURL(spec);
            //setURL_Browse(spec);
        }

        if (DEBUG.DR || DEBUG.RESOURCE) {
            setProperty("@<spec>", spec);
            setProperty(BROWSE_KEY, "" + mURL_Browse);
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
            System.out.println("SKIPPING META-DATA SCAN " + this);
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
            VUE.Log.info(_url + ": meta-data extraction failed: " + t);
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
            setSize(Integer.parseInt((String) getProperty("contentLength")));
        } catch (Exception e) {}
    }

    private void markAccessAttempt() {
        this.accessAttempted = System.currentTimeMillis();
    }
    private void markAccessSuccess() {
        this.accessSuccessful = System.currentTimeMillis();
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

   public boolean isLocalFile() {
        asURL();
        return mURL_Browse == null || mURL_Browse.getProtocol().equals("file");
        //String s = spec.toLowerCase();
        //return s.startsWith("file:") || s.indexOf(':') < 0;
    }
    
    public String getExtension() {
        final String r = getSpec();
        String ext = "xxx";

        if (r.startsWith("http"))
            ext = "web";
        else if (r.startsWith("file"))
            ext = "file";
        else {
            ext = r.substring(0, Math.min(r.length(), 3));
            if (!r.endsWith("/")) {
                int i = r.lastIndexOf('.');
                if (i > 0 && i < r.length()-1)
                    ext = r.substring(i+1);
            }
        }
        if (ext.length() > 4)
            ext = ext.substring(0,4);
        
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
            setSize(value);
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
    
    /** @deprecated */
    public void setProperties(Properties p) {
        tufts.Util.printStackTrace("URLResource.setProperties: deprecated " + p);
    }
    
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getToolTipInformation() {
        return "ToolTip Information";
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
        Iterator i = mXMLpropertyList.iterator();
        while (i.hasNext()) {
            final PropertyEntry entry = (PropertyEntry) i.next();
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
            if (spec != SPEC_UNSET) setProperty("@(spec)", spec);
            if (mTitle != null) setProperty("@(title)", mTitle);
        }

        mXMLpropertyList = null;
        mXMLrestoreUnderway = false;
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
    

    public boolean isImage() {
        //return isImage(this);
        return isImage;
    }

    /* guiess if a URL or File contains image dta */
    private static boolean looksLikeImageFile(String path) {
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

    private static boolean isImageMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("image/");
    }

    private static boolean isHtmlMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("text/html");
    }


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

    /** Return this object if cached (can use the full, raw content for preview),
     * otherwise thumbnail if there is one.  TODO: don't make this decision
     * for the UI... just always return thumbnail URL if there is one (null if none).
     */
    public Object getPreview() {

        if (isCached)
            return this;
        else if (mURL_Thumb != null)
            return mURL_Thumb;
        else if (mURL_Image != null)
            return mURL_Image;
        else if (isImage)
            // TODO: this not a good idea... only doing it so Images can put meta-data back into it
            return this;
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

    public boolean isCached() {
        return isCached;
    }

    // todo: this should be computed internally (move code out of Images.java)
    public void setCached(boolean cached) {
        isCached = cached;
    }

    /*
    public void setPreview(Object preview) {
        // todo: ignored for now (Osid2AssetResource putting gunk here)
        //mPreview = preview;
        //out("Ignoring setPreview " + preview);
    }
    */
    
    public Icon getIcon() {
        return getIcon(null);
    }
    
    /* should deprecate: ResourceIcon could discover painter the first time it paints */
    public synchronized Icon getIcon(java.awt.Component painter) {

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
            mIcon = new tufts.vue.ui.ResourceIcon(this, 32, 32, painter);
        }
        return mIcon;
    }
        
    // TODO: calling with a different width/height only changes the size of
    // the existing icon, thus all who have reference to this will change!
    //public Icon getIcon(int width, int height) {
        //mIcon.setSize(width, height);

    
    /*
    public void setIcon(Icon i) {
        mIcon = i;
    }
    */

    public String toString() {
        if (mProperties == null)
            return getSpec();
        else
            return getSpec();// + " " + mProperties;
    }


    
    

    /*
    public void setPreview(JComponent preview) {
        this.preview = preview;
    }

    public JComponent getPreview() {
        return this.preview;
    }
    */

    
    
      
    /*

    public boolean isSelected(){
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

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


    protected void out(String s) {
        System.err.println(getClass().getName() + "@" + Integer.toHexString(hashCode()) + ": " + s);
    }

    public static void main(String args[]) {
        String rs = args.length > 0 ? args[0] : "/";
        
        DEBUG.Enabled = true;
        DEBUG.DND = true;

        URLResource r = new URLResource(rs);
        System.out.println("Resource: " + r);
        System.out.println("URL: " + r.asURL());
        r.displayContent();
    }
    
    
}
