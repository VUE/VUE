/*
 * RippleLayout.java
 *
 * Created on November 18, 2008, 1:53 PM
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
import java.awt.geom.Point2D;
/*
 * This layout creates a ripple around the nodes that have been selected.
 * All nodes are pushed a distance of d^1/4, where d is the distance from a selection
 * node
 */


public class RippleLayout extends Layout {
    public static final double POWER = 0.25;
    /** Creates a new instance of RippleLayout */
    public RippleLayout() {
    }
    public void layout(LWSelection selection) {
        double MAX_SHIFT = 50; //maximum shift of a node
        double SHIFT_RANGE = 300; // distance from the selected nodes that experience ripple
        for(LWComponent component:selection) {
            double centerX = component.getLocation().getX();
            double centerY = component.getLocation().getY();
            Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(LWContainer.ChildKind.PROPER).iterator();
            while(i.hasNext()) {
            	 
                LWComponent c = i.next();
                if (c.isManagedLocation())
                    continue; 
                if(!selection.contains(c) && c instanceof LWNode) {
                    double x = c.getLocation().getX();
                    double y = c.getLocation().getY();
                    double angle = Math.atan2(centerY-y,x-centerX);
                    double dist =  Point2D.distance(centerX,centerY,x,y);
                    if(dist<SHIFT_RANGE) {
                        double newDist = dist+(SHIFT_RANGE-dist)*MAX_SHIFT/SHIFT_RANGE;
                        double newX = centerX+newDist*Math.cos(angle);
                        double newY = centerY-newDist*Math.sin(angle);
                        c.setLocation(newX,newY);
                    }
                }
            }
        }
    }
    
    public   LWMap createMap(Dataset ds,String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }
    
}
