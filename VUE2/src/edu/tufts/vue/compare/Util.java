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

    public enum MP { LABEL, TYPE, BOTH, RESOURCE };
    
    // returns a map for concept map comparison
    public static final String[] CITIES = {"Boston","Miami", "Denver","London","Mumbai","Tokyo"};
    public static final String MAP_NAME= "Cities";
    public static final int MAP_SIZE = 200; // creates a map in a square of 200*200 approx
    
    private static final boolean DEBUG = false;
    private static MP MergeProperty = MP.LABEL;
    
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
    
    public static void setMergeProperty(MP property) {
        MergeProperty = property;
    }

    // May want an isValidTarget here that does the check for LWImage/LWNode, and, at minimum, if
    // the current merge-key is RESOURCE, filter out Flag.ICON as well.  Right now the impl filters
    // all of those out, tho technically they could have different labels, in which case we could
    // merge on them, tho that may be too much of a stretch.  (They could have meta-data as well,
    // etc).  Really, tho, we should treat them as if they're not there and entirely owned by the
    // node, as their existence is only the result of a hack.

    public static MP getMergeProperty() { return MergeProperty; }
    
    public static Object getMergeProperty(final LWComponent c)
    {
        // if (!c.hasMetaData(VueMetadataElement.ONTO_TYPE)) // don't bother with onto crap

        //System.out.println("getMergeKey " + MergeProperty + " " + c);
        
        switch (MergeProperty) {
        case BOTH: return c.getLabel() + "|" + c.getMetadataList().getOntologyListString();
        case TYPE: return c.getMetadataList().getOntologyListString(); // concats, SLOWLY, any Onto VME's?
        case RESOURCE: return c.hasResource() ? c.getResource().getSpec() : null;
        case LABEL:
        default: return c.getLabel();
        }
    }
                 
    // private static final String StaticMergeType = VueResources.getString("merge.ontologyType");
    // private static final boolean RuntimePropertyEnabled = !VueResources.getString("merge.ontologyType.gui").equals("OFF");
    // public static String getMergeProperty(LWComponent comp)
    // {
    //     if (RuntimePropertyEnabled) {
    //            
    //              if (mergeProperty == LABEL) return comp.getLabel();     
    //         else if (mergeProperty == TYPE)  return comp.getMetadataList().getOntologyListString(); // concats, SLOWLY, any Onto VME's?
    //         else if (mergeProperty == BOTH)  return comp.getLabel() + "|" + comp.getMetadataList().getOntologyListString();
    //                 
    //     } else {
    //         // SMF: Inspecting 2012-05-09 16:29.43 Wednesday SFAir.local:
    //         /** VueResources.properties: merge.ontologyType.gui=ON */
    //         // Don't know if this value was ever tested "OFF" or if this code below
    //         // is still working.  It's strange -- was this stuff ever driven
    //         // entirely statically via resources?
    //
    //         if (DEBUG) { System.out.println("edu.tufts.vue.compare.Util merge.ontologyType: " + StaticMergeType); }
    //            
    //              if (StaticMergeType.equals("LABEL")) { mergeProperty = LABEL; }
    //         else if (StaticMergeType.equals("TYPE"))  { mergeProperty = TYPE; }
    //         else if (StaticMergeType.equals("BOTH"))  { mergeProperty = BOTH; }
    //         else if (StaticMergeType.equals("NONE"))  { return comp.getLabel(); }
    //         else {
    //             if (comp.getMetadataList().containsOntologicalType(mergeType)) {
    //                 return mergeType + "-" + comp.getLabel();
    //             } else {
    //                 return comp.getLabel();
    //             }
    //         }
    //     }
    //     // Always default to label
    //     return comp.getLabel();
    // }
}
