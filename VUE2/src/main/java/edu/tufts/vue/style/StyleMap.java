/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.util.*;
import java.io.*;
import java.net.*;
/* This class only has static methods and contains a HashMap of all styles used in VUE.
 */

public class StyleMap {
    public static final String CSS_EXTENSION =".css";
    static  Map<String,Style> m = Collections.synchronizedMap(new HashMap());
    
    public static Style getStyle(String key) {
        return m.get(key);
    }
    
    private  static void addStyle(String key, Style style) {
        m.put(key,style);
    }
    
    public static void addStyle(Style style) {
        m.put(style.getName(),style);
    }
    public static void remove(String key) {
        m.remove(key);
    }
    public static void removeAll() {
        m.clear();
    }
    public static boolean containsKey(String key) {
        return m.containsKey(key);
    }
    public static boolean containsStyle(String style) {
        return m.containsValue(style);
    }
    public static int size(){
        return m.size();
    }
    public static Set keySet() {
        return m.keySet();
    }

    public static Set<Map.Entry<String,Style>> entrySet() {
        return m.entrySet();
    }
    
    public static String printStyles() {
        String s  = new String();
        Collection<Style> c = m.values();
        for (Style style : c) {
            s += style.toString();
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
        Writer fileWriter = new BufferedWriter(new FileWriter(tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+fileName));
        fileWriter.write(styleToCSS());
        fileWriter.close();
        return fileName;
    }
    
    public static void readFromUniqueUserFile(String fileName) throws IOException{
        URL url = (new File(tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+fileName)).toURL();
        CSSParser parser = new CSSParser();
        parser.parse(url);
    }
}

