/*
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

/*
 * TextOpenAction.java
 * 
 * Very rough initial data import demos and first Zotero Prototype
 * data import
 *
 * Created on October 23, 2003, 12:40 PM
 */

package tufts.vue.action;

/**
 *
 * @author  akumar03
 * @author  dhelle01
 */
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
 
public class TextOpenAction  extends VueAction {
    
    private static boolean DEBUG_LOCAL = false;
    
    //todo: add constants for row length, starting position, y gaps and x gaps
    //also make these all be read from VueResources.properties file for ease of modification
    //after compilation
    public static final int NODE_LABEL_TRUNCATE_LENGTH = 8;
    public static final int CIRCLE_LAYOUT = 0;
    public static final int RANDOM_LAYOUT = 1;
    public static final int GRAVITY_LAYOUT = 2;
    public static final int STAGGERED_LAYOUT = 3;
    public static int layout = STAGGERED_LAYOUT;
    public static final int MAP_SIZE = 500;
    public static final int MAX_SIZE =5000;
    
    public static boolean ZOTERO_PROTOTYPE = false;
    
    public TextOpenAction(String label) {
        super(label, null, ":general/Open");
    }
    
    public TextOpenAction() {
        this("Import Text file...");
    }
    
    public static boolean isZoteroProtypeEnabled()
    {
    	return ZOTERO_PROTOTYPE;
    }
    
    public static void setZoteroPrototypeEnabled(boolean enabled)
    {
    	ZOTERO_PROTOTYPE = enabled;
    }
    
    // workaround for rapid-succession Ctrl-O's which pop multiple open dialogs
    private static final Object LOCK = new Object();
    private static boolean openUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (openUnderway)
                return;
            openUnderway = true;
        }
        try {
            File file = ActionUtil.openFile("Open Map", "text");
            displayMap(file);
            System.out.println("Action["+e.getActionCommand()+"] completed.");
        } finally {
            openUnderway = false;
        }
    }
    
    
    public static void displayMap(File file) {
        if (file != null) {
            VUE.activateWaitCursor();
            try {
                LWMap loadedMap = loadMap(file.getAbsolutePath());
                VUE.displayMap(loadedMap);
            }catch(Throwable t) {
                t.printStackTrace();
            } finally {
                VUE.clearWaitCursor();
            }
        }
    }
    
    // todo: have only one root loadMap that hanldes files & urls -- actually, make it ALL url's
    public static LWMap loadMap(String fileName) throws Exception{
        Map<String,LWNode> nodeMap = new HashMap<String,LWNode>();
         Map<String,Integer> repeatMap = new HashMap<String,Integer>();
        Map<String,LWLink> linkMap = new HashMap<String, LWLink>();
        String mapName = fileName.substring(fileName.lastIndexOf("/")+1,fileName.length());
        if(mapName.lastIndexOf(".")>0)
            mapName = mapName.substring(0,mapName.lastIndexOf("."));
        if(mapName.length() == 0)
            mapName = "Text Import";
        LWMap map = new LWMap(mapName);
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        String links = null; // for Zotero prototype
        
        if(!ZOTERO_PROTOTYPE) // in zotero import each row is a node
        {    
          reader.readLine(); // skip the first line
        }
        int count = 0;
        
        // for staggered layout
        float y = 20;
        float x = 0;
        int toggle = 0;
        
        List<String[]> linksList = new ArrayList<String[]>(); // also for Zotero Prototype
        
        while((line=reader.readLine()) != null && count <MAX_SIZE) {
           
            if(DEBUG_LOCAL)
            {    
              System.out.println(line+" words: "+line.split(",").length);
            }
            
            if(ZOTERO_PROTOTYPE)
            {
              String[] parts = line.split("\"");
              //todo: sanity check on number of parts
              line = parts[0];
              
              if(parts.length > 1)
                links = parts[1];
              else
                links = null;
              
              if(DEBUG_LOCAL)
              {    
                if(links!=null)
                {    
                  System.out.println("links -- " + links);
                }
              }
            }    
            
            String[] words = line.split(",");
            LWNode node1;
            LWNode node2 = null;
            
            if(ZOTERO_PROTOTYPE)
            {   
                node1 = new LWNode(words[1]);
                try
                { 
                  // todo: can throw its own stack trace
                  // so, avoid for non standard (rdf, zotero notes etc.) uri;
                  Resource resource = map.getResourceFactory().get(words[2]);
                  node1.setResource(resource);
                }
                catch(Exception e)
                {
                  System.out.println("Exception setting resource: " + e);
                }
                
                String id = words[0];
                
                if(links != null)
                {
                  String[] linksTo = links.split(",");
                
                  String[] arr = new String[linksTo.length+1];
                
                  arr[0] = id;
                
                  System.arraycopy(linksTo,0,arr,1,linksTo.length);
              
                  linksList.add(arr);
                }
                
                // this makes the id editable.. 
                // have to be careful about maintaining id for updates
                // todo: put in content info instead.
                //edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                ////vme.setType(edu.tufts.vue.metadata.VueMetadataElement.CATEGORY);
                //String[] obj = {"http://vue.tufts.edu/custom.rdfs#z_id",id};   
                //vme.setObject(obj);
                //node1.getMetadataList().getMetadata().add(vme);
                //vme.setType(edu.tufts.vue.metadata.VueMetadataElement.CATEGORY);
                
                // put in content info instead of keywords:
                node1.getResource().setProperty("Zotero.id",id);
                
                // just for debug purposes -- many more references than I expected
                // and they are not bidirectional.. this seems to not just be
                // the "related" relation from zotero, which is probably what we want?
                //edu.tufts.vue.metadata.VueMetadataElement vme2 = new edu.tufts.vue.metadata.VueMetadataElement();
                //vme2.setType(edu.tufts.vue.metadata.VueMetadataElement.CATEGORY);
                //String[] obj2 = {"http://vue.tufts.edu/custom.rdfs#z_links",links};   
                //vme2.setObject(obj2);
                //if(links!=null)
                //node1.getMetadataList().getMetadata().add(vme2);
                
                nodeMap.put(words[0],node1);
                
                if(DEBUG_LOCAL)
                {
                    System.out.println("Node map addition " + words[0] + "," + node1);
                }
                //repeatMap.put(words[0], new Integer(1));
                map.add(node1);
                node1.layout();
            }
            else
            if(words.length == 4) {
                if(!nodeMap.containsKey(words[0])) {
                    node1 = new LWNode(words[0]);
                      Resource resource = map.getResourceFactory().get(words[1]);
                    node1.setResource(resource);
                    nodeMap.put(words[0],node1);
                     repeatMap.put(words[0], new Integer(1));
                    map.add(node1);
                } else {
                    node1 = nodeMap.get(words[0]);
                    int nc= repeatMap.get(words[0]).intValue();
                    repeatMap.put(words[0],new Integer(nc+1));
                }
                if(!nodeMap.containsKey(words[2])) {
                    node2 = new LWNode(words[2]);
                     Resource resource =   map.getResourceFactory().get( words[3] );
                    node2.setResource(resource);
                    map.add(node2);
                    nodeMap.put(words[2],node2);
                    repeatMap.put(words[2], new Integer(1));
                    
                } else {
                    node2 = nodeMap.get(words[2]);
                      int nc= repeatMap.get(words[2]).intValue();
                    repeatMap.put(words[2],new Integer(nc+1));
                }
                
            }else {
                
                try
                {
                
                if(!nodeMap.containsKey(words[0])) {
                    node1 = new LWNode(words[0]);
                    nodeMap.put(words[0],node1);
                     repeatMap.put(words[0], new Integer(1));
                    map.add(node1);
                } else {
                    node1 = nodeMap.get(words[0]);
                      int nc= repeatMap.get(words[0]).intValue();
                    repeatMap.put(words[0],new Integer(nc+1));
                }
                if(!nodeMap.containsKey(words[1])) {
                    node2 = new LWNode(words[1]);
                    map.add(node2);
                    nodeMap.put(words[1],node2);
                     repeatMap.put(words[1], new Integer(1));
                } else {
                    node2 = nodeMap.get(words[1]);
                      int nc= repeatMap.get(words[1]).intValue();
                    repeatMap.put(words[1],new Integer(nc+1));
                }
                
                }
                catch(Exception e)
                {
                    System.out.println("Exception in text import: " + e);
                    node1 = new LWNode("exception");
                    node2 = new LWNode("exception 2");
                }
                
            }
            
            
            if(ZOTERO_PROTOTYPE)
            {
                // actually just avoiding else clause code
                // zotero links are calculated after loop
                // using linkslist and nodemap
            }
            else
            {
                
                
              System.out.println("COUNT: "+count+" Node1 :"+ node1.getLabel()+" Node2 : "+
                      (node2.getLabel()));
            
              String linkKey = node1.getLabel()+node2.getLabel();
              if(!linkMap.containsKey(linkKey)) {
                
                  LWLink link = new LWLink(node1,node2);
                  linkMap.put(linkKey,link);
                
                  map.add(link);
              }
            }
            
            if(layout == STAGGERED_LAYOUT)
            {
                //x += 150;
                x += 300;
                
                if(toggle == 0)
                {
                    toggle++;
                    y = y + 50;
                }
                else
                if(toggle == 1)
                {
                    toggle = 0;
                    y = y - 50;
                }
                if(count % 5 == 0)
                {
                    y += 100;
                    x = 400;
                    toggle = 0;
                }
                
                node1.setLocation(x,y);
                if(node2 != null)
                  node2.setLocation(x+150,y);
            }
            if(layout == CIRCLE_LAYOUT ) {
                double angle = Math.random()*Math.PI*4;
                node1.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
                angle = Math.random()*Math.PI*4;
                node2.setLocation(MAP_SIZE*(1+Math.cos(angle)),MAP_SIZE*(1+Math.sin(angle)));
            } else if (layout == RANDOM_LAYOUT) {
                 node1.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
               node2.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());
            } else if(layout == GRAVITY_LAYOUT) {
                  double angle = Math.random()*Math.PI*4;
                  int nc1= repeatMap.get(node1.getLabel()).intValue();
                 // nc1 = nc1*nc1;
                  double fact1 = Math.sqrt(nc1);
                node1.setLocation(MAP_SIZE*(1+Math.cos(angle)/fact1),MAP_SIZE*(1+Math.sin(angle)/fact1));
                angle = Math.random()*Math.PI*4;
                int nc2= repeatMap.get(node1.getLabel()).intValue();
                //nc2=nc2*nc2;
                double fact2 = Math.sqrt(nc2);
                node2.setLocation(MAP_SIZE*(1+Math.cos(angle)/fact2),MAP_SIZE*(1+Math.sin(angle)/fact2));
            }
            
            count++;
        }
        
        if(ZOTERO_PROTOTYPE)
        {
            Iterator<String[]> nodes = linksList.iterator();
            
            while(nodes.hasNext())
            {
                String[] arr = nodes.next();
                LWNode node = nodeMap.get(arr[0]);
                
                if(DEBUG_LOCAL)
                {
                    System.out.println("TextOpenAction: linkslist arr length " + arr.length);
                }
                
                for(int i=1;i<arr.length;i++)
                {
                  String linkKey = arr[0] + "|" +  arr[i];
                  
                  if(DEBUG_LOCAL)
                  {
                      System.out.println("TextOpenAction: linkKey " + linkKey);
                  }
                  
                  if(!linkMap.containsKey(linkKey)) 
                  {
                      
                    if(DEBUG_LOCAL)
                    {
                      System.out.println("TextOpenAction: accepted -- " + linkKey);  
                    }
                      
                    LWNode node2 = nodeMap.get(arr[i]);
                    LWLink link = new LWLink(node,node2);
                    linkMap.put(linkKey,link);
                
                    map.add(link);
                  }
                }
            }
        }
        
        return map;
    }
     
    
    
    public static void main(String args[]) throws Exception {
        String file = args.length == 0 ? "C:\\anoop\\vue\\maps\\Short.rdf" : args[0];
        System.err.println("Attempting to read map from " + file);
        DEBUG.Enabled = true;
        VUE.parseArgs(args);
        LWMap map;
        
        map = loadMap(file);
        System.out.println("Loaded map: " + map);
    }
}
