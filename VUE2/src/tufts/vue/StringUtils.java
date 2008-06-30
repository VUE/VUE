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

/*
 * StringUtils.java
 *
 * Created on December 24, 2003, 11:09 AM
 */

package tufts.vue;
import java.util.*;

/**
 *  Implements a variety of string utility methods.  Some of these might be included in
 *  various Java utility packages, but in a more complicated form involving regular expressions.
 *
 * @author  Mark Norton
 */
public abstract class StringUtils {
    
    /** Creates a new instance of StringUtils */
    public StringUtils() {
    }
    
    /**
     *  Create a list of strings from the master string provided by parsing out
     *  sub-strings based on the delimiter passed.
     *
     *  @author Mark Norton
     */
    private static Vector pExplode (char[] str, char delim) {
        Vector list = new Vector(100);
        
        //  Parse out sub-strings based on delim.
        StringBuffer temp = new StringBuffer();
        for (int i=0; i<str.length; i++) {
            
            //  Check for the end of a sub-string.
            if (str[i] == delim) {
                list.add (new String(temp.toString().trim()));     //  Add the sub-string to the list.
                temp.delete (0, temp.length());             //  Flush the temp string.
            }
            
            //  Check for last sub-string.
            else if (i == str.length-1) {
                temp.append (str[i]);
                list.add (new String(temp.toString().trim()));
                temp.delete (0, temp.length());             //  Flush the temp string.
            }
            
            //  Otherwise just copy the character.
            else
                temp.append (str[i]);
        }
        
        return list;
    }
    
    /**
     *  Create a list of strings from the String provided by parsing out
     *  sub-strings based on the delimiter passed.
     *
     *  @author Mark Norton
     */
    public static Vector explode(String str, char delim) {
        char [] string = str.toCharArray();
        return pExplode (string, delim);
    }
    
    /**
     *  Create a list of strings from the StringBuffer provided by parsing out
     *  sub-strings based on the delimiter passed.
     *
     *  @author Mark Norton
     */
    public static Vector explode(StringBuffer buf, char delim) {
        char [] string = buf.toString().toCharArray();
        return pExplode (string, delim);
    }
    
    /**
     *  Create a list of strings from the array of characters provided by parsing out
     *  sub-strings based on the delimiter passed.
     *
     *  @author Mark Norton
     */
    public static Vector explode(char[] buf, char delim) {
        return pExplode (buf, delim);
    }
    
    /**
     *  Create a String from the list of strings provided separated by the delimiter.
     *
     *  @author Mark Norton
     */
    public static String implode(Vector strings, char delim) {
        StringBuffer buf = new StringBuffer();
        
        for (int i=0; i<strings.size(); i++) {
            buf.append ((String)strings.elementAt(i));
            buf.append (delim);
        }
        
        return buf.toString();
    }
    
}
