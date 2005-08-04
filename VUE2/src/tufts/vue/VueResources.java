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

import java.text.*;
import java.awt.*;
import javax.swing.*;
import java.net.URL;
import java.io.File;

/**
 * VueResources Class
 *
 * This class provides a central access method to get a variety of
 * resource types.  It also can be modified to support caching of
 * of resources for performance (todo: yes, implement a result cache).
 *
 **/
public class VueResources
{
    /** Resource Bundle **/
    //protected static final ResourceBundle VueResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources");
    //protected static final ResourceBundle NarraVisionResourceBundle;
    protected static final ResourceBundle sResourceBundle;

    protected static Map Cache = new HashMap();

    static {

        String featureSet = null;
        String classPath = null;

        try {
            featureSet = System.getProperty("tufts.vue.features");
            classPath = System.getProperty("java.class.path");
            System.out.println("CLASSPATH: " + classPath);
        } catch (java.security.AccessControlException e) {
            System.err.println("Can't access system properties: applet assumed");
        }

        boolean NarraVision = false;
            
        if (featureSet != null && featureSet.equalsIgnoreCase("NarraVision")
            || classPath != null && classPath.toLowerCase().indexOf("narravision") >= 0) {
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

        /*        
        //NarraVisionResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("en", "US", "NV"));
        NarraVisionResourceBundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale("", "", "NV"));
        sResourceBundle = NarraVisionResourceBundle;
        //sResourceBundle = VueResourceBundle;
        */
        
        //System.out.println("DEFAULT LOCALE: " + Locale.getDefault());
        //System.out.println("RESOURCE BUNDLE: " + sResourceBundle + " locale: " + sResourceBundle.getLocale());

        dumpResource("resources.vue");
        dumpResource("resources.narravision");
        //dumpResource("application.name");
        //dumpResource("application.title");

    }

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
     * getImageIcon()
     * This method returns an ImageIcon based on the file
     * specified by the properties file
     * myIcon=/my/package/myImage.gif
     * It will use the resource bulde's class to generate a URL
     * to map to local systems.
     *
     * @param pLookupKey - the key in the properties file
     *  @returns ImageIcon referenced by the resource bundle's lookupkey balue
     **/
    public static ImageIcon getImageIcon(String key)  {
        if (Cache.containsKey(key))
            return (ImageIcon) Cache.get(key);

        ImageIcon icon = null;
        String str = getString(key);
        if (str != null)
            icon = loadImageIcon(str);
        Cache.put(key, icon);
        return icon;
    }
    
    /** @return an image icon loaded from the given resource path */
    public static ImageIcon getImageIconResource(String path)  {
        if (Cache.containsKey(path))
            return (ImageIcon) Cache.get(path);
        
        ImageIcon icon = loadImageIcon(path);
        Cache.put(path, icon);
        return icon;
    }
    
    /**
     * @return Icon
     **/
    public static Icon getIcon(String key)  {
        return getImageIcon(key);
    }

    private static ImageIcon loadImageIcon(String file) {
        ImageIcon icon = null;
        //debug("\tloadImageIcon["+ file + "]");
        try {
            URL resource = sResourceBundle.getClass().getResource(file); //  new URL(  urlStr );
            //if (DEBUG.INIT) System.out.println("\tURL[" + resource + "]");
            if (resource != null) {
                icon = new ImageIcon( resource);
                if (icon.getImageLoadStatus() != MediaTracker.COMPLETE)
                    alert("Unable to load image resource " + file +"; URL = '" + resource + "'");
            }
        } catch (Exception e) {
            System.err.println(e);
            alert("  !!! failed to load image icon: " + e);
        }
        if (DEBUG.INIT) System.out.println("\tloadImageIcon[" + file + "] = " + icon);
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
            System.err.println(e);
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
            url = sResourceBundle.getClass().getResource(getString(pLookupKey));
            System.out.println("URL found for lookup key <" + pLookupKey + "> : " + url);
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
     * getString
     * This method returns the String from a properties file
     * for the given lookup key.
     * Format:  myString=Some Nice Message String
     * 
     * @param pLookupKey - the key
     * @return String - the result String, null if not found
     **/
    public final static String getString(String pLookupKey) {
        String result = null;
        try {
            result = sResourceBundle.getString(pLookupKey);
        } catch (MissingResourceException mre) {
            alert("!!! Warning: Missing string resource "+pLookupKey );
        }
        if (DEBUG.INIT) System.out.println("VueResources[" + pLookupKey + "] = " + (result==null?"null":"\"" + result + "\""));
        return result;
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


    /**
     * getFont()
     * This method gets a Font based on the Font string in the
     * the properties file.  Use formate:  
     *   myFont=fontname,plain|bold|italic|bolditalic,size
     * 
     *
     * @param key the string lookupkey in the properties file
     * @return Font the Font, or null if missing
     **/
    static public Font getFont( String key) {
        if (Cache.containsKey(key))
            return (Font) Cache.get(key);            
		
        Font font = null;
		
        try {
            String [] strs  = getStringArray( key);
            if( (strs != null)  && (strs.length == 3) ) {
                String fontName = strs[0];
                int style = 0;
                if( strs[1].equals("bold") ) {
                    style = Font.BOLD;
                } else if (strs[1].equals("italic") ) {
                    style = Font.ITALIC;
                } else if (strs[1].equals("bolditalic") ) {
                    style = Font.BOLD + Font. ITALIC;
                } else {
                    style = Font.PLAIN;
                }
                Integer size = new Integer( strs[2] );
                font = new Font( fontName, style, size.intValue()  );
            }
        } catch (Exception e) {
            alert("Missing or malformed font with key: "+key);
        }
        Cache.put(key, font);
        return font;
    }


    /**
     * getColor()
     * This method gets a color based on the color string in the
     * the properties file.  Use format:  myColor=rgbHex
     * grayColor=4F4F4F
     * blue=FF
     * or rgb decimal: e.g, : myColor=201,208,223
     *
     * @param key the string lookupkey in the properties file
     * @return Color the color, or null if missing
     **/
    static public Color getColor(String key) {
        if (Cache.containsKey(key))
            return (Color) Cache.get(key);
		
        Color value = null;
		
        try {
            String s = sResourceBundle.getString(key);
            if (s != null) {
                s.trim();
                if (s.indexOf(',') > 0) {
                    int[] rgb = _getIntArray(key, 3);
                    value = new Color(rgb[0], rgb[1], rgb[2]);
                } else {
                    value = makeColor(s);
                }
            }
        } catch (Exception e) {
            alert("Missing Color resource: "+key);
        }
        Cache.put(key, value);
        return value;
    }

    static Color makeColor(String hex) {
        if (hex.startsWith("#"))
            hex = hex.substring(1);
        boolean hasAlpha = hex.length() > 6;
        int bits = Long.valueOf(hex, 16).intValue();
        Color c = new Color(bits, hasAlpha);
        //System.out.println("From " + hex + " made " + c + " alpha=" + c.getAlpha());
        return c;
    }


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
                    value[i] = makeColor(strs[i]);
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
        if( (sDebug)  || ( get("alerts") != null) )
            System.out.println(pMsg);
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
        if (background != null && !VueUtil.isMacAquaLookAndFeel())
            pObj.setBackground(background);

        if (pObj instanceof JTabbedPane)
            pObj.setFocusable(false);
	
    }
	
    static private boolean sDebug = false;
	
    static protected void debug( String pStr) {
        if (sDebug || DEBUG.INIT) System.out.println( pStr);
    }
}
