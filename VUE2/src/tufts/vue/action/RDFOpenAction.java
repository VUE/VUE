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
    public RDFOpenAction(String label) {
        super(label, null, ":general/Open");
    }
    
    public RDFOpenAction() {
        this("Open RDF File");
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
            LWMap map = new LWMap(fileName);
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
            ResIterator iter = model.listSubjects();
            float y = 20;
            float x = 400;
            // read the rdf statements and add metadata values and links
            while (iter.hasNext()) {
                 com.hp.hpl.jena.rdf.model.Resource  r = iter.nextResource();                 
                LWNode node = new LWNode(r.getURI());
                com.hp.hpl.jena.rdf.model.StmtIterator stmtIterator = r.listProperties();
                java.util.Properties properties = new java.util.Properties();
                x += 80;
                node.setLocation(x,y);
                tufts.vue.MapResource resource = new MapResource(r.getURI());
                resource.setProperties(properties);
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
        
            return map;
        } catch (Exception e) {
            // out of the Open File dialog box.
            System.err.println("OpenAction.loadMap[" + fileName + "]: " + e);
            VueUtil.alert(null, "\"" + fileName + "\" cannot be opened in this version of VUE.", "Map Open Error");
            e.printStackTrace();
        }
        return null;
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
