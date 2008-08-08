/*
 * Dataset.java
 *
 * Created on July 15, 2008, 5:40 PM
 *
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
 *
 */
package edu.tufts.vue.dataset;


import java.util.*;
import java.io.*;
import tufts.vue.*;
public class Dataset {
    
    public static final int MAX_SIZE = tufts.vue.VueResources.getInt("dataset.maxSize");
    public static final int MAX_LABEL_SIZE = 10000;
    String fileName;
    String label;
    ArrayList<String> heading;
    ArrayList<ArrayList<String>> rowList;
    /** Creates a new instance of Dataset */
    
    Layout layout = new ListRandomLayout(); // this is the default if no layout is set
    public Dataset() {
    }
    
    public void setLayout(Layout layout) {
        this.layout = layout;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public void setHeading(ArrayList<String> heading) {
        this.heading = heading;
    }
    public ArrayList<String> getHeading() {
        return heading;
    }
    
    
    public ArrayList<ArrayList<String>> getRowList() {
        return rowList;
    }
    
    public LWMap createMap() throws Exception{
        return layout.createMap(this,getMapName(fileName));
    }
    private  String getMapName(String fileName) {
        String mapName = fileName.substring(fileName.lastIndexOf(File.separator)+1,fileName.length());
        if(mapName.lastIndexOf(".")>0)
            mapName = mapName.substring(0,mapName.lastIndexOf("."));
        if(mapName.length() == 0)
            mapName = "Text Import";
        return mapName;
    }
}
