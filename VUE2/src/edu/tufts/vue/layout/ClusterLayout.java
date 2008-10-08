/*
 * ClusterLayout.java
 *
 * Created on October 1, 2008, 2:14 PM
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
 */
package edu.tufts.vue.layout;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.dataset.*;

public class ClusterLayout  extends Layout {
    public static String DEFAULT_METADATA_LABEL = "default";
    public final int clusterColumn = 3;
    public final int total = 15;
    /** Creates a new instance of ClusterLayout */
    public ClusterLayout() {
    }
    public LWMap createMap(Dataset ds,String mapName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
        Map<String,Integer> repeatMap = new HashMap<String,Integer>();
        ArrayList<String> clusterColumnList = new ArrayList<String>();
        LWMap map = new LWMap(mapName);
        int count = 0;
        // set map size of the map
        double rowCount = ds.getRowList().size();
        double goodSize =  (int)Math.sqrt(rowCount)*100;
        MAP_SIZE = MAP_SIZE>goodSize?MAP_SIZE:goodSize;
        
        
        for(ArrayList<String> row: ds.getRowList()) {
            String node1Label = row.get(0);
            LWNode node1;
            node1 = new LWNode(node1Label);
            for(int i=1;i<row.size();i++ ) {
                String value = row.get(i);
                String key = ((ds.getHeading()==null) || ds.getHeading().size() <i)?DEFAULT_METADATA_LABEL:ds.getHeading().get(i);
                //                   System.out.println("i="+i+" key="+key+" value ="+value);
                
                VueMetadataElement vm = new VueMetadataElement();
                vm.setKey(key);
                vm.setValue(value);
                vm.setType(VueMetadataElement.CATEGORY);
                node1.getMetadataList().addElement(vm);
            }
            if(ds.getHeading().size()>1 && ds.getHeading().get(1).equals("resource")) {
                Resource resource = node1.getResourceFactory().get(new File(row.get(1)));
                node1.setResource(resource);
            }
            // special hack to demo the dataset laurie baise dataset
            if(ds.getHeading().size()>6 && ds.getHeading().get(6).equals("Actual")) {
                if(row.get(6).equalsIgnoreCase("A")) {
                    node1.setFillColor(Color.CYAN);
                }
            }
            if(ds.getHeading().size()>2&& ds.getHeading().get(2).equals("Role")) {
                if(row.get(2).contains("mentor")) {
                    node1.setFillColor(Color.CYAN);
                }
            }
            if(ds.getHeading().size()>9&& ds.getHeading().get(9).contains("Total")) {
                double width = Double.parseDouble(row.get(9));
                width = Math.sqrt(width);
                if(width > 10) width = 10;
                node1.setStrokeWidth((float)width);
                
            }
            String clusterElement = row.get(clusterColumn);
            if(!clusterColumnList.contains(clusterElement)) clusterColumnList.add(clusterElement);
            nodeMap.put(node1Label,node1);
            int COLUMNS = 8;
            int MAP_SIZE  = 5000;
            double Q_SIZE = (double)MAP_SIZE/COLUMNS;
            double x = (clusterColumnList.indexOf(clusterElement)%COLUMNS)*Q_SIZE-Q_SIZE/2;
            double y = (clusterColumnList.indexOf(clusterElement)/COLUMNS)*Q_SIZE-Q_SIZE/2;
            
            node1.layout();
            map.add(node1);
            
            double angle = Math.random()*Math.PI*4;
            
            node1.setLocation(x+Math.cos(angle)*Q_SIZE/3,y+Math.sin(angle)*Q_SIZE/3);
            
//            node1.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            
        }
        return map;
    }
    public void layout(LWSelection selection) {
        Iterator<LWComponent> i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if(c instanceof LWNode) {
                LWNode node = (LWNode)c;
                node.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            }
        }
    }
}
