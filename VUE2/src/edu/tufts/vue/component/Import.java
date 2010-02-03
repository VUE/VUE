/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 * Import.java
 *
 * Created on Feb 20, 2009
 *
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
package edu.tufts.vue.component;

import tufts.vue.ds.*;
import edu.tufts.vue.dataset.Dataset;
import edu.tufts.vue.layout.*;
import tufts.vue.action.SaveAction;
import tufts.vue.action.ActionUtil;
import tufts.vue.*;

import java.io.File;
import java.util.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;


public class Import {

    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_RANDOM = 1;
    public static final int LAYOUT_CIRCLE =2;
    public static final int LAYOUT_FILLED_CIRCLE = 3;
    public static final int LAYOUT_TABLE = 4;
    
    public static final String  LAYOUT_PACKAGE = "edu.tufts.vue.layout.";
    public static final String LAYOUT_EXTENSION = "Layout";
    public static final String[] LAYOUT_CLASS_NAME = {"ListRandom","ListRandom","Circular","FilledCircular","Tabular"};

    public static final String[] LAYOUT_SHORTCUT = {"default","random","circle","filled","table"};

    XmlDataSource   datasource;
     /**
     * Create a map of specified layout with specific layout id
     * @param inputFile comma or tab delimited import file
     * @param outputFile  a map generated from input file
     * @param layoutId  0- default(random), 1 - random, 2 - circle, 3 -filled circle, 4 - table
     * @throws java.lang.Exception
     */
    public void createMap(String inputFile,String outputFile,int layoutId) throws Exception {
    	String className = LAYOUT_PACKAGE+LAYOUT_CLASS_NAME[layoutId]+LAYOUT_EXTENSION;
    	ClassLoader classLoader =  ClassLoader.getSystemClassLoader();
    	Class layoutClass = classLoader.loadClass(className);
        Layout layout = (Layout) layoutClass.newInstance();
        createMap(inputFile,outputFile,layout);
    }
    /**
     * Create a map of specified layout with specific layout id
     * @param inputFile comma or tab delimited import file
     * @param outputFile  a map generated from input file
     * @param layout    random, circle, filled, table
     * @throws java.lang.Exception
     */
    public void createMap(String inputFile,String outputFile,String layout) throws Exception {
    	HashMap<String,Integer> optionsMap = new HashMap<String,Integer>();
		optionsMap.put("random",1);
		optionsMap.put("circle",2);
		optionsMap.put("filled",3);
		optionsMap.put("table",4);
		int layoutId = optionsMap.get(layout);
    	createMap(inputFile,outputFile,layoutId);
    }
    /**
     * Create a map of specified layout
     *
     * @param inputFile   comma or tab delimited import file
     * @param outputFile  a map generated from input file
     * @param layout layout object type of layout
     * @throws java.lang.Exception
     */

    public void createMap(String inputFile,String outputFile, Layout layout) throws Exception {
    	 Schema schema = Schema.getInstance(Resource.instance(inputFile),edu.tufts.vue.util.GUID.generate());
         
        String mapName = Dataset.getMapName(outputFile);
        datasource = new XmlDataSource(mapName,inputFile);
        Properties props = new Properties();
 		props.put("displayName", mapName);
 		props.put("name", mapName);
 		props.put("address", inputFile);
 		datasource.setConfiguration(props);
 	    schema = datasource.ingestCSV(schema,inputFile,true);
 		LWMap map = new LWMap("test");
// 		schema.setRowNodeStyle(DataAction.makeStyleNode(schema));
 		List<LWComponent> nodes =  DataAction.makeRowNodes(schema);
		for(LWComponent component: nodes) {	
			map.add(component);
		}
		layout.layout(new LWSelection(nodes));
        ActionUtil.marshallMap(new File(outputFile), map);
    }

    /** A method that creates a map from input file. The input file needs to be
     * comma or tab delimited similar to the ones used in VUE XML datasource. The
     * default layout is random layout but can be set by passing params. The default
     * is to make list random layout.
     *
     * @param inputFile comma or tab delimited import file
     * @param outputFile a map generated from input file
     * @throws java.lang.Exception
     */

    public void createMap(String inputFile, String outputFile) throws Exception {
    	
        Layout layout = new ListRandomLayout();
        createMap(inputFile,outputFile,layout);
    }
    
	public void printHelp() {
		System.out.println("Usage: java -jar VUEInport.jar <input file> <output file(vue map)> [option]");
		System.out.println();
		System.out.println("The arguments  are:");
		System.out.println("-h or --help  : prints this informaion");
		System.out.println("<input file>  :  this is a data file in csv or tab delimited format" );
		System.out.println("<output file> : location to output vue file" );
		System.out.println("[option]      : a number or format specifying the layout");
		System.out.println("   			     0, 1 random or no option - random layout" );
		System.out.println("   			     2, circle - circular layout" );
		System.out.println("   			     3, filled - filled circular layout" );
		System.out.println("   			     4, table - tabular layout" );	
	}
     
    
    public static void main(String[] args) throws Exception {
    	Import importer = new Import();
        
    	if(args.length < 2) {
			importer.printHelp();
			System.exit(0);
		}
		if(args[0] != null ) {
			if(args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help")) {
				importer.printHelp();
				System.exit(0);
			}
		}
        String inputFile = args[0];
        String outputFile = args[1];
        if(args.length == 3 && args[2] != null) {
        	try {
        	int  layoutId= Integer.parseInt(args[2]);
        	 if(layoutId > 4 || layoutId < 0 ) {
        		 layoutId = 0;
        	 }
        	
        	importer.createMap(inputFile,outputFile,layoutId);
        	} catch(Exception ex) {
        		importer.createMap(inputFile, outputFile,args[2]);
        	}
        } else {
        	importer.createMap(inputFile,outputFile);
        }
       }
}
