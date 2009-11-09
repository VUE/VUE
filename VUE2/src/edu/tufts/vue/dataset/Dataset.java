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
import tufts.vue.ds.DataAction;
import tufts.vue.ds.XmlDataSource;

import java.net.*;



import au.com.bytecode.opencsv.CSVReader;

import edu.tufts.vue.layout.*;

public class Dataset {
    
    public static final int MAX_SIZE = tufts.vue.VueResources.getInt("dataset.maxSize");
    public static final int MAX_LABEL_SIZE = 10000;
    String fileName;
    String label;
    ArrayList<String> heading;
    ArrayList<ArrayList<String>> rowList;
    
    String baseClass;
    XmlDataSource	datasource; 
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
    public String getFileName() {
    	return this.fileName;
    }
    public void setHeading(ArrayList<String> heading) {
        this.heading = heading;
    }
    public ArrayList<String> getHeading() {
        return heading;
    }
    
    public String getBaseClass() {
        return baseClass;
    }
    
    public ArrayList<ArrayList<String>> getRowList() {
        return rowList;
    }
    
    public LWMap createMap() throws Exception{
    	String mapName = getMapName(fileName);
        datasource = new XmlDataSource(mapName,getFileName());
        Properties props = new Properties();
 		props.put("displayName", mapName);
 		props.put("name", mapName);
 		props.put("address", getFileName());
 		datasource.setConfiguration(props);
 		VUE.getContentDock().setVisible(true);
 		VUE.getContentPanel().showDatasetsTab();
 		DataSetViewer.getDataSetList().addOrdered(datasource);
 		VUE.getContentPanel().getDSBrowser().getDataSetViewer().setActiveDataSource(datasource);			
 		DataSourceViewer.saveDataSourceViewer();	
    	LWMap map  = new LWMap(getMapName(fileName));
    	List<LWComponent> nodes =  DataAction.makeRowNodes(datasource.getSchema());
        for(LWComponent component: nodes) {
            map.add(component);
        }
        LayoutAction.random.act(new LWSelection(nodes));
        return map;
    }
    public void loadDataset() throws Exception {
    		
 		
    }
    /**
    public  void loadDataset() throws Exception {
        rowList = new ArrayList<ArrayList<String>>();
        label = fileName;
        CSVReader reader;
        if(fileName.endsWith(".csv")) {
            reader = new CSVReader(new FileReader(fileName));
        } else {
            reader = new CSVReader(new FileReader(fileName),'\t');
        }
        String line;
        int count = 0;
        // add the first line to heading  of dataset
        
        String [] words;
        while((words = reader.readNext()) != null && count < MAX_SIZE) {
            ArrayList<String> row = new ArrayList<String>();
            for(int i =0;i<words.length;i++) {
                if(words[i].length() > MAX_LABEL_SIZE) {
                    row.add(words[i].substring(0,MAX_LABEL_SIZE)+"...");
                } else {
                    row.add(words[i]);
                }
            }
            if(count==0) {
                setHeading(row);
            }else {
                rowList.add(row);
            }
            count++;
        }
        reader.close();
    }
    **/
    
    final protected String getMapName(String fileName) {
        String mapName = fileName.substring(fileName.lastIndexOf(File.separator)+1,fileName.length());
        if(mapName.lastIndexOf(".")>0)
            mapName = mapName.substring(0,mapName.lastIndexOf("."));
        if(mapName.length() == 0)
            mapName = "Text Import";
        return mapName;
    }
}
