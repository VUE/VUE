/*
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
package edu.tufts.vue.compare;

import tufts.vue.*;
import  java.util.*;
public class Util {
    
    public static final int LABEL = 0;
    public static final int TYPE = 1;
    public static final int BOTH = 2;
    
    // returns a map for concept map comparison
    public static final String[] CITIES = {"Boston","Miami", "Denver","London","Mumbai","Tokyo"};
    public static final String MAP_NAME= "Cities";
    public static final int MAP_SIZE = 200; // creates a map in a square of 200*200 approx
    
    private static final boolean DEBUG = false;
    private static int mergeProperty = LABEL;
    
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
    
    public static void setMergeProperty(int property)
    {
        mergeProperty = property;
    }
    
    public static String getMergeProperty(LWComponent comp)
    {
        
        String selectionMethod = VueResources.getString("merge.ontologyType.gui");
        
        if(selectionMethod.equals("OFF"))
        {    
        
          String mergeType = tufts.vue.VueResources.getString("merge.ontologyType");
        
          if(DEBUG)
          {
            System.out.println("edu.tufts.vue.compare.Util merge.ontologyType: " + mergeType);
          }
        
          if(mergeType.equals("LABEL"))
          {
            mergeProperty = LABEL;
          }
          else
          if(mergeType.equals("TYPE"))
          {
            mergeProperty = TYPE;
          }
          else
          if(mergeType.equals("BOTH"))
          {
            mergeProperty = BOTH;
          }
          else
          if(mergeType.equals("NONE"))
            return  comp.getLabel();
          else
          {
            if(comp.getMetadataList().containsOntologicalType(mergeType))
            {
                return mergeType + "-" + comp.getLabel();
            }
            else
            {
                return comp.getLabel();
            }
          }
        }

        if(mergeProperty == LABEL)
        {
           return comp.getLabel();     
        }
            
        if(mergeProperty == TYPE)
        {
           return comp.getMetadataList().getOntologyListString();     
        }
            
        if(mergeProperty == BOTH)
        {
           return comp.getLabel() + "|" + comp.getMetadataList().getOntologyListString();    
        }
        
        return comp.getLabel();
    
    }
}
