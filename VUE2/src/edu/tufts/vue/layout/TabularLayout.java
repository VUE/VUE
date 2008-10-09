/*
 * TabularLayout.java
 *
 * Created on October 8, 2008, 2:32 PM
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

public class TabularLayout extends Layout {
    
    /** Creates a new instance of TabularLayout */
    public TabularLayout() {
    }
    public   LWMap createMap(Dataset ds,String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }
    
    public   void layout(LWSelection selection) throws Exception {
        double minX =10000;
        double minY = 10000;
        int count = 0;
        int total = 0;
        int mod = 4;
        int xAdd = 100;
        int yAdd = 50;
        Iterator<LWComponent> i = selection.iterator();
        
        while (i.hasNext()) {
            LWComponent c = i.next();
            if(c instanceof LWNode) {
                LWNode node = (LWNode)c;
                minX = node.getLocation().getX()<minX?node.getLocation().getX():minX;
                minY =node.getLocation().getY()<minY?node.getLocation().getY():minY;
                total++;
//               System.out.println(node.getLabel()+"X= "+node.getLocation().getX()+" Y= "+node.getLocation().getY()+" MIN: "+minX+" : "+minY);
            }
        }
        double x = minX;
        double y = minY;
        mod = (int) Math.sqrt((double)total);
        i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if(c instanceof LWNode) {
                LWNode node = (LWNode)c;
                total++;
                if(count%mod ==0) {
                    if(count!=0) y += yAdd;
                    x =minX;
                } else {
                    x+= xAdd;
                }
                count++;
                node.setLocation(x,y);
            }
        }
    }
    
}
