/*
 * Dataset.java
 *
 * Created on July 15, 2008, 5:40 PM
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
 */
package edu.tufts.vue.dataset;


import java.util.*;
import java.io.*;

public class Dataset {
    
    public static final int MAX_SIZE = tufts.vue.VueResources.getInt("dataset.maxSize");
    public static final int MAX_LABEL_SIZE = 40;
    String label;
    ArrayList<String> heading;
    ArrayList<ArrayList<String>> rowList;
    /** Creates a new instance of Dataset */
    public Dataset() {
    }
    
    public void load(String fileName) throws Exception {
        rowList = new ArrayList<ArrayList<String>>();
        this.label = fileName;
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        int count = 0;
        while((line=reader.readLine()) != null && count <MAX_SIZE) {
            ArrayList<String> row = new ArrayList<String>();
            String[] words = line.split(",");
            for(int i =0;i<words.length;i++) {
                if(words[i].length() > MAX_LABEL_SIZE) {
                    row.add(words[i].substring(0,MAX_LABEL_SIZE)+"...");
                } else {
                    row.add(words[i]);
                }
            }
            rowList.add(row);
            count++;
        }
        reader.close();
    }
    
    public ArrayList<ArrayList<String>> getRowList() {
        return rowList;
    }
}
