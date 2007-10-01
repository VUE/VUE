/*
 * Util.java
 *
 * Created on October 5, 2006, 12:17 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.compare;

import tufts.vue.*;
import  java.util.*;
public class Util {
    // returns a map for concept map comparison
    public static final String[] CITIES = {"Boston","Miami", "Denver","London","Mumbai","Tokyo"};
    public static final String MAP_NAME= "Cities";
    public static final int MAP_SIZE = 200; // creates a map in a square of 200*200 approx
    public static LWMap getMap() {
        LWMap map = new LWMap("Cities");
        LWNode n1 = new LWNode("Boston");
        LWNode n2 = new LWNode("Miami");
        LWNode n3 = new LWNode("Denver");
        n1.setLocation(100, 30);
        n2.setLocation(100, 100);
        n3.setLocation(50, 180);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        LWLink k1 = new LWLink(n1, n2);
        LWLink k2 = new LWLink(n2, n3);
        LWLink k3 = new LWLink(n3, n1);
        map.addLink(k1);
        map.addLink(k2);
        //map.addLink(k3);
        return map;
    }
    // TODO: a method that generated a random map from five nodes with approximately nLinks number  of links
    public static LWMap getRandomMap(int nNodes, int nLinks) {
        int maxNodes = CITIES.length;
        if(nNodes > maxNodes) {
            nNodes = maxNodes;
        }
        double maxLinks = Math.floor((float)nNodes*(nNodes-1)/2);
        if(nLinks > maxLinks) {
            nLinks = (int)maxLinks;
        }  
        double angle = 2*Math.PI/nNodes;
        
        return null;
    }
    
    public static String getMergeProperty(LWComponent comp) {
        //if(comp instanceof LWImage)
        //{
        //  System.out.println("Util - getMergeProperty for LWImage: " + ((LWImage)comp).getResource().toString());  
        //  return ((LWImage)comp).getResource().toString();   
        //}
        //else
        //{
        //  System.out.println("Util - getMergeProperty for non LWImage: " + comp.getLabel());   
        String mergeType = tufts.vue.VueResources.getString("merge.ontologyType");
        
        if(mergeType.equals("NONE"))
          return  comp.getLabel();
        else
        {
          if(comp.getMetadataList().containsOntologicalType(mergeType))
          {
              return mergeType;
          }
          else
          {
              return comp.getLabel();
          }
        }
        //}
    }
}
