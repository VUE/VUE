
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
			alert("Missing string resource "+pLookupKey );
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
	 * the properties file.  Use formate:  myColor=rbbInteger
	 *
	 * @param pLookupKey the string lookupkey in the properties file
	 * @return Color the color, or null if missing
	 **/
	static public Color getColor( String pLookupKey) {
		
		Color retValue = null;
		
		try {
			String str = sResourceBundle.getString( pLookupKey);
			if( str != null) {
				Integer intVal = new Integer( str);
				retValue = new Color( intVal.intValue() );
				}
		} catch (Exception e) {
			alert("Missing Color resource: "+pLookupKey);
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
		System.out.println(pMsg);
	}
	
	static private boolean sDebug = true;
	
	static protected void debug( String pStr) {
		if( sDebug) alert( pStr);
	}
}