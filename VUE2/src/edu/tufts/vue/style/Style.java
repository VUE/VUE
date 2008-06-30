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

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.style;

import java.awt.*;
import java.awt.font.*;
import java.util.*;

public abstract class Style {
    public static final String[] DEFAULT_FONT_KEYS = { "font-family","font-size","font-color"};
    public static final String[] DEFAULT_FONT_VALUES = {"Arial","12pt","#000000"};
    public static final int DEFAULT_FONT_FAMILY_KEY = 0;
    public static final int DEFAULT_FONT_SIZE_KEY = 1;
    public static final String FONT_COLOR_KEY = "font-color";
    public static final String FONT_SIZE_KEY = "font-size";
    public static final String FONT_FAMILY_KEY = "font-family";
    public static final String FONT_STYLE_KEY = "font-style";
    public static final String FONT_WEIGHT_KEY = "font-weight";
    public static final int LENGTH_PREFIX = 4;
    public static final String OPEN="{";
    public static final String CLOSE="}";
    public static final String SEMI=";";
    public static final String COLON=":";
    String name;
    private Map<String,String>  attributes =  new HashMap();
    
    public abstract org.osid.shared.Type getType();
    public String  getName() {
        return name;
    }
    
    public void setAttributes(Map<String,String> attributes) {
        this.attributes = attributes;
    }
    
    public Map<String,String> getAttributes() {
        return this.attributes;
    }
    
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    public void setAttribute(String key,String value) {
        if(this.attributes.containsKey(key)){
            this.attributes.remove(key);
        }
        this.attributes.put(key,value);
    }
    
    public Font getFont() {
        Font f = new Font(DEFAULT_FONT_VALUES[DEFAULT_FONT_FAMILY_KEY],Font.PLAIN,10);
        // setting the font color
        Map fa = new HashMap();
        // Font color is not part of the font it is handled in 
        /*
        if(attributes.get(FONT_COLOR_KEY) != null) {
            fa.put(TextAttribute.FOREGROUND,ShorthandParser.parseFontColor(attributes.get(FONT_COLOR_KEY)));
        }
         */
        if(attributes.get(FONT_SIZE_KEY) != null) {
            fa.put(TextAttribute.SIZE,ShorthandParser.parseFontSize(attributes.get(FONT_SIZE_KEY)));
        }
        if(attributes.get(FONT_FAMILY_KEY) != null) {
            fa.put(TextAttribute.FAMILY,attributes.get(FONT_FAMILY_KEY));
        }
        if(attributes.get(FONT_STYLE_KEY) != null) {
            fa.put(TextAttribute.POSTURE,ShorthandParser.parseFontStyle(attributes.get(FONT_STYLE_KEY)));
        }
        if(attributes.get(FONT_WEIGHT_KEY) != null) {
            fa.put(TextAttribute.WEIGHT, ShorthandParser.parseFontWeight(attributes.get(FONT_WEIGHT_KEY)));
        }
        f = f.deriveFont(fa);
        return f;
    }
    void setDefaultAttributes(){
        for(int i = 0;i<DEFAULT_FONT_KEYS.length;i++) {
            setAttribute(DEFAULT_FONT_KEYS[i],DEFAULT_FONT_VALUES[i]);
        }
    }
    public  String toCSS() {
        String s = new String();
        s += name+OPEN+"\n";
        Set<String> keys= attributes.keySet();
        for(String key: keys) {
            s += key+":"+attributes.get(key)+";";
        }
        s +="\n"+CLOSE+"\n";
        return s;
    }
    
    public static String colorToHex(Color color) {
        
        String colorstr = new String("#");
        
        // Red
        String str = Integer.toHexString(color.getRed());
        if (str.length() > 2)
            str = str.substring(0, 2);
        else if (str.length() < 2)
            colorstr += "0" + str;
        else
            colorstr += str;
        
        // Green
        str = Integer.toHexString(color.getGreen());
        if (str.length() > 2)
            str = str.substring(0, 2);
        else if (str.length() < 2)
            colorstr += "0" + str;
        else
            colorstr += str;
        
        // Blue
        str = Integer.toHexString(color.getBlue());
        if (str.length() > 2)
            str = str.substring(0, 2);
        else if (str.length() < 2)
            colorstr += "0" + str;
        else
            colorstr += str;
        
        return colorstr;
    }
    
    /**
     * Convert a "#FFFFFF" hex string to a Color.
     * If the color specification is bad, an attempt
     * will be made to fix it up.
     */
    public  static final Color hexToColor(String value) {
        String digits;
        int n = value.length();
        value = value.replaceAll(" ","");
        if (value.startsWith("#")) {
            digits = value.substring(1, Math.min(value.length(), 7));
        } else {
            digits = value;
        }
        String hstr = "0x" + digits;
        Color c;
        try {
            c = Color.decode(hstr);
        } catch (NumberFormatException nfe) {
            c = null;
        }
        return c;
    }
    
    /**
     * Convert a color string such as "RED" or "#NNNNNN" or "rgb(r, g, b)"
     * to a Color.
     */
    public static Color stringToColor(String str) {
        Color color = null;
        
        if (str.length() == 0)
            color = Color.black;
        else if (str.startsWith("rgb(")) {
            color = parseRGB(str);
        } else if (str.charAt(0) == '#')
            color = hexToColor(str);
        else if (str.equalsIgnoreCase("Black"))
            color = hexToColor("#000000");
        else if(str.equalsIgnoreCase("Silver"))
            color = hexToColor("#C0C0C0");
        else if(str.equalsIgnoreCase("Gray"))
            color = hexToColor("#808080");
        else if(str.equalsIgnoreCase("White"))
            color = hexToColor("#FFFFFF");
        else if(str.equalsIgnoreCase("Maroon"))
            color = hexToColor("#800000");
        else if(str.equalsIgnoreCase("Red"))
            color = hexToColor("#FF0000");
        else if(str.equalsIgnoreCase("Purple"))
            color = hexToColor("#800080");
        else if(str.equalsIgnoreCase("Fuchsia"))
            color = hexToColor("#FF00FF");
        else if(str.equalsIgnoreCase("Green"))
            color = hexToColor("#008000");
        else if(str.equalsIgnoreCase("Lime"))
            color = hexToColor("#00FF00");
        else if(str.equalsIgnoreCase("Olive"))
            color = hexToColor("#808000");
        else if(str.equalsIgnoreCase("Yellow"))
            color = hexToColor("#FFFF00");
        else if(str.equalsIgnoreCase("Navy"))
            color = hexToColor("#000080");
        else if(str.equalsIgnoreCase("Blue"))
            color = hexToColor("#0000FF");
        else if(str.equalsIgnoreCase("Teal"))
            color = hexToColor("#008080");
        else if(str.equalsIgnoreCase("Aqua"))
            color = hexToColor("#00FFFF");
        else
            color = hexToColor(str); // sometimes get specified without leading #
        return color;
    }
    
    /**
     * Parses a String in the format <code>rgb(r, g, b)</code> where
     * each of the Color components is either an integer, or a floating number
     * with a % after indicating a percentage value of 255. Values are
     * constrained to fit with 0-255. The resulting Color is returned.
     */
    private static Color parseRGB(String string) {
        // Find the next numeric char
        int[] index = new int[1];
        
        index[0] = 4;
        int red = getColorComponent(string, index);
        int green = getColorComponent(string, index);
        int blue = getColorComponent(string, index);
        
        return new Color(red, green, blue);
    }
    
    /**
     * Returns the next integer value from <code>string</code> starting
     * at <code>index[0]</code>. The value can either can an integer, or
     * a percentage (floating number ending with %), in which case it is
     * multiplied by 255.
     */
    private static int getColorComponent(String string, int[] index) {
        int length = string.length();
        char aChar;
        
        // Skip non-decimal chars
        while(index[0] < length && (aChar = string.charAt(index[0])) != '-' &&
                !Character.isDigit(aChar) && aChar != '.') {
            index[0]++;
        }
        
        int start = index[0];
        
        if (start < length && string.charAt(index[0]) == '-') {
            index[0]++;
        }
        while(index[0] < length &&
                Character.isDigit(string.charAt(index[0]))) {
            index[0]++;
        }
        if (index[0] < length && string.charAt(index[0]) == '.') {
            // Decimal value
            index[0]++;
            while(index[0] < length &&
                    Character.isDigit(string.charAt(index[0]))) {
                index[0]++;
            }
        }
        if (start != index[0]) {
            try {
                float value = Float.parseFloat(string.substring
                        (start, index[0]));
                
                if (index[0] < length && string.charAt(index[0]) == '%') {
                    index[0]++;
                    value = value * 255f / 100f;
                }
                return Math.min(255, Math.max(0, (int)value));
            } catch (NumberFormatException nfe) {
                // Treat as 0
            }
        }
        return 0;
    }
    public static final Style getStyle(String reference, Map<String,Style> map) {
        Set keys = map.keySet();
        Iterator<String> i = keys.iterator();
        while(i.hasNext()) {
            String key = i.next();
            if(key.substring(LENGTH_PREFIX+1).equals(reference)){
                return map.get(key);
            }
        }
        return getDefaultStyle();
    }
    
    public static final Style getDefaultStyle() {
        return NodeStyle.DEFAULT_NODE_STYLE;
    }
    public String toString() {
        return attributes == null ? "null" : attributes.toString();
    }
    
}

