/*
 * ListRandomLayout.java
 *
 * Created on August 6, 2008, 1:37 PM
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

/* ListRandomLaout - This layout scatters all the nodes at random in a square
 * by default the area of the square is propotional to the number of nodes in 
 * selection. 
 */
public class ListRandomLayout extends Layout {

    public static String DEFAULT_METADATA_LABEL = "default";
    public static final double FACTOR = 1.5;
    public static final int MAX_COLLISION_CHECK = 20; // check number of times to check for collisions

    /** Creates a new instance of ListRandomLayout */
    public ListRandomLayout() {
    }

    public LWMap createMap(Dataset ds, String mapName) throws Exception {
        Map<String, LWNode> nodeMap = new HashMap<String, LWNode>();
        Map<String, Integer> repeatMap = new HashMap<String, Integer>();
        LWMap map = new LWMap(mapName);
        int count = 0;
        // set map size of the map
        double rowCount = ds.getRowList().size();
        double goodSize = (int) Math.sqrt(rowCount) * 100;
        MAP_SIZE = MAP_SIZE > goodSize ? MAP_SIZE : goodSize;


        for (ArrayList<String> row : ds.getRowList()) {
            String node1Label = row.get(0);
            LWNode node1;
            if (!nodeMap.containsKey(node1Label)) {
                node1 = new LWNode(node1Label);
                for (int i = 1; i < row.size(); i++) {
                    String value = row.get(i);
                    String key = ((ds.getHeading() == null) || ds.getHeading().size() < i) ? DEFAULT_METADATA_LABEL : ds.getHeading().get(i);
                    //                   System.out.println("i="+i+" key="+key+" value ="+value);

                    VueMetadataElement vm = new VueMetadataElement();
                    vm.setKey(key);
                    vm.setValue(value);
                    vm.setType(VueMetadataElement.CATEGORY);
                    node1.getMetadataList().addElement(vm);
                }
                if (ds.getHeading().size() > 1 && ds.getHeading().get(1).equals("resource")) {
                    Resource resource = node1.getResourceFactory().get(new File(row.get(1)));
                    node1.setResource(resource);
                }
                // special hack to demo the dataset laurie baise dataset
                if (ds.getHeading().size() > 6 && ds.getHeading().get(6).equals("Actual")) {
                    if (row.get(6).equalsIgnoreCase("A")) {
                        node1.setFillColor(Color.CYAN);
                    }
                }
                nodeMap.put(node1Label, node1);
                node1.layout();
                map.add(node1);
            } else {
                node1 = nodeMap.get(node1Label);
            }

            node1.setLocation(MAP_SIZE * Math.random(), MAP_SIZE * Math.random());

        }
        return map;
    }

    /** It takes a map and scatters the nodes randomly
     * @param map the map for layout
     */
    public void layout(LWMap map) {
        Iterator<LWNode> nodeIterator = map.getChildIterator();
        while (nodeIterator.hasNext()) {
            LWNode node = nodeIterator.next();
            node.setLocation(MAP_SIZE * Math.random(), MAP_SIZE * Math.random());
        }
    }

    public void layout(java.util.List<LWNode> nodeList) {
        for (LWNode node : nodeList) {
            node.setLocation(MAP_SIZE * Math.random(), MAP_SIZE * Math.random());
        }
    }

    public void layout(LWSelection selection) {
        // determine the left corner of the selection and and use that as the 
        // center for layout.  We assume the selection is a rectangle. 
        // also compute the total number of nodes in the selection
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double xAdd = X_COL_SIZE; // default horizontal distance between the nodes
        double yAdd = Y_COL_SIZE; //default vertica
        int total = 0;
        Iterator<LWComponent> i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                minX = node.getLocation().getX() < minX ? node.getLocation().getX() : minX;
                minY = node.getLocation().getY() < minY ? node.getLocation().getY() : minY;
                maxX = node.getLocation().getX() > maxX ? node.getLocation().getX() : maxX;
                maxY = node.getLocation().getY() > maxY ? node.getLocation().getY() : maxY;
                xAdd = xAdd > node.getWidth() ? xAdd : node.getWidth();
                yAdd = yAdd > node.getHeight() ? yAdd : node.getHeight();
                total++;
            }
        }

        double xSize = Math.sqrt(total) * (xAdd + X_SPACING) * FACTOR;
        double ySize = Math.sqrt(total) * (yAdd + Y_SPACING) * FACTOR;
        i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                double centerX = (minX + maxX) / 2;
                double centerY = (minY + maxY) / 2;
                double x = centerX + xSize * (Math.random() - 0.5);
                double y = centerY + ySize * (Math.random() - 0.5);
                int col_count = 0;
                boolean flag = true;
                while (flag && col_count < MAX_COLLISION_CHECK) {

                    LWComponent overlapComponent;
                    if ((VUE.getActiveViewer().pickNode((float) x, (float) y) != null) || (VUE.getActiveViewer().pickNode((float) x + node.getWidth(), (float) y + node.getHeight()) != null) || (VUE.getActiveViewer().pickNode((float) x, (float) y + node.getHeight()) != null) || (VUE.getActiveViewer().pickNode((float) x + node.getWidth(), (float) y) != null)) {
                        x = centerX + xSize * (Math.random() - 0.5);
                        y = centerY + ySize * (Math.random() - 0.5);
                    } else {
                        flag = false;
                    }
                    col_count++;
//                    System.out.println("Node: "+node.getLabel()+" count:"+col_count);

                }
                node.setLocation(x, y);
            }
        }
    }
}
