/*
 * ShorthandParser.java
 *
 * Created on April 6, 2007, 10:32 AM
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
 *
 * This class contains a set of static methods to handle shorthands allowed in CSS
 * Examples: "blue"  color needs to be parsed to #00000FF
 */
package edu.tufts.vue.style;

import java.awt.*;
import java.awt.font.*;
public class ShorthandParser {
    public static final float DEFAULT_FONT_SIZE = 10;
    public static final String CSS_FONT_STYLE_VAL = "italic";
    public static final String CSS_FONT_WEIGHT_VAL = "bold";
    
    /**
     * The method returns the float value of font size defined in css file.
     */
    public static Float parseFontSize(String value){
        float fv = DEFAULT_FONT_SIZE;
         Float f = parseSize(value);
         
        if(f != null) {
             return f;
        } else {
             return new Float(fv);
        }
    }
    
    /*
     * The method returns Color
     */
    public static Float parseFontStyle(String value) {
        float fv = TextAttribute.POSTURE_REGULAR;
        if(value.equalsIgnoreCase(CSS_FONT_STYLE_VAL)) {
            fv = TextAttribute.POSTURE_OBLIQUE;
        }
        return fv;
        
    }
    public static Float parseFontWeight(String value) {
        float fv = TextAttribute.WEIGHT_REGULAR;
        if(value.equalsIgnoreCase(CSS_FONT_WEIGHT_VAL)) {

            fv = TextAttribute.WEIGHT_BOLD;
        }
        return fv;
        
    }
    
    public static Float parseSize(String value) {
         float fv;
        if(value.endsWith("pt")) {
            fv = Float.parseFloat(value.substring(0,value.length()-2)) ;
        }else if(value.endsWith("px")) {
            fv = Float.parseFloat(value.substring(0,value.length()-2)) ;
        } else  {
            fv = Float.parseFloat(value);
        }
        Float f = new Float(fv);
        return f;
    }
}