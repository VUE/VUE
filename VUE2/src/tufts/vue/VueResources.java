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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * This class provides a central access method to get a variety of
 * resource types.  It also can be modified to support caching of
 * of resources for performance (todo: yes, implement a result cache).
 *
 * @version $Revision: 1.58 $ / $Date: 2009-03-31 03:16:36 $ / $Author: vaibhav $
 *
 */
public class VueResources
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueResources.class);

    /** Resource Bundle **/
    //protected static final ResourceBundle VueResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources");
    //protected static final ResourceBundle NarraVisionResourceBundle;
    protected static final ResourceBundle sResourceBundle;
    protected static final ResourceBundle platformBundle;

    protected static Map Cache = new HashMap() {
            public Object put(Object key, Object value) {
                if (value == null) return null;
                if (DEBUG.INIT) Log.debug("cached(" + key + ") = " + value + "");
                return super.put(key, value);
            }
        };

    static {
    	
    	final class VueResourceBundle extends ResourceBundle{

//    		 List of bundles in merge bundle
    		private List<ResourceBundle> bundles;

    		/**
    		 * Constructs a resource bundle from a list of other resource bundles. If
    		 * there are duplicate keys, the key from the resource bundle with the
    		 * smallest index takes precedence.
    		 * Currently supports  merging 2 resource bundles but can be modified for
    		 * any number of resource bundles 
    		 * @param baseNameList
    		 * list of bundle base names to search for key/value pairs
    		 */
    		public VueResourceBundle(ResourceBundle baseNameList, ResourceBundle childNameList) {
    			bundles = new ArrayList<ResourceBundle>(2);
    			bundles.add(baseNameList);
    			bundles.add(childNameList);	
    		}
    		

    		
    		@Override
    		public Enumeration getKeys() {
    			 return new Enumeration(){

    		            Enumeration enumer = null;

    		            int i = 0;

    		            public boolean hasMoreElements(){

    		               boolean b = false;

    		               while (enumer == null || !(b = enumer.hasMoreElements())){

    		                  if (i >= bundles.size()){

    		                	  enumer = null;

    		                     return b;

    		                  }

    		                  enumer = ((ResourceBundle)bundles.get(i++)).getKeys();

    		              }

    		               return b;

    		            }

    		            public Object nextElement(){

    		               if (enumer == null) throw new NoSuchElementException();

    		               return enumer.nextElement();

    		            }

    		         };
    		}

    		/*
    		 * (non-Javadoc)
    		 * 
    		 * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
    		 */
    		@Override
    		protected Object handleGetObject(String key) {

    	         ResourceBundle rb = null;

    	         String val        = null;

    	         for (int i=0;i<bundles.size();i++){

    	            rb = (ResourceBundle) bundles.get(i);

    	            try{

    	               val = rb.getString(key);

    	           }catch (Exception e){}

    	            if (val != null) break;

    	         }

    	         return val;

    	      }

			    		
    	}
        if (DEBUG.INIT) tufts.Util.printStackTrace("VueResources; FYI: static init block");

        if (tufts.Util.isMacPlatform()) {
            sResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale(Locale.getDefault().getLanguage(), "", "Mac"));
            platformBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("","" ,"Mac")); 

        } /*else if (tufts.Util.isWindowsPlatform())*/ 
        else
        {
        	//System.out.println("ISO country language   "+ Locale.getDefault().getLanguage());
        	//System.out.println("ISO country code   "+ Locale.getDefault().getCountry());
        	//sResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale(Locale.getDefault().getLanguage(),Locale.getDefault().getCountry(),"Win"));
        	ResourceBundle langBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale(Locale.getDefault().getLanguage(), Locale.getDefault().getCountry()));
            platformBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("","" ,"Win"));    
            sResourceBundle = new VueResourceBundle(langBundle,platformBundle);
        } /*else {
            sResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources");
        }*/

        if (DEBUG.INIT) System.out.println("Got bundle: " + sResourceBundle
                                           + " in locale [" + sResourceBundle.getLocale() + "]");

        if (DEBUG.INIT) dumpResource("DEBUG.platform");
    }
    
    /*
    static {

        if (DEBUG.INIT) tufts.Util.printStackTrace("VueResources; FYI: static init block");

        String featureSet = null;
        String classPath = null;

        try {
            featureSet = System.getProperty("tufts.vue.features");
            classPath = System.getProperty("java.class.path");
            if (DEBUG.INIT) System.out.println("CLASSPATH: " + classPath);
        } catch (java.security.AccessControlException e) {
            System.err.println("Can't access system properties: applet assumed");
        }

        boolean NarraVision = false;
            
        if (featureSet != null && featureSet.equalsIgnoreCase("NarraVision")) {
            NarraVision = true;
        } else if (featureSet == null && classPath != null && classPath.toLowerCase().indexOf("narravision") >= 0) {
            // If running as a mac app, or explicitly from command line with -Dtufts.vue.featres=NarraVision,
            // we know to run as NarraVision.  However, if running just from a jar file on the PC,
            // we guess from classpath -- if "NarraVision" appears anywhere in it, run
            // as MIT NarraVision.  (e.g.: "MIT-NarraVision-2005-03-20.jar") will still work.
            NarraVision = true;
        }

        if (NarraVision) {
            // This will load VueResources.properties as the parent,
            // and then VueResources___NV.properties as the child, who's
            // properties will override any duplicate settings the parent.
            sResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("", "", "NV"));
        } else {
            sResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources");
        }

        //NarraVisionResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("en", "US", "NV"));
        NarraVisionResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("", "", "NV"));
        sResourceBundle = NarraVisionResourceBundle;
        //sResourceBundle = VueResourceBundle;
        
        //System.out.println("DEFAULT LOCALE: " + Locale.getDefault());
        //System.out.println("RESOURCE BUNDLE: " + sResourceBundle + " locale: " + sResourceBundle.getLocale());

        // note: as we're in a static block, will only see this if DEBUG.INIT set to true in DEBUG.java
        if (DEBUG.INIT) dumpResource("resources.vue");
        if (DEBUG.INIT) dumpResource("resources.narravision");
        //dumpResource("application.name");
        //dumpResource("application.title");
    }
    */

    public static void main(String[] args) {
    }

    private static void dumpResource(String name) {
        System.out.println(name + ": " + getString(name));
    }
    
    /**
     * Return the vue resource bundle.
     */
    public static ResourceBundle getBundle() {
        return sResourceBundle;
    }
    
    /**
     * This method returns an ImageIcon based on the file
     * specified by the properties file
     * myIcon=/my/package/myImage.gif
     * It will use the resource bulde's class to generate a URL
     * to map to local systems.
     *
     * @param pLookupKey - the key in the properties file
     *  @returns ImageIcon referenced by the resource bundle's lookupkey balue
     **/

    public static ImageIcon getImageIcon(Class clazz, String keyOrPath)  {
        //if (DEBUG.INIT && DEBUG.META) tufts.Util.printStackTrace("getImageIcon " + keyOrPath + " in " + clazz);

        String key;

        if (clazz == null)
            key = keyOrPath;
        else
            key = clazz.getName() + keyOrPath;
        
        if (Cache.containsKey(key))
            return (ImageIcon) Cache.get(key);

        ImageIcon icon = null;
        if (clazz == null) {
            String str = getString(key);
            if (str != null)
                icon = loadImageIcon(VueResources.class,str);
        } else {
            icon = loadImageIcon(clazz, keyOrPath);
        }
        Cache.put(key, icon);
        return icon;
    }
    public static ImageIcon getImageIcon(String key) {
        return getImageIcon(null, key);
    }
    private static boolean isPath(String key) {
        return key.indexOf('/') >= 0;
    }
    
    public static BufferedImage getBufferedImage(String key)
    {
    	Image i = getImage(key);
    	int w = i.getWidth(null);
    	int h = i.getHeight(null);
    	BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
    	Graphics bg = bi.createGraphics();
    	bg.drawImage(i,0,0,(ImageObserver)null);
    	bg.dispose();
    	return bi;


    }
    public static Image getImage(String key)  {
        ImageIcon icon = null;
        if (key.charAt(0) == '/')
            icon = getImageIcon(VueResources.class, "images" + key);
        else
            icon = getImageIcon(key);
        
        if (icon != null)
            return icon.getImage();
        else {
            alert("warning: didn't find Image resource with key [" + key + "]");
            return null;
        }
    }
    
    /** @return an image icon loaded from the given resource path */
    public static ImageIcon getImageIconResource(String path)  {
        if (Cache.containsKey(path))
            return (ImageIcon) Cache.get(path);
        
        ImageIcon icon = loadImageIcon(path);
        Cache.put(path, icon);
        return icon;
    }
    
    public static Icon getIcon(String key)  {
        return getImageIcon(key);
    }

    public static Icon getIcon(Class clazz, String path)  {
        return getImageIcon(clazz, path);
    }

    private static ImageIcon loadImageIcon(String file) {
        return loadImageIcon(sResourceBundle.getClass(), file);
    }

    private static ImageIcon loadImageIcon(Class clazz, String file) {
        ImageIcon icon = null;
        //debug("\tloadImageIcon["+ file + "]");
        try {
            URL resource = clazz.getResource(file);
            //if (DEBUG.INIT) System.out.println("\tURL[" + resource + "]");
            if (resource != null) {
                icon = new ImageIcon(resource);
                if (icon.getImageLoadStatus() != MediaTracker.COMPLETE)
                    alert("Unable to load image resource " + file +"; URL = '" + resource + "'");
            } else {
                alert("loadImageIcon; failed to find any resource at: " + file);
            }
        } catch (Exception e) {
            System.err.println(e);
            alert("failed to load image icon: " + e);
        }
        if (DEBUG.INIT && DEBUG.META) System.out.println("\tloadImageIcon[" + file + "] = " + icon);
        return icon;
    }

    public static Dimension getSize(String key) {
        if (Cache.containsKey(key))
            return (Dimension) Cache.get(key);

        String[] data = _getStringArray(key);
        if (data == null)
            return null;
        Dimension d = new Dimension();
        if (data.length > 0) {
            d.width = parseInt(data[0]);
            if (data.length > 1)
                d.height = parseInt(data[1]);
        }
        Cache.put(key, d);
        return d;
    }
        
    public static Cursor getCursor(String key)
    {
        String[] data = _getStringArray(key);
        if (data == null)
            return null;
        ImageIcon icon = loadImageIcon(data[0]);
        Cursor cursor = null;
        if (icon != null) {
            Point hot = new Point();
            if (data.length > 2) {
                hot.x = parseInt(data[1]);
                hot.y = parseInt(data[2]);
            }
            //System.out.println("Creating cursor for " + icon);
            //System.out.println("Creating cursor " + key + " " + Arrays.asList(data) + " with hotspot " + hot);
            cursor = Toolkit.getDefaultToolkit().createCustomCursor(icon.getImage(), hot, key+":"+icon.toString());
        }
        return cursor;
    }

    private static int parseInt(String str) {
        return parseInt(str, 0);
    }
    private static int parseInt(String str, int exceptionValue) {
        int i = exceptionValue;
        try {
            i = Integer.parseInt(str);
        } catch (Exception e) {
            Log.warn("parseInt: " + e);
            if (DEBUG.INIT) e.printStackTrace();
        }
        return i;
    }
    private static float parseFloat(String str) {
        return parseFloat(str, 0f);
    }
    private static float parseFloat(String str, float exceptionValue) {
        float f = exceptionValue;
        try {
            f = Float.parseFloat(str);
        } catch (Exception e) {
            System.err.println(e);
        }
        return f;
    }
        
    public static URL getURL(String pLookupKey) 
    {       
        URL url = null;
            
        try {
            //url =new File(sResourceBundle.getClass().getResource(getString(pLookupKey)).getFile().replaceAll("%20"," ")).toURL();
			//url = sResourceBundle.getClass().getResource(getString(pLookupKey));
			url = VueResources.class.getResource(getString(pLookupKey));
            if (DEBUG.INIT) alert("URL for key <" + pLookupKey + "> is [" + url + "]");
        } catch (Exception e) {
            alert("  !!! failed to lead due to "+ e.toString() );    
        }    
        
        return url;
    }

        
     public static File getFile(String pLookupKey) 
     {
           File file = null;
            try {
                file =new File(sResourceBundle.getClass().getResource(getString(pLookupKey)).getFile().replaceAll("%20"," ")) ;
                
                if (!file.exists())
                {
                    System.out.println("getFile is doing class loader thing hopefully YA!!");
                    //file = new File(ClassLoader.getSystemResource(getString(pLookupKey)).getFile().replaceAll("%20"," "));
                    URL url = ClassLoader.getSystemResource(getString(pLookupKey));   
                    System.out.println("the url of the " + pLookupKey + " is " + url);
                    
                    file = new File(url.getFile().replaceAll("%20"," "));
                    System.out.println("finished with " + file.toString());
                    
                    if (file == null)
                        System.err.println("error in getFile method");
                    
                    else if (!file.exists())
                    {
                        System.err.println("getting screwed");
                        file = null;
                    }
                }
                
                else
                {
                    System.out.println("file exists!!!!!!!!!!");
                }
                
                System.out.println("URL found for plookupkey = "+pLookupKey+"  : "+file);
            } catch (Exception e) {
		alert("  !!! failed to lead due to "+ e.toString() );
            }
            return file;
     }
    
    /**
     * This method returns the String from a properties file
     * for the given lookup key.
     * Format:  myString=Some Nice Message String
     * 
     * @return String - the result String, null if not found
     **/
    public final static String getString(String key) {
        String result = null;
        try {
            result = sResourceBundle.getString(key);
            if(result==null){
             result = platformBundle.getString(key);
            }
        } catch (MissingResourceException mre) {
            // FYI: we get tons of failures that are perfectly okay.
            //if (DEBUG.INIT) alert("warning: didn't find String resource with key [" + pLookupKey + "]");
        }
        if (DEBUG.INIT) {
            if (DEBUG.META || result != null)
                Log.debug("lookup(" + key + ") = " + (result==null?"null":"\"" + result + "\""));
        }
        return result;
    }

    public final static String getString(String key, String defaultString) {
        String s = getString(key);
        return s == null ? defaultString : s;
    }
	
    /**
     * getStringArray()
     * This method returns a String array based on the string in
     * the properties file.  Commas may not be used in the string.
     * There is no escapeingo of commas at this point.
     * Format:  myStrings=File,Edit,Windows,Help
     * 
     * @param pLookupKey - the key in the properties file
     * @return String [] the array -- results are cached for follow-on requests
     **/
    public static String[] getStringArray(String key) {
        if (Cache.containsKey(key))
            return (String[]) Cache.get(key);
        String[] value = _getStringArray(key);
        Cache.put(key, value);
        return value;
    }

    // non-caching string array fetch
    private static String[] _getStringArray(String key) {
        String[] value = null;
        String s = getString(key);
        if (s != null)
            value = s.split(",\\s*");
        return value;
    }
	
    /**
     * getIntArray
     * Thsi method returns an integer array from a properties resource
     * The format for the properties file is:  myInts=12,13,14,15
     * Using a "," (comma) as the separator 
     * Format:  myIntArray=123,456,789
     *
     * @param String pLookupKey the key of the property
     * #return int [] the array of ints
     **/
    public static int[] getIntArray(String key) {
        if (Cache.containsKey(key))
            return (int[]) Cache.get(key);
        int[] value = _getIntArray(key);
        Cache.put(key, value);
        return value;
    }

    private static int[] _getIntArray(String key) {
        return _getIntArray(key, 0);
    }
    // non-caching int array fetch
    private static int[] _getIntArray(String key, int minSize) {
        int[] value = null;
        String[] s = _getStringArray(key);

        if (s != null) {
            value = new int[Math.max(s.length, minSize)];
            for (int i = 0; i < s.length; i++)
                value[i] = parseInt(s[i]);
        }
        return value;
    }

    private static int[] makeIntArray(String s, int minSize) {
        int[] values = null;
        if (s != null) {
            String[] segs = s.split(",\\s*");
            values = new int[Math.max(segs.length, minSize)];
            for (int i = 0; i < segs.length; i++) {
                //System.out.println("seg"+i+"=[" + segs[i] + "]");
                values[i] = parseInt(segs[i]);
            }
        }
        return values;
    }
    
    
    public static float[] getFloatArray(String key) {
        if (Cache.containsKey(key))
            return (float[]) Cache.get(key);
        
        float[] value = null;
        String[] s = _getStringArray(key);
		
        if (s != null) {
            value = new float[s.length];
            for (int i = 0; i < s.length; i++)
                value[i] = parseFloat(s[i]);
        }
        Cache.put(key, value);
        return value;
    }



    /**
     * getInt
     * This returns an int based on the int at the lookup key.
     * Format: myInt=123
     * @param key - the lookup key
     * @returns int - the int value in the properties file
     **/
    static public int getInt( String key)
    {
        int value = 0;
        String s = getString(key);
        if (s != null)
            value = Integer.parseInt(s);
	return value;
    }

    /**
     * getDouble
     * This returns a double based on the double at the lookup key.
     * Format: myDouble=1.0
     * @param key - the lookup key
     * @returns double - the double value in the properties file
     **/
    static public double getDouble(String key)
    {
        double value = 0;
        String s = getString(key);
        if (s != null)
            value = Double.parseDouble(s);
        return value;
    }

	
    /**
     * getInt
     * Returns an int value if one is found for the given key,
     * otherwise the default value.
     * @param key - the lookup key
     * @param pDefault - value to return if none found under key
     * @return int - value found or default
     */
    static public int getInt(String key, int pDefault)
    {
        int value = pDefault;
        String s = getString(key);
        if (s != null) {
            Integer i = new Integer(s);
            value = i.intValue();
        }
	return value;
    }

    /**
     * getChar
     * Gets a char value for the given key.
     * @param key - the lookup key
     * @return char - value found or 0 if key not found
     */
    static public char getChar(String key)
    {
        char value = 0;
        String s = getString(key);
        if (s != null)
            value = s.charAt(0);
	return value;
    }

    /** see getFont(key, defaultFont) */
    static public Font getFont(String key) {
        return getFont(key, null);
    }

    /**
     * This method gets a Font based on the Font string in the
     * the properties file.  Use format:  
     *   myFont=fontname,plain|bold|italic|bolditalic,size
     * or standard java font format:
     *  myFont=Arial-BOLD-12
     *
     * @param key the string lookupkey in the properties file
     * @param defaultFont -- font to use if none found for given key
     * @return Font the Font, or null if missing or malformed
     **/
    static public Font getFont(String key, Font defaultFont) {
        if (Cache.containsKey(key))
            return (Font) Cache.get(key);            
		
        String spec = getString(key);
        Font font = defaultFont;
        
        if (spec != null) {
            try {
                if (spec.indexOf(',') > 0) {
                    // Old-style: or own font decoder
                    String[] parts = getStringArray(key);
                    String fontName = parts[0];
                    int style = 0;
                    if ("bold".equalsIgnoreCase(parts[1])) {
                        style = Font.BOLD;
                    } else if ("italic".equalsIgnoreCase(parts[1])) {
                        style = Font.ITALIC;
                    } else if ("bolditalic".equalsIgnoreCase(parts[1])) {
                        style = Font.BOLD + Font. ITALIC;
                    } else {
                        style = Font.PLAIN;
                    }
                    int size = Integer.parseInt(parts[2]);
                    font = new Font(fontName, style, size);
                } else {
                    // new style: let Font class decode it (uses dashes instead of commas)
                    font = Font.decode(spec);
                }
            } catch (Throwable t) {
                alert("Missing or malformed font with key: " + key + " " + t);
            }
        }

        Cache.put(key, font);
        return font;
    }


    /**
     * This method gets a color based on the color string in the
     * the properties file.  Use format: myColor=R,G,B or
     * myColor=4F4F4F.  E.g.,
     *
     *   myRed=FF0000
     * myGreen=00FF00
     *  myBlue=0000FF
     *   myRed=255,0,0
     * myGreen=0,255,0
     *  myBlue=0,0,255
     *
     * Leading zeros may be left off hex values.
     *
     * @param key the string lookupkey in the properties file
     * @return the Color found, or default color if dr != -1
     **/
    static public Color getColor(String key, Color defaultColor)
    {
        if (Cache.containsKey(key))
            return (Color) Cache.get(key);
		
        Color value = null;
		
        try {
            value = parseColor(getString(key));
//             String s = getString(key);
//             if (s != null) {
//                 s.trim();
//                 if (s.indexOf(',') > 0) {
//                     int[] rgb = makeIntArray(s, 3);
//                     value = new Color(rgb[0], rgb[1], rgb[2]);
//                 } else {
//                     value = makeColor(s);
//                 }
//             } 
        } catch (java.util.MissingResourceException e) {
            ; // will try and use default
        } catch (Throwable t) {
            alert("getColor: " + key + " " + t);
            if (DEBUG.INIT) t.printStackTrace();
        }
        
        if (value == null) {
            if (defaultColor != null)
                value = defaultColor;
          //if (dr >= 0)
              //value = new Color(dr, dg, db);
            else
                if (DEBUG.INIT) alert("No such resource (color): " + key);
        }
        
        Cache.put(key, value);
        return value;
    }

    public static Color parseColor(String txt) {
        if (txt == null)
            return null;
        txt = txt.trim();
        if (txt.indexOf(',') > 0)
            return parseIntColor(txt);
        else
            return parseHexColor(txt);
    }

    static Color parseIntColor(String commaText) {
        int[] rgb = makeIntArray(commaText, 3);

        if (rgb.length > 3)
            return new Color(rgb[0], rgb[1], rgb[2], rgb[3]);
        else
            return new Color(rgb[0], rgb[1], rgb[2]);

    }
    
    static Color parseHexColor(final String _hex) {
        String hex = _hex;
        
        if (hex.startsWith("#"))
            hex = hex.substring(1);

        int separateAlpha = -1;

        int i;
        if ((i=hex.indexOf('%')) > 0 && hex.length() > (i+1)) {
            final String pctTxt = hex.substring(i + 1);
            final int pctAlpha = parseInt(pctTxt, -1);
            if (pctAlpha != -1) {
                float pct = pctAlpha / 100f;
                separateAlpha = (int) (pct * 255 + 0.5);
            }
            hex = hex.substring(0, i);
        }

        final boolean hasCombinedAlpha = separateAlpha >= 0 || hex.length() > 6;
        int bits = Long.valueOf(hex, 16).intValue();

        if (separateAlpha > 0) {
            // results undefined if any exitings alpha in bits (anything in 0xff000000)
            bits &= 0xFFFFFF; // strip any alpha bits that were in the hex value
            bits |= (separateAlpha << 24);
        }
            
        final Color c = new Color(bits, hasCombinedAlpha);
        //Log.debug(String.format("From [%s] (%s) made bits=%X, sepAlpha=%d, netAlpha=%d, %s", _hex, hex, bits, separateAlpha, c.getAlpha(), c));
        return c;
    }
    
    static public Color getColor(String key) {
        return getColor(key, null);
    }
    
    /**
     * @param dr,dg,db - default R,G,B color values if key not found
     */
    static public Color getColor(String key, int dR, int dG, int dB) {
        return getColor(key, new Color(dR, dG, dB));
    }
    

    
//     static Color makeColor(String hex) {
//         if (hex.startsWith("#"))
//             hex = hex.substring(1);
//         boolean hasAlpha = hex.length() > 6;
//         int bits = Long.valueOf(hex, 16).intValue();
//         Color c = new Color(bits, hasAlpha);
//         //System.out.println("From " + hex + " made " + c + " alpha=" + c.getAlpha());
//         return c;
//     }


    /**
     * getColorArray()
     * This method gets a color based on the color string in the
     * the properties file.  Use formate:  myColor=rgbHex
     * grayColor=4F4F4F,BBCCDD,FFAAFF
     * blue=FF
     *
     * @param key the string lookupkey in the properties file
     * @return Color[]  the colors, or null if missing
     **/
    static public Color[] getColorArray(String key) {
        if (Cache.containsKey(key))
            return (Color[]) Cache.get(key);            
		
        Color [] value = null;
        try {
            String[] strs = _getStringArray(key);
            if( strs != null) {
                int len = strs.length;
                value = new Color[len];
                for (int i=0; i< len; i++) {
                    value[i] = parseColor(strs[i]);
                }
            }
        } catch (Exception e) {
            alert("Missing Color resource: "+key);
            value = null;
        }
        Cache.put(key, value);
        return value;
    }


    /**
     * getBool
     * Usage: flag=true
     *
     * @param key the string lookupkey in the properties file
     * @return true if found and is set to "true"
     **/
    static public boolean getBool(String key)
    {
        boolean value = false;
        try {
            String str = sResourceBundle.getString( key);
            if (str != null)
                value = str.equalsIgnoreCase("true");
        } catch (Exception e) {
            alert("Unknown bool resource: "+key);
        }
        return value;
    }


	/***
	 * getMessageString
	 * Fetches a message resource string the  bundle 
	 * and fills in the parameters
	 *
	 * @param key the lookup key
	 * @param pArgs an object array of parameter strings
	 *
	 * @returns the formatted string
	 **/
	static public String getMessageString (String key, Object[] pArgs)
	{
		String msg = VueResources.getString( key);
		msg = MessageFormat.format(msg, pArgs);
		return msg;
	}


    static protected void alert( String pMsg) {
        if( DEBUG.Enabled || sDebug || ( get("alerts") != null) )
            //System.out.println("VueResources: " + pMsg);
            Log.info("alert: " + pMsg);
    }
	

    static private String get( String pKey)  {
        String value = null;
        try {
            value = getBundle().getString( pKey);
        } catch( Exception e) {
        }
        return value;
    }
	
    static public void initComponent( JComponent pObj, String pKey) {
	
        Font font = getFont( pKey+".font");
        Color background = getColor( pKey + ".background");
		
        if( font != null)
            pObj.setFont(font);
        if (background != null && !tufts.vue.gui.GUI.isMacAqua())
            pObj.setBackground(background);

        if (pObj instanceof JTabbedPane)
            pObj.setFocusable(false);
	
    }
     /**
      * This method returns the formated string in the default 
      * language
      * @param arguments : variable to be placed in the string 
      * @param pattern : string retrived from the properties file.
      * @return
      */
      public static String getFormatMessage(Object[] arguments, String pattern){
		
		MessageFormat formatter = new MessageFormat(getString(pattern));
		
		if(arguments!=null){
		return formatter.format(arguments);
		}else{
			return getString(pattern);
		}
	}
	
    static private boolean sDebug = false;
	
    static protected void debug( String pStr) {
        if (sDebug || DEBUG.INIT) System.out.println( pStr);
    }
    
    
}
