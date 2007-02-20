/*
 * StyleMap.java
 *
 * Created on February 5, 2007, 11:37 AM
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

import java.util.*;
import java.io.*;
/* This class only has static methods and contains a HashMap of all styles used in VUE.
 */

public class StyleMap {
    public static final String CSS_EXTENSION =".css";
    static  Map<String,Style> m = Collections.synchronizedMap(new HashMap());
    
    public static final Style getStyle(String key) {
        return m.get(key);
    }
    
    private  static final void addStyle(String key,Style style) {
        m.put(key,style);
    }
    
    public static final void addStyle(Style style) {
        m.put(style.getName(),style);
    }
    public static final void remove(String key) {
        m.remove(key);
    }
    public static final void removeAll() {
        m.clear();
    }
    public static final boolean containsKey(String key) {
        return m.containsKey(key);
    }
    public static final boolean containsStyle(String style) {
        return m.containsValue(style);
    }
    public static final int size(){
        return m.size();
    }
    public static final Set keySet() {
        return m.keySet();
    }
    public static String printStyles() {
        String s  = new String();
        Collection<Style> c = m.values();
        Iterator<Style> i = c.iterator();
        while(i.hasNext()){
            s += i.next().toString();
        }
        return s;
    }
    
    public static String styleToCSS() {
        String s = new String();
        Collection<Style> c = m.values();
        for(Style style: c) {
            s +=  style.toCSS();
        }
        return s;      
    }
    
    public static String saveToUniqueUserFile()  throws IOException {
        String fileName = edu.tufts.vue.util.GUID.generate()+CSS_EXTENSION;
        Writer fileWriter = new BufferedWriter(new FileWriter(tufts.vue.VueUtil.getDefaultUserFolder()+fileName));
        fileWriter.write(styleToCSS());
        return fileName;
    }
    
    public static void readFromUniqueUserFile(String fileName) thows IOException{
        
    }
}

