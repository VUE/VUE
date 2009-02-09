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

/*
 * This layout puts nodes in a grid.  By  default it will make or try to make 
 * a square grid. Eventually it may be possible to define the number of rows, 
 * columns and cell spacing.
 */
public class TabularLayout extends Layout {

    public static final double X_COL_SIZE = 100;
    public static final double Y_COL_SIZE = 30;
    public static final double X_SPACING = 15;
    public static final double Y_SPACING = 12;
    public static final int DEFAULT_COL = 4;

    /** Creates a new instance of TabularLayout */
    public TabularLayout() {
    }

    public LWMap createMap(Dataset ds, String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }

    public void layout(LWSelection selection) throws Exception {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        int mod = DEFAULT_COL;
        double xAdd = X_COL_SIZE; // default horizontal distance between the nodes
        double  yAdd = Y_COL_SIZE; //default vertical distance between nodes
        int count = 0;
        int total = 0;
        Iterator<LWComponent> i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                minX = node.getLocation().getX() < minX ? node.getLocation().getX() : minX;
                minY = node.getLocation().getY() < minY ? node.getLocation().getY() : minY;
                xAdd = xAdd > node.getWidth() ? xAdd : node.getWidth();
                yAdd = yAdd > node.getHeight() ? yAdd : node.getHeight();
                total++;
//               System.out.println(node.getLabel()+"X= "+node.getLocation().getX()+" Y= "+node.getLocation().getY()+" MIN: "+minX+" : "+minY);
            }
        }
        xAdd += X_SPACING; // spacing between nodes
        yAdd += Y_SPACING; // vertical spacing
        double x = minX;
        double y = minY;
        mod = (int) Math.ceil(Math.sqrt((double) total));
        i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c instanceof LWNode) {
                LWNode node = (LWNode) c;
                total++;
                if (count % mod == 0) {
                    if (count != 0) {
                        y += yAdd;
                    }
                    x = minX;
                } else {
                    x += xAdd;
                }
                count++;
                node.setLocation(x, y);
            }
        }
    }
}
