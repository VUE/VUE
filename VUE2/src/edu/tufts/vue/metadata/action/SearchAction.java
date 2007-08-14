
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.metadata.action;

import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import tufts.vue.*;

/*
 * SearchAction.java
 *
 * Created on July 27, 2007, 2:21 PM
 *
 * @author dhelle01
 */
public class SearchAction extends AbstractAction {
    
    private List<List<URI>> finds = null;
    
    private List<String> tags;
    private List<LWComponent> comps;
    
    private JTextField searchInput;
    private edu.tufts.vue.rdf.RDFIndex index;
    //public final String name = "Search";
    
    public SearchAction(JTextField searchInput) {
        super("Search");
        this.searchInput = searchInput;
        Thread t = new Thread() {
            public void run() {
                index = new  edu.tufts.vue.rdf.RDFIndex();
                index.index(VUE.getActiveMap());
            }
        };
        t.start();
        
        
    }
    
    public void loadKeywords(String searchString) {
        
        tags = new ArrayList<String>();
        String[] parsedSpaces = searchString.split(" ");
        for(int i=0;i<parsedSpaces.length;i++) {
            tags.add(parsedSpaces[i]);
        }
        
    }
    
    public void performSearch() {
        // edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().index(VUE.getActiveMap());
        long t0 = System.currentTimeMillis();
        synchronized(index) { 
        if(DEBUG.RDF)System.out.println("Time at the beginning: "+(System.currentTimeMillis()-t0));
        index.index(VUE.getActiveMap());
        if(DEBUG.RDF)System.out.println("Performed Index:"+(System.currentTimeMillis()-t0));
        }
        finds = new ArrayList<List<URI>>();
        
        List<URI> found = null;
        
        for(int i=0;i<tags.size();i++) {
            //found = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().search(tags.get(i));
            if(DEBUG.RDF)System.out.println("Beginning search "+i+" at: "+(System.currentTimeMillis()-t0));
            found = index.search(tags.get(i));
            if(DEBUG.RDF)System.out.println("Ending search "+i+" at: "+(System.currentTimeMillis()-t0));
            finds.add(found);
        }

        Iterator<List<URI>> findsIterator = finds.iterator();
        
        comps = new ArrayList<LWComponent>();
        
        while(findsIterator.hasNext()) {
            found = findsIterator.next();
            if(found !=null) {
                Iterator<URI> foundIterator = found.iterator();
                while(foundIterator.hasNext()) {
                    URI uri = foundIterator.next();
                    LWComponent r = (LWComponent)edu.tufts.vue.rdf.VueIndexedObjectsMap.getObjectForID(uri);
                    if(r!=null && (r.getMap() != null) && r.getMap().equals(VUE.getActiveMap())) {
                        comps.add(r);
                    }
                }
            }
        }
        
        // System.out.println("VUE Object Index: " + edu.tufts.vue.rdf.VueIndexedObjectsMap.objs);
    }
    
    public String getName() {
        return "Search";
    }
    
    public void actionPerformed(ActionEvent e) {
        
        loadKeywords(searchInput.getText());
        performSearch();
        VUE.getSelection().setTo(comps.iterator());
    }
}
