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

import com.sun.image.codec.jpeg.*;


/**
 * The MapResource class is handles a reference
 * to either a local file or a URL.
 */

public class MapResource implements Resource {
    static final long SIZE_UNKNOWN = -1;
    public static java.awt.datatransfer.DataFlavor resourceFlavor;
    // constats that define the type of resource
    static {
        try{
            resourceFlavor = new java.awt.datatransfer.DataFlavor(Class.forName("tufts.vue.Resource"),"Resource");
        } catch(Exception ex) {ex.printStackTrace();}
    }
    private long referenceCreated;
    private long accessAttempted;
    private long accessSuccessful;
    private long size = SIZE_UNKNOWN;
    protected transient boolean selected = false;
    private String spec = "";
    private int type;
    private JComponent viewer;
    private JComponent preview = null;
    
    /** the metadata property map **/
    protected   Map mProperties = new Properties();
    
    /** property name cache **/
    private String [] mPropertyNames = null;
    
    /** an optional resource title */
    protected String mTitle;
    
    private URL url = null;
    
    // for castor to save and restore
    Vector propertyList = null;
    
    // we REALLY need to get rid of this constructor: all the code
    // expects SPEC to be non-null!  Castor won't let us though...
    public MapResource() {
        this.type =  Resource.NONE;
    }
    
    public MapResource(String spec) {
        setSpec(spec);
        this.mTitle = spec; // why are we doing this??  title is an OPTIONAL field...
    }
    
    
    public Object toDigitalRepositoryReference() {
        return null;
    }
    
    public String toURLString() {
        String txt;
        
        // todo fixme: this pathname may have been created on another
        // platform, (meaning, a different separator char) unless
        // we're going to canonicalize everything ourselves coming
        // in...
        
        if (this.url == null) {
            // this logic shouldn't be needed: if spec can be a a valid URL, this.url will already be set
            if (spec.startsWith("file:") || spec.startsWith("http:")) {
                System.err.println(getClass() + " BOGUS MapResource: is URL, but unrecognized! " + this);
                txt = spec;
            }
            // todo: handle resource: case
            else
                txt = "file:///" + spec;
        } else
            txt = this.url.toString();

        //if (!spec.startsWith("file") && !spec.startsWith("http"))
        //    txt = "file:///" + spec;

        return txt;
    }
    
    public java.net.URL toURL()
        throws java.net.MalformedURLException
    {
        if (url == null)
            return new java.net.URL(toURLString());
        else
            return this.url;
    }
    
    public void displayContent() {
        System.out.println(getClass() + " displayContent for " + this);
        try {
            this.accessAttempted = System.currentTimeMillis();
            //if (getAsset() != null) {
            //AssetViewer a = new AssetViewer(getAsset());
            //a.setSize(600,400);
            //a.show();
            //} else
            // TODO FIX: shouldn't be different for the mac here...
            if (VueUtil.isMacPlatform())
                VueUtil.openURL(toURLString());
            else
                VueUtil.openURL(getSpec());
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
    
    public void setTitle(String title) {
        mTitle = title;
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    // todo: resource's should be atomic: don't allow post construction setSpec
    public void setSpec(final String spec) {
        //System.out.println(this + " setSpec " + spec);
        this.spec = spec;
        this.referenceCreated = System.currentTimeMillis();
        try {

            if (spec.startsWith("resource:")) {
                final String classpathResource = spec.substring(9);
                //System.out.println("Searching for classpath resource [" + classpathResource + "]");
                url = getClass().getResource(classpathResource);
            } else {
                url = new URL(spec);
            }

            /*
            String fname = url.getFile();
            System.out.println("Resource [" + spec + "] has URL [" + url + "] file=["+url.getFile()+"] path=[" + url.getPath()+"]");
            java.io.File file = new java.io.File(fname);
            System.out.println("\t" + file + " exists=" +file.exists());
            file = new java.io.File(spec);
            System.out.println("\t" + file + " exists=" +file.exists());
            */

        } catch (MalformedURLException e) {
            // Okay for url to be null: means local file
            //System.err.println(e);
            //System.out.println("Resource [" + spec + "] *** NOT A URL ***");
        } catch (Exception e) {
            //System.err.println(e);
            e.printStackTrace();
        }

        this.type = isLocalFile() ? Resource.FILE : Resource.URL;
        this.preview = null;
    }
    
    /**
     * Scan an initial chunk of our content for an HTML title tag, and if one is found, set our title
     * field to what we find there.  RUNS IN IT'S OWN THREAD.  If the give LWC's label is the same
     * as the title at the start, that is updated also.
     */
    public void setTitleFromContentAsync(final LWComponent c) {
        final URL _url;
        try {
            _url = toURL();
        } catch (Exception e) {
            return;
        }
        final boolean labelIsTitle = c.getLabel() == null || c.getLabel().equals(mTitle);

        new Thread("URL meta-data search of " + _url) {
            public void run() {
                _setTitleFromContent(_url);
                if (labelIsTitle)
                    c.setLabel(getTitle());
            }
        }.start();
    }
    
    public void setTitleFromContent() {
        URL _url = null;
        try {
            _url = toURL();
        } catch (Exception e) {}
        if (_url != null)
            _setTitleFromContent(_url);
    }
    
    private void _setTitleFromContent(URL _url) {
        try {
            System.out.println("*** Opening connection to " + _url);
            markAccessAttempt();
            URLConnection conn = _url.openConnection();
            //System.err.println("Connecting...");
            //conn.connect();
            if (DEBUG.DND && DEBUG.META) {
                System.err.println("Getting headers from " + conn);
                System.err.println("Headers: " + conn.getHeaderFields());
                //Object content = conn.getContent();
                //System.err.println("GOT CONTENT[" + content + "]"); // is stream
                //System.out.println("Content-type[" + conn.getContentType() + "]");
                //System.out.println("Content-Encoding[" + conn.getContentEncoding() + "]");
            }
            if (DEBUG.DND) System.err.println("*** getting contentType");
            String contentType = conn.getContentType();
            // note: be sure to call getContentType and don't rely on getting it from the HeaderFields,
            // as sometimes it's set by the OS for a file:/// URL when there are no header fields (no http server)
            // (actually, this is set by java via a mime type table based on file extension, or a guess based on the stream)
            if (contentType.toLowerCase().startsWith("text/html")) {
                System.err.println("*** contentType [" + contentType + "]");
            } else {
                System.err.println("*** contentType [" + contentType + "] not HTML; aborting meta-data scan");
                return;
            }
            // TODO: charset sometimes come's with the contentType
            if (DEBUG.DND) System.err.println("*** scanning for HTML meta-data...");
            String title = searchURLforTitle(conn);
            markAccessSuccess();
            // TODO: do NOT do this if it came from a .url shortcut -- we
            // already have the file-name
            if (title != null)
                setTitle(title);
        } catch (Exception e) {
            System.err.println("Resource title scrape: " + e);
        }
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
    private static final Pattern HTML_Title = Pattern.compile(".*<\\s*title[^>]*>\\s*([^<]+)", // hacked for lang=he, but too broad
                                                              Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    private static final Pattern Content_Charset = Pattern.compile(".*charset\\s*=\\s*([^\">\\s]+)",
                                                              Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    private static String searchURLforTitle(URLConnection url_conn) {
        String title = null;
        try {
            //title = searchStreamForRegex(new InputStreamReader(url_conn.getInputStream()), HTML_Title, 2048);
            title = searchStreamForRegex(url_conn.getInputStream(), HTML_Title, 2048);
            // todo: would it help our windows encoding problem if we instead coverted
            // this entire stream to UTF as it came in?  tho that's prob not it:
            // that might make sense if errors were showing at head or tail of the title
            // (perhaps clipping a multi-byte char)
            if (title == null) {
                if (DEBUG.DND) System.out.println("*** no title found");
                return null;
            }
            title = title.replace('\n', ' ').trim();
            //System.out.println("*** got title ["+title+"]");
        } catch (Exception e) {
            System.out.println(e);
        }
        return title;
    }

    /** search a stream for a single regex -- one matching group only.
     * Only search the first n bytes of input stream. */
    // TODO: break this out into a buffered stream loaded, then regex searchers on the buf
    // TODO: break out searching into looking for regex with each chunk of data we get in at least x size (e.g, 256)
    //private static String searchStreamForRegex(Reader in, Pattern regex, int maxSearchChars) {
    
    private static String searchStreamForRegex(InputStream in, Pattern regex, int maxSearchChars) {
        //InputStreamReader in = new InputStreamReader(_in);
        String result = null;
        try {
            //System.out.println("*** Searching for regex in " + in + " encode=" + in.getEncoding());
            /*
            if (!(in instanceof BufferedReader)) {
                // this handles a possibly "chunked" http stream,
                // which would only hand back, say, 23 bytes the first
                // time, as Yahoo's http server did.
                in = new BufferedReader(in, maxSearchChars);
                if (DEBUG.DND) System.out.println("*** created buffered reader " + in);
            }
            */
            //char[] buf = new char[maxSearchChars];
            byte[] buf = new byte[maxSearchChars];
            int total = 0;
            int len = 0;
            // BufferedInputStream still won't read thru a block, so we need to allow
            // a few reads here to get thru a couple of blocks, so we can get up to
            // our maxbytes (e.g., a common return chunk count is 1448 bytes, presumably related to the MTU)
            do {
                int max = maxSearchChars - total;
                len = in.read(buf, total, max);
                System.out.println("*** read " + len);
                if (len > 0) total += len;
            } while (len > 0 && total < maxSearchChars);
            if (DEBUG.DND) System.out.println("*** Got total chars: " + total);
            in.close();
            String str = new String(buf, 0, total);
            if (DEBUG.DND && DEBUG.META) System.out.println("*** String[" + str + "]");
            Matcher m = regex.matcher(str);
            //if (DEBUG.DND) System.err.println("*** got Matcher " + m);
            if (m.lookingAt()) {
                if (DEBUG.DND) System.err.println("*** found match");
                result = m.group(1);
                if (DEBUG.DND) System.err.println("*** regex found ["+result+"]");
            }

            String charset = "UTF-8"; // default

            if (result != null) {
                Matcher cm = Content_Charset.matcher(str);
                if (cm.lookingAt()) {
                    charset = cm.group(1);
                    if (DEBUG.DND) System.err.println("*** found HTML specified charset ["+charset+"]");
                    // TODO: PUT THIS CHARSET AS A PIECE OF RESOURCE META-DATA!
                }
            }

            try {
                result = new String(result.getBytes(), charset);
                if (DEBUG.DND) System.err.println("*** successfully converted from " + charset + " to local unicode");
            } catch (java.io.UnsupportedEncodingException e) {
                System.err.println("*** charset not found [" + charset + "]; defaulting to UTF-8");
                result = VueUtil.decodeUTF(result);
            }

        } catch (Throwable e) {
            System.err.println("searchStreamForRegex: " + e);
            if (DEBUG.DND) e.printStackTrace();
        }

        if (DEBUG.DND || DEBUG.Enabled) System.err.println("*** searchStreamForRegex returning [" + result + "]");
        return result;
    }
    
    
    
    /** Return exactly whatever we were handed at creation time.  We
     * need this because if it's a local file (file: URL or just local
     * file path name), we need whatever the local OS gave us as a
     * reference in order to give that to give back to openURL, as
     * it's the most reliable string to give back to the underlying OS
     * for opening a local file.  */
    
    public String getSpec() {
        return this.spec;
    }
    
    /**
     * If isLocalFile is true, this will return a file name
     * suitable to be given to java.io.File such that it
     * can be found.  Note that this may differ from getSpec.
     * If isLocalFile is false, it will return the file
     * portion of the URL, although that may not be useful.
     */
    public String getFileName() {
        if (this.url == null)
            return getSpec();
        else
            return url.getFile();
    }
    
    public boolean isLocalFile() {
        return url == null || url.getProtocol().equals("file");
        //String s = spec.toLowerCase();
        //return s.startsWith("file:") || s.indexOf(':') < 0;
    }
    
    public String getExtension() {
        String ext = "xxx";
        if (spec.startsWith("http"))
            ext = "web";
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
     * setPropertyValue+-
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
    
    public Properties getProperties() {
        return (Properties)mProperties;
    }
    
    public void setProperties(Properties pProperties) {
        System.out.println("SET PROPERTIES");
        this.mProperties = pProperties;
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
    public String toString() {
        return getSpec();
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
    
    
    
    public Vector getPropertyList() {
        propertyList = new Vector();
        if(mProperties.size() ==0) // a hack for castor to work
            return null;
        
        
        Iterator i = mProperties.keySet().iterator();
        while(i.hasNext()) {
            Object object = i.next();
            PropertyEntry entry = new PropertyEntry();
            entry.setEntryKey(object);
            entry.setEntryValue(mProperties.get(object));
            propertyList.add(entry);
        }
        if (DEBUG.Enabled) System.out.println(this + " getPropertyList: " + propertyList);
        return propertyList;
    }
    
    public void setPropertyList(Vector propertyList) {
        this.propertyList = propertyList;
        this.mProperties = new Properties();
        Iterator i = propertyList.iterator();
        while(i.hasNext()) {
            PropertyEntry entry = (PropertyEntry) i.next();
            mProperties.put(entry.getEntryKey(),entry.getEntryValue());
        }
    }
    
    public JComponent getAssetViewer(){
        
     return null;   
        
    }
    public void setAssetViewer(JComponent viewer){
        
     this.viewer = viewer;   
        
    }

    //todo: move to Resource spec & a new AbstractResource class
    public static boolean isImage(Resource r) {
        String s = r.getSpec().toLowerCase();
        // will need java advanced imageio for bmp & tiff
        return s.endsWith(".gif")
            || s.endsWith(".jpg")
            || s.endsWith(".jpeg")
            //|| s.endsWith(".bmp")
            || s.endsWith(".png")
            //|| s.endsWith(".tif")
            //|| s.endsWith(".tiff")
            ;
    }

    public boolean isImage() {
        return isImage(this);
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
                **/
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
    
    
}
