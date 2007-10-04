
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

import edu.tufts.vue.rdf.*;
import edu.tufts.vue.metadata.*;

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
   
    private final static boolean DEBUG_LOCAL = true; 
    
    public static final int FIELD = 0;
    public static final int QUERY = 1;
    
    public static final int SHOW_ACTION = 0;
    public static final int HIDE_ACTION = 1;
    public static final int SELECT_ACTION = 2;
    
    private List<List<URI>> finds = null;
    
    private List<String> tags;
    private Query query;
    private List<LWComponent> comps;
    private static List<LWComponent> globalResults;
    private static List<LWComponent> globalHides;
    
    private JTextField searchInput;
    private edu.tufts.vue.rdf.RDFIndex index;
    
    private int searchType = FIELD;
    private List<VueMetadataElement> searchTerms;
    
    //enable for hide (for now until GUI arrives)
    // private int resultsType = HIDE_ACTION;
    private int resultsType = SELECT_ACTION;
    private static int globalResultsType = SELECT_ACTION;
    
    public SearchAction(JTextField searchInput) {
        super("Search");
        this.searchInput = searchInput;
        runIndex();
        searchType = FIELD;
        /*Thread t = new Thread() {
            public void run() {
                index = new  edu.tufts.vue.rdf.RDFIndex();
                index.index(VUE.getActiveMap());
            }
        };
        t.start();*/ 
    }
    
    public SearchAction(java.util.List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms)
    {  
        super("Search");
        runIndex();
        searchType = QUERY;
        this.searchTerms = searchTerms;
    }
    
    public void runIndex()
    {
        Thread t = new Thread() {
            public void run() {
                index = new  edu.tufts.vue.rdf.RDFIndex();
                index.index(VUE.getActiveMap());
            }
        };
        t.start(); 
    }
    
    /**
     *
     * could help for easy creation/synch of text field in toolbar 
     * from multiple field based version of search window
     * preferably: use previous method instead
     * .. this actually would now use VueMetadataElements most likely..
     * (since even simple search uses those elements now)
     **/
    /*public SearchAction(String[] searchTerms)
    {
        
    }*/
    
    public void loadKeywords(String searchString) {
        
        tags = new ArrayList<String>();
        String[] parsedSpaces = searchString.split(" ");
        for(int i=0;i<parsedSpaces.length;i++) {
            tags.add(parsedSpaces[i]);
        }
        
    }
    
    public void createQuery()
    {
        query = new Query();
        Iterator<VueMetadataElement> criterias = searchTerms.iterator();
        while(criterias.hasNext())
        {
            VueMetadataElement criteria = criterias.next();
            System.out.println("SearchAction adding criteria - getKey(), getValue() " + criteria.getKey() + "," + criteria.getValue());
           // query.addCriteria(criteria.getKey(),criteria.getValue());
            String[] statement = (String[])(criteria.getObject());
            query.addCriteria(criteria.getKey(),criteria.getValue(),statement[2]);
        }
        
        System.out.println("SearchAction: query - " + query.createSPARQLQuery());
        
    }
    
    public void performSearch() {
        
        if(searchType == QUERY)
        {
            createQuery();
        }
        
        // edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().index(VUE.getActiveMap());
        long t0 = System.currentTimeMillis();
        synchronized(index) { 
        if(DEBUG.RDF)System.out.println("Time at the beginning: "+(System.currentTimeMillis()-t0));
        index.remove(index);
        index.index(VUE.getActiveMap());
        System.out.println("SearchAction: index - " + index);
        if(DEBUG.RDF)System.out.println("Performed Index:"+(System.currentTimeMillis()-t0));
        }
        finds = new ArrayList<List<URI>>();
        
        List<URI> found = null;
        
        if(searchType == FIELD)
        {
          for(int i=0;i<tags.size();i++) {
              //found = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().search(tags.get(i));
              if(DEBUG.RDF)System.out.println("Beginning search "+i+" at: "+(System.currentTimeMillis()-t0));
              found = index.searchAllResources(tags.get(i));
              if(DEBUG.RDF)System.out.println("Ending search "+i+" at: "+(System.currentTimeMillis()-t0));
              finds.add(found);
          }
        }
        else if(searchType == QUERY)
        {
            finds.add(index.search(query));
        }

        Iterator<List<URI>> findsIterator = finds.iterator();
        
        comps = new ArrayList<LWComponent>();
        
        while(findsIterator.hasNext()) {
            found = findsIterator.next();
            if(found !=null) {
                Iterator<URI> foundIterator = found.iterator();
                while(foundIterator.hasNext()) {
                    URI uri = foundIterator.next();
                    System.out.println("SearchAction: uri found - " + uri);
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
        
        VUE.getSelection().clear();
        revertSelections();
        if(searchType == FIELD)
        {
          loadKeywords(searchInput.getText());
        }
        performSearch();
        
        if(DEBUG_LOCAL)
        {
          System.out.println("SearchAction: comps size after perform search - " + comps.size());
        }
       
        // this was the marquee approach -- may well be worth creating a new choice for this in future
        // (it was the way select search worked with the old keyword system)
        // VUE.getSelection().setTo(comps.iterator());
        
        revertGlobalSearchSelection();
        Iterator<LWComponent> it = comps.iterator();
        
        if(resultsType == HIDE_ACTION || resultsType == SELECT_ACTION)
        {    
          while(it.hasNext())
          {
             if(resultsType == SELECT_ACTION)
             {
               it.next().setSelected(true);
             }
             if(resultsType == HIDE_ACTION)
             {
               it.next().setHidden(LWComponent.HideCause.DEFAULT);  
             }
          }
        }
        
        globalResults = comps;
        
        if(resultsType == SHOW_ACTION)
        {    
          //globalHides = // opposite of found 
        }
        else if(resultsType == HIDE_ACTION)
        {
          globalHides = comps; 
        }    
        
        // also need to save last results type...
        globalResultsType = resultsType;
        
        VUE.getActiveViewer().repaint();
    }
    
    public void revertSelections()
    {
        revertSelections(comps);
    }
    
    public static void revertSelections(List<LWComponent> toBeReverted)
    {
        if(toBeReverted == null)
            return;
        Iterator<LWComponent> it = toBeReverted.iterator();
        while(it.hasNext())
        {
            it.next().setSelected(false);
        } 
    }
    
    public static void revertGlobalSearchSelection()
    {
        //if(globalResultType == SELECT_ACTION)
        revertSelections(globalResults);
    }
    
}
