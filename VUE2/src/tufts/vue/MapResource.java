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

public class MapResource implements Resource, XMLUnmarshalListener  {
    static final long SIZE_UNKNOWN = -1;
    public static java.awt.datatransfer.DataFlavor resourceFlavor;
    // constats that define the type of resource
    static {
        try{
            resourceFlavor = new java.awt.datatransfer.DataFlavor(Class.forName("tufts.vue.Resource"),"Resource");
        } catch(Exception ex) {ex.printStackTrace();}
    }
    private long referenceCreated; // this currently meaningless -- gets set every time -- is there anything meaningful here?
    private long accessAttempted;
    private long accessSuccessful;
    private long size = SIZE_UNKNOWN;
    protected transient boolean selected = false;
    private String spec = "";
    private int type;
    private JComponent viewer;
    private JComponent preview = null;
    
    /** the metadata property map **/
    protected Properties mProperties = new Properties();
    
    /** property name cache **/
    private String [] mPropertyNames = null;
    
    /** an optional resource title */
    protected String mTitle;
    
    private URL url = null;

    private boolean mXMLrestoreUnderway;
    private ArrayList mXMLpropertyList;
    
    // for castor to save and restore
    
    // we REALLY need to get rid of this constructor: all the code
    // expects SPEC to be non-null!  Castor won't let us though...
    public MapResource() {
        this.type =  Resource.NONE;
        mXMLrestoreUnderway = true;
        mXMLpropertyList = new ArrayList();
    }
    
    public MapResource(String spec) {
        mXMLrestoreUnderway = false;
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
                if (DEBUG.DND || labelIsTitle)
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
        System.out.println(this + " prop vector " + mXMLpropertyList);
        try {
            System.out.println("*** Opening connection to " + _url);
            markAccessAttempt();

            Properties metaData = scrapeHTMLmetaData(_url.openConnection(), 2048);
            System.out.println("*** Got meta-data " + metaData);
            markAccessSuccess();
            String title = metaData.getProperty("title");
            if (title != null) {
                setProperty("title", title);
                title = title.replace('\n', ' ').trim();
                setTitle(title);
            }
            try {
                setSize(Integer.parseInt((String) getProperty("contentLength")));
            } catch (Exception e) {}

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("HTML meta-data scrape failed of " + _url);
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
        
        setProperty("contentType", contentType);
        setProperty("contentEncoding", contentEncoding);
        if (contentLength >= 0)
            setProperty("contentLength", contentLength);
        
        if (contentType.toLowerCase().startsWith("text/html") == false) {
            System.err.println("*** contentType [" + contentType + "] not HTML; terminating meta-data scan");
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
                metaData.put("title", title);
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
     * This method sets a property value. 
     * Does nothing if either key or value is null.
     **/
    public void setProperty(String key, Object value) {
        /** invalidate our dumb cache of names if we add a new one **/
        /*
          BAD IDEA -- whenever we add a new propery value, it will clear out all the others!!
        if( !mProperties.containsKey( pName) ) {
            mPropertyNames = null;
        }
        */
        System.out.println(this + " setProperty key=" + key + " value[" + value + "]");
        if (key != null && value != null)
            mProperties.put(key, value);
    }

    public void setProperty(String key, int value) {
        //setProperty(key, new Integer(value)); // PropertiesEditor bombs out on anything but string
        setProperty(key, new Integer(value).toString());
    }
    
    /**
     * getPropertyValue
     * This method returns a value for the given property name.
     * @param pName the property name.
     * @return Object the value
     **/
    public Object getProperty(String pName) {
        Object value = null;
        value = mProperties.get(pName);
        return value;
    }
    
    
    public Properties getProperties() {
        return mProperties;
    }
    
    public void setProperties(Properties pProperties) {
        System.out.println("SET PROPERTIES " + this + " " + pProperties);
        new Throwable("setProperties").printStackTrace();
        this.mProperties = pProperties;
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
    
    
    public java.util.List getPropertyList() {
        
        if (mXMLrestoreUnderway == false) {

            if (mProperties.size() == 0) // a hack for castor to work
                return null;

            mXMLpropertyList = new ArrayList();

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
    }
    public void XML_completed()
    {
        if (DEBUG.CASTOR) System.out.println(this + " XML COMPLETED");
        Iterator i = mXMLpropertyList.iterator();
        while (i.hasNext()) {
            final PropertyEntry entry = (PropertyEntry) i.next();
            final Object key = entry.getEntryKey();
            final Object value = entry.getEntryValue();
            setProperty((String)key, value);
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

    public Object getContent()
        throws IOException, MalformedURLException
    {
        if (isImage()) {
            URL u = toURL();
            ImageIcon imageIcon = new ImageIcon(u);
            setProperty("width", imageIcon.getIconWidth());
            setProperty("height", imageIcon.getIconHeight());
            System.out.println("LWImage size " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight() + " from " + u);
            return imageIcon;
        } else {
            return getContentData();
        }
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
