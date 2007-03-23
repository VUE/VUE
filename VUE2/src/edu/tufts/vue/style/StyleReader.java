/*
 * StyleReader.java
 *
 * Created on February 5, 2007, 12:13 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.style;


import javax.swing.text.*;
import javax.swing.text.html.*;
import java.util.*;
import java.net.*;

public class StyleReader {
    
    public static final String NODE_PREFIX = "node";
    public static final String LINK_PREFIX = "link";
    
    public static void readStyles(String lookupKey) {
        //StyleSheet styles = new StyleSheet();
        //styles.importStyleSheet(tufts.vue.VueResources.getURL(lookupKey));
        //getStyleNames(styles);
        CSSParser parser = new CSSParser();
        parser.parse(tufts.vue.VueResources.getURL(lookupKey));
    }
    
    public static void readCSS(URL url) {
        try {
          CSSParser parser = new CSSParser();
          parser.parse(url);
        }catch(Exception ex) {
            System.out.println("StyleReader.readCSS"+ex);
        }
        
    }
    
}
