/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 * Import.java
 *
 * Created on Feb 20, 2009
 *
 * Copyright 2003-2009 Tufts University  Licensed under the
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

    public static final String[] LAYOUT_CLASS_NAME = {"ListRandom","ListRandom","Circular","FilledCircular","Tabular"};

    public static final String[] LAYOUT_SHORTCUT = {"default","random","cirlce","filledCircle","table"};

    Timer           timer = null;
    XmlDataSource   datasource;
    ActionListener  createMapListener = null;
    LWMap           map;
    /**
     * Create a map of specified layout with specific layout id
     * @param inputFile comma or tab delimited import file
     * @param outputFile  a map generated from input file
     * @param layoutId  0- default(random), 1 - random, 2 - circle, 3 -filled circle, 4 - table
     * @throws java.lang.Exception
     */
    public void createMap(String inputFile,String outputFile,int layoutId) throws Exception {
        Layout layout = (Layout) Class.forName(LAYOUT_CLASS_NAME[layoutId]+"Layout.class").newInstance();
        createMap(inputFile,outputFile,layout);
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
         
        String mapName = "test";
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
    public static void main(String[] args) throws Exception {
        String inputFile = args[0];
        String outputFile = args[1];
        Import importer = new Import();
        importer.createMap(inputFile,outputFile);
       }
}
