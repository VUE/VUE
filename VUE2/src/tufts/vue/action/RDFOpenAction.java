/*
 * -----------------------------------------------------------------------------
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * RDFOpenAction.java
 *
 * Created on October 23, 2003, 12:40 PM
 */

package tufts.vue.action;

/**
 *
 * @author  akumar03
 */
import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import com.hp.hpl.jena.rdf.model.*;

public class RDFOpenAction extends VueAction {
    
    //todo: add constants for row length, starting position, y gaps and x gaps
    //also make these all be read from properties file for ease of modification
    //after compilation
    public static final int NODE_LABEL_TRUNCATE_LENGTH = 30;
    
    public RDFOpenAction(String label) {
        super(label, null, ":general/Open");
    }
    
    public RDFOpenAction() {
        this("Map from RDF File");
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
            File file = ActionUtil.openFile("Open Map", "rdf");
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

            } finally {
                VUE.clearWaitCursor();
            }
        }
    }
    
    // todo: have only one root loadMap that hanldes files & urls -- actually, make it ALL url's
       public static LWMap loadMap(String fileName) {
        try {
            
            //probably don't want to save over original rdf
            //LWMap map = new LWMap(fileName);
            //so construct a name based on file name instead:
            String mapName = fileName.substring(fileName.lastIndexOf("/")+1,fileName.length());
            if(mapName.lastIndexOf(".")>0)
                mapName = mapName.substring(0,mapName.lastIndexOf("."));
            if(mapName.length() == 0)
                mapName = "RDF Import";
            LWMap map = new LWMap(mapName);
            

            // create an empty model
            Model model = ModelFactory.createDefaultModel();
            
            // use the FileManager to find the input file
            InputStream in = new FileInputStream(fileName);
            if (in == null) {
                throw new IllegalArgumentException(
                        "File: " + fileName + " not found");
            }
            
// read the RDF/XML file
            model.read(in, "");
            
// write it to standard out
            model.write(System.out);
            
            ArrayList<com.hp.hpl.jena.rdf.model.Resource> list = new ArrayList<com.hp.hpl.jena.rdf.model.Resource>();
            ResIterator resIterator = model.listSubjects();
            while(resIterator.hasNext()) {
                com.hp.hpl.jena.rdf.model.Resource res = resIterator.nextResource();
                list.add(res);
            map.add(createNodeFromResource(res));
            }
            
            com.hp.hpl.jena.rdf.model.NodeIterator nIterator = model.listObjects();
            while(nIterator.hasNext()) {
                RDFNode rdfNode = nIterator.nextNode();
                if(rdfNode instanceof  com.hp.hpl.jena.rdf.model.Resource ) {
                    com.hp.hpl.jena.rdf.model.Resource nodeResource = (com.hp.hpl.jena.rdf.model.Resource)rdfNode;
                    if(!list.contains(nodeResource)) {
                        list.add(nodeResource);
                        map.add(createNodeFromResource(nodeResource));
                    }
                }
                    
            }
            /**
            float y = 20;
            float x = 0;
            int count = 0;
            
            LWNode node = null;
            
            int toggle = 0;
            
            // read the rdf statements and add metadata values and links
            Iterator<com.hp.hpl.jena.rdf.model.Resource> iter = list.iterator();
            while (iter.hasNext()) {
                 com.hp.hpl.jena.rdf.model.Resource  r = iter.next();   
                
                float oldWidth = 0.0f;
                if(node!=null)
                    oldWidth = node.getWidth();
                 
                String uri = r.getURI();
                String name = uri;
                int lastSlash = name.lastIndexOf("/");
                if(lastSlash > 1 && (lastSlash == name.length()-1) )
                {
                    name = name.substring(0,name.length()-1);
                }
                name = name.substring(name.lastIndexOf("/")+1,name.length());
                if(name.length() == 0)
                {
                    name = uri;
                }
                if(name.length() > NODE_LABEL_TRUNCATE_LENGTH)
                    name = name.substring(0,NODE_LABEL_TRUNCATE_LENGTH) + "...";
                node = new LWNode(name);
                System.out.println("Resource uri: "+r.getURI());
                com.hp.hpl.jena.rdf.model.StmtIterator stmtIterator = r.listProperties();
                java.util.Properties properties = new java.util.Properties();
                
                // oldWidth can be used to seperate wide nodes from each other
                // not perfectly reliable and not needed while node names are truncated
                //if(oldWidth > 150)
                //  x += oldWidth + 50;
                //else
                  x += 150;
                
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
                
                count++;
                
                node.setLocation(x,y);
                tufts.vue.MapResource resource = new MapResource(r.getURI());
                //resource.setProperties(properties);
                node.setResource(resource);
                map.addNode(node);
                while(stmtIterator.hasNext()){
                      com.hp.hpl.jena.rdf.model.Statement stmt = stmtIterator.nextStatement();
                      Object obj = stmt.getObject();
                      if(obj instanceof com.hp.hpl.jena.rdf.model.Resource) {
                          Iterator nodeIterator = map.getNodeIterator();
                          while(nodeIterator.hasNext()) {
                              LWNode nodeCon = (LWNode)nodeIterator.next();
                              if(nodeCon.getResource().getSpec().equals(((com.hp.hpl.jena.rdf.model.Resource)obj).getURI())){
                                    LWLink link = new LWLink(node,nodeCon);
                                    link.setLabel(stmt.getPredicate().getLocalName());
                                    System.out.println("Resource"+r.getURI()+"Predicate: "+stmt.getPredicate().getLocalName()+"Object:"+obj);
                                    node.setLocation(node.getLocation().getX(),node.getLocation().getY()-20);
                                    nodeCon.setLocation(nodeCon.getLocation().getX(),nodeCon.getLocation().getY()-20);
                                    map.addLink(link);
                              }
                          }
                          System.out.println("Resource: "+r.getURI()+" sub resource:"+obj.toString());
                      }else if(obj instanceof com.hp.hpl.jena.rdf.model.Literal) {
                          properties.setProperty(stmt.getPredicate().getLocalName(), obj.toString());
                          System.out.println("Resource: "+r.getURI()+" Literal:"+obj.toString()+" Predicate"+stmt.getPredicate());
                      }
                      //System.out.println("Literal="+stmt.getLiteral()+"predicate ="+stmt.getPredicate());
                }
               
            }
            **/
        
            return map;
        } catch (Exception e) {
            // out of the Open File dialog box.
            System.err.println("OpenAction.loadMap[" + fileName + "]: " + e);
            VueUtil.alert(null, "\"" + fileName + "\" cannot be opened in this version of VUE.", "Map Open Error");
            e.printStackTrace();
        }
            
        return null;
    }
    
    private static LWNode createNodeFromResource(com.hp.hpl.jena.rdf.model.Resource r) {
        tufts.vue.MapResource resource = new MapResource(r.getURI());
        LWNode node = new LWNode(r.getURI());
        node.setResource(resource);
        node.setLocation(Math.random()*400,Math.random()*400);
        return node;
    }
    
    public static void main(String args[]) throws Exception {
        String file = args.length == 0 ? "test.xml" : args[0];
        System.err.println("Attempting to read map from " + file);
        DEBUG.Enabled = true;
        VUE.parseArgs(args);
        LWMap map;
        if (file.indexOf(':') >= 0)
            map = OpenAction.loadMap(new java.net.URL(file));
        else
            map = OpenAction.loadMap(file);
        System.out.println("Loaded map: " + map);
    }
}
