
package tufts.vue;


import java.util.*;

import java.text.*;
import java.awt.*;
import javax.swing.*;
import java.net.URL;

/**
 * VueResources Class
 *
 * This class provides a central access method to get a variety of
 * resource types.  It also can be modified to support caching of
 * of resources forperformance.
 *
 **/
public class VueResources
{
  

	/** Resource Bundle **/
	protected static ResourceBundle sResourceBundle =  ResourceBundle.getBundle("tufts.vue.VueResources");
 

	
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
	public static ImageIcon getImageIcon(String pLookupKey)  {
		ImageIcon icon = null;   // (ImageIcon) sIconDict. getString(pLookupKey);
		String  str = null;
		URL resource = null;
		
		debug("Loading icon: "+pLookupKey);
		try {
			str = getString( pLookupKey);
			if( str != null) {
				debug("  value: "+ str);
				resource = sResourceBundle.getClass().getResource( str) ;        //  new URL(  urlStr );
				if (resource != null) {
					icon = new ImageIcon( resource);
					
					if ( icon.getImageLoadStatus() != MediaTracker.COMPLETE)
						alert("Unable to load image resource " + str +"; URL = '" + resource + "'");
					}
				}
			
		} catch (Exception e) {
			alert("  !!! failed to lead due to "+ e.toString() );
			}
		return icon;
		}
        
        public static URL getURL(String pLookupKey) {
            URL url = null;
            try {
                url = sResourceBundle.getClass().getResource(getString(pLookupKey));
                System.out.println("URL found for plookupkey = "+pLookupKey+"  : "+url);
            } catch (Exception e) {
			alert("  !!! failed to lead due to "+ e.toString() );
            }
            return url;
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
		try {
			return sResourceBundle.getString(pLookupKey);
		}
		catch (MissingResourceException mre) {
			alert("!!! Warning: Missing string resource "+pLookupKey );
			}
		return null;
	}

	
	/**
	 * getStringArray()
	 * This method returns a String array based on the string in
	 * the properties file.  Commas may not be used in the string.
	 * There is no escapeingo of commas at this point.
	 * Format:  myStrings=File,Edit,Windows,Help
	 * 
	 * @param pLookupKey - the key in the properties file
	 * @return String [] the array
	 **/
	public static String [] getStringArray(String pLookupKey) {
		
		String [] retValue = null;
		String s = getString( pLookupKey);
		if( s != null) {
			retValue = s.split(",");
			}

		return retValue;
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
	public static int [] getIntArray( String pLookupKey) {
		
		int [] retValue = null;
		String [] s = getStringArray( pLookupKey);
		
		if( s != null) {
			retValue = new int[ s.length];
			Integer intValue = null;
			for( int i=0; i<s.length; i++) {
				intValue = new Integer( s[i]);
				retValue[i] = intValue.intValue() ; 
				}
			}
		return retValue;
	}



	/**
	 * getInt
	 * This returns an int based on the int at the lookup key.
	 * Format: myInt=123
	 * @param pLookupKey - the lookup key
	 * @returns int - the int value in the properties file
	 **/
	static public int getInt( String pLookupKey) {
		
		int retValue = 0;
		String s = getString( pLookupKey);
		
		if( s != null) {
			Integer i = new Integer( s );
			retValue = i.intValue();
			}
	return retValue;
	}

	
    /**
     * getInt
     * Returns an int value if one is found for the given key,
     * otherwise the default value.
     * @param pLookupKey - the lookup key
     * @param pDefault - value to return if none found under key
     * @return int - value found or default
     */
    static public int getInt(String pLookupKey, int pDefault)
    {
        int retValue = pDefault;
        String s = getString(pLookupKey);
        if (s != null) {
            Integer i = new Integer(s);
            retValue = i.intValue();
        }
	return retValue;
    }

    /**
     * getChar
     * Gets a char value for the given key.
     * @param pLookupKey - the lookup key
     * @return char - value found or 0 if key not found
     */
    static public char getChar(String pLookupKey)
    {
        char retValue = 0;
        String s = getString(pLookupKey);
        if (s != null)
            retValue = s.charAt(0);
	return retValue;
    }


	/**
	 * getFont()
	 * This method gets a Font based on the Font string in the
	 * the properties file.  Use formate:  
	 *   myFont=fontname,plain|bold|italic|bolditalic,size
	 * 
	 *
	 * @param pLookupKey the string lookupkey in the properties file
	 * @return Font the Font, or null if missing
	 **/
	static public Font getFont( String pLookupKey) {
		
		Font font = null;
		
		try {
			String [] strs  = getStringArray( pLookupKey);
			if( (strs != null)  && (strs.length == 3) ) {
				String fontName = strs[0];
				int style = 0;
				if( strs[1].equals("bold") ) {
					style = Font.BOLD;
					}
				else
				if( strs[1].equals("italic") ) {
					style = Font.ITALIC;
				}
				else
				if( strs[1].equals("bolditalic") ) {
					style = Font.BOLD + Font. ITALIC;
				}
				else {
					style = Font.PLAIN;
				}
				
				Integer size = new Integer( strs[2] );
				font = new Font( fontName, style, size.intValue()  );
				}
		} catch (Exception e) {
			alert("Missing or malformed font with key: "+pLookupKey);
		}
		return font;
	}


	/**
	 * getColor()
	 * This method gets a color based on the color string in the
	 * the properties file.  Use formate:  myColor=rgbHex
	 * grayColor=4F4F4F
	 * blue=FF
	 *
	 * @param pLookupKey the string lookupkey in the properties file
	 * @return Color the color, or null if missing
	 **/
	static public Color getColor( String pLookupKey) {
		
		Color retValue = null;
		
		try {
			String str = sResourceBundle.getString( pLookupKey);
			if( str != null) {
				Integer intVal =  Integer.valueOf(str, 16);
				 
				retValue = new Color( intVal.intValue() );
				}
		} catch (Exception e) {
			alert("Missing Color resource: "+pLookupKey);
		}
		return retValue;
	}


	/**
	 * getColorArray()
	 * This method gets a color based on the color string in the
	 * the properties file.  Use formate:  myColor=rgbHex
	 * grayColor=4F4F4F,BBCCDD,FFAAFF
	 * blue=FF
	 *
	 * @param pLookupKey the string lookupkey in the properties file
	 * @return Color[]  the colors, or null if missing
	 **/
	static public Color [] getColorArray( String pLookupKey) {
		
		Color [] retValue = null;
		
		try {
			String []  strs = getStringArray( pLookupKey);
			if( strs != null) {
				int len = strs.length;
				retValue = new Color [ len];
				
				for( int i=0; i< len; i++) {
					Integer intVal =  Integer.valueOf(strs[i], 16);
				 
					retValue[i] = new Color( intVal.intValue() );
					}
				}
		} catch (Exception e) {
			alert("Missing Color resource: "+pLookupKey);
			retValue = null;
		}
		return retValue;
	}



    /**
     * getBool
     * Usage: flag=true
     *
     * @param pLookupKey the string lookupkey in the properties file
     * @return true if found and is set to "true"
     **/
    static public boolean getBool(String pLookupKey)
    {
        boolean retValue = false;
        try {
            String str = sResourceBundle.getString( pLookupKey);
            if (str != null)
                retValue = str.equalsIgnoreCase("true");
        } catch (Exception e) {
            alert("Unknown bool resource: "+pLookupKey);
        }
        return retValue;
    }


	/***
	 * getMessageString
	 * Fetches a message resource string the  bundle 
	 * and fills in the parameters
	 *
	 * @param pLookupKey the lookup key
	 * @param pArgs an object array of parameter strings
	 *
	 * @returns the formatted string
	 **/
	static public String getMessageString (String pLookupKey, Object[] pArgs)
	{
		String msg = VueResources.getString( pLookupKey);
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
			pObj.setFont( font);
		if( background != null)
			pObj.setBackground( background);	
	
	}
	
	static private boolean sDebug = false;
	
	static protected void debug( String pStr) {
		if( sDebug) System.out.println( pStr);
	}
}
