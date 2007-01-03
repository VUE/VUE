/*
 * TestResources.java
 *
 * Created on December 13, 2006, 11:38 AM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

/**
 * This class is used to get test properties saveed int TestResources.properties file
 *
 */

package edu.tufts.vue;

import java.util.*;
import java.net.*;
import java.io.File;

public class TestResources {
    protected static final ResourceBundle sResourceBundle =  ResourceBundle.getBundle("edu.tufts.vue.TestResources");
    
    protected static Map Cache = new HashMap();
    
    /**
     * This method returns the String from a properties file
     * for the given lookup key.
     * Format:  myString=Some Nice Message String
     * @return String - the result String, null if not found
     **/
    public final static String getString(String key) {
        String result = null;
        try {
            result = sResourceBundle.getString(key);
        } catch (MissingResourceException mre) {
        }
        return result;
    }
    
    public final static String getString(String key, String defaultString) {
        String s = getString(key);
        return s == null ? defaultString : s;
    }
     public static URL getURL(String pLookupKey) 
    {       
        URL url = null;
            
        try {
            url = sResourceBundle.getClass().getResource(getString(pLookupKey));
        } catch (Exception e) {
            System.out.println("  !!! failed to lead due to "+ e.toString() );    
        }    
        
        return url;
    }
    
}
