
/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
   
    private final static boolean DEBUG_LOCAL = false; 

    private final static boolean MARQUEE = true;
    
    public static final int FIELD = 0;
    public static final int QUERY = 1;
    
    public static final int SHOW_ACTION = 0;
    public static final int HIDE_ACTION = 1;
    public static final int SELECT_ACTION = 2;
    public static final int COPY_ACTION = 3;
    
    public static final int SEARCH_SELECTED_MAP = 0;
    public static final int SEARCH_ALL_OPEN_MAPS = 1;
    
    public static final int AND = 0;
    public static final int OR = 1;
    
    private int crossTermOperator = AND;
    
    private List<List<URI>> finds = null;
    
    private List<String> tags;
    private Query query;
    private List<Query> queryList;
    private List<LWComponent> comps;
    private static List<LWComponent> globalResults;
    private static List<LWComponent> globalHides;
    
    private JTextField searchInput;
    private edu.tufts.vue.rdf.RDFIndex index;
    
    private int searchType = FIELD;
    private List<VueMetadataElement> searchTerms;
    
    private int searchLocationType = SEARCH_SELECTED_MAP;
    
    //enable for show or hide (for now until GUI dropdown installed)
    private int resultsType = SHOW_ACTION;
    //private int resultsType = HIDE_ACTION;
    //private int resultsType = SELECT_ACTION;
    private static int globalResultsType = SHOW_ACTION;
    
    private static int searchResultsMaps = 1;
    
    private boolean setBasic = true;
    
    private List<String> textToFind = new ArrayList<String>();
    private boolean actualCriteriaAdded = false;
    
    private boolean treatNoneSpecially = false;
    private boolean textOnly = false;
    private boolean metadataOnly = false;
    
    public SearchAction(JTextField searchInput) {
        super("Search");
        this.searchInput = searchInput;
        runIndex();
        searchType = FIELD; 
    }
    
    public SearchAction(java.util.List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms)
    {  
        super("Search");
        runIndex();
        searchType = QUERY;
        this.searchTerms = searchTerms;
    }
    
    public void setOperator(int operator)
    {
        crossTermOperator = operator;
    }
    
    public void setBasic(boolean basic)
    {
        setBasic = basic;
    }
    
    public void setNoneIsSpecial(boolean set)
    {
        treatNoneSpecially = set;
    }
    
    // runs special index with only metadata
    public void setMetadataOnly(boolean set)
    {
        metadataOnly = set;
    }
    
    public void setTextOnly(boolean set)
    {
        textOnly = set;
    }
    
    public void runIndex()
    {
        Thread t = new Thread() {
            public void run() {
             index = new  edu.tufts.vue.rdf.RDFIndex();
                //index.index(VUE.getActiveMap());
                
             if(searchLocationType == SEARCH_ALL_OPEN_MAPS)
             { 
            
                    if(DEBUG_LOCAL)
                    { 
                      System.out.println("SearchAction: Searching all open maps...");
                    }
          
                    Iterator<LWMap> maps = VUE.getLeftTabbedPane().getAllMaps();
                    while(maps.hasNext())
                    {    
                         index.index(maps.next(),metadataOnly);
                    }
             }    
             else // default is SEARCH_SELECTED_MAP
             {    
                    index.index(VUE.getActiveMap(),metadataOnly);
             }
                
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
    
    public void createQuery()
    {
        query = new Query();
        textToFind = new ArrayList<String>();
        Iterator<VueMetadataElement> criterias = searchTerms.iterator();
        actualCriteriaAdded = false;
        while(criterias.hasNext())
        {
            VueMetadataElement criteria = criterias.next();
            if(DEBUG_LOCAL)
            {    
              System.out.println("SearchAction adding criteria - getKey(), getValue() " + criteria.getKey() + "," + criteria.getValue());
            }
           // query.addCriteria(criteria.getKey(),criteria.getValue());
            String[] statement = (String[])(criteria.getObject());
            
            if(setBasic != true) 
            {  
                
              if(DEBUG_LOCAL)
              {    
                System.out.println("query setBasic != true");
                System.out.println("criteria.getKey() " + criteria.getKey());
                System.out.println("RDFIndex.VUE_ONTOLOGY+ none " + RDFIndex.VUE_ONTOLOGY+"none");
                System.out.println("text only = " + textOnly);
              }
              if(criteria.getKey().equals("http://vue.tufts.edu/vue.rdfs#"+"none") && treatNoneSpecially)
              {
                //System.out.println("adding criteria * ...");  
                  
                //query.addCriteria("*",criteria.getValue(),statement[2]);
                textToFind.add(criteria.getValue());
              }   
              else if(textOnly)
              {
                textToFind.add(criteria.getValue()); 
              }
              else
              {    
                query.addCriteria(criteria.getKey(),criteria.getValue(),"CONTAINS");  
                //query.addCriteria(criteria.getKey(),criteria.getValue(),statement[2]); // would be nice to be able to say
                // query condition here -- could do as subclass of VueMetadataElement? getCondition()? then a search
                // can be metadata too..
                actualCriteriaAdded = true;
              }
            }
            else
            {
              query.addCriteria(RDFIndex.VUE_ONTOLOGY+Constants.LABEL,criteria.getValue(),"CONTAINS");
              //System.out.println("query -- setBasic == true");
              //query.addCriteria(RDFIndex.VUE_ONTOLOGY+Constants.LABEL,criteria.getValue(),statement[2]); // see comments just above
              actualCriteriaAdded = true;
            }
        }
        
        if(DEBUG_LOCAL)
        {
          System.out.println("SearchAction: query - " + query.createSPARQLQuery());
        }
        
    }
    
    public void createQueries()
    {
        queryList = new ArrayList<Query>();
        
        //query = new Query();
        textToFind = new ArrayList<String>();
        Iterator<VueMetadataElement> criterias = searchTerms.iterator();
        actualCriteriaAdded = false;
        while(criterias.hasNext())
        {
            Query currentQuery = new Query();
            
            
            VueMetadataElement criteria = criterias.next();
            if(DEBUG_LOCAL)
            {    
              System.out.println("SearchAction adding criteria - getKey(), getValue() " + criteria.getKey() + "," + criteria.getValue());
            }
           // query.addCriteria(criteria.getKey(),criteria.getValue());
            String[] statement = (String[])(criteria.getObject());
            
            if(setBasic != true) 
            {  
                
              if(DEBUG_LOCAL)
              {    
                System.out.println("query setBasic != true");
                System.out.println("criteria.getKey() " + criteria.getKey());
                System.out.println("RDFIndex.VUE_ONTOLOGY+ none " + RDFIndex.VUE_ONTOLOGY+"none");
                System.out.println("text only = " + textOnly);
              }
              if(criteria.getKey().equals("http://vue.tufts.edu/vue.rdfs#"+"none") && treatNoneSpecially)
              {
                //System.out.println("adding criteria * ...");  
                  
                //query.addCriteria("*",criteria.getValue(),statement[2]);
                textToFind.add(criteria.getValue());
              }   
              else if(textOnly)
              {
                textToFind.add(criteria.getValue()); 
              }
              else
              {    
                currentQuery.addCriteria(criteria.getKey(),criteria.getValue(),statement[2]); // would be nice to be able to say
                // query condition here -- could do as subclass of VueMetadataElement? getCondition()? then a search
                // can be metadata too..
                actualCriteriaAdded = true;
              }
            }
            else
            {
              //System.out.println("query -- setBasic == true");
              currentQuery.addCriteria(RDFIndex.VUE_ONTOLOGY+Constants.LABEL,criteria.getValue(),statement[2]); // see comments just above
              actualCriteriaAdded = true;
            }
            
            queryList.add(currentQuery);
        }
        
        if(DEBUG_LOCAL)
        {
          for(int i=0;i<queryList.size();i++)
          {
            System.out.println("SearchAction: query - " + "i: " + queryList.get(i).createSPARQLQuery());
          }
        }        
        
        
    }
    
    public void performSearch(final int searchLocationType) 
    {
       //Thread t = new Thread()
       //{
       //    public void run()
       //    {
             runSearchThread(searchLocationType);               
       //    }
       //};
       //t.start();
    }   
    
    // note: performSearch determines if this is actually a seperate thread
    public void runSearchThread(int searchLocationType)
    {
        
        if(searchType == QUERY && crossTermOperator == AND)
        {
            createQuery();
        }
        else // todo: AND in first query
        if(searchType == QUERY && crossTermOperator == OR)
        {
            createQueries();
        }
       // else // todo: for gathering text into its own list
             // hmm seems to suggest a superclass for Query?
        /*if(searchType == QUERY && crossTermOperation == OR)
        {
            getSearchStrings();
        }*/
        
        // edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().index(VUE.getActiveMap());
        
        
        long t0 = System.currentTimeMillis();
        
        synchronized(index) { 
        if(DEBUG.RDF)System.out.println("Time at the beginning: "+(System.currentTimeMillis()-t0));
        index.remove(index);
        
        if(searchLocationType == SEARCH_ALL_OPEN_MAPS)
        { 
            
          if(DEBUG_LOCAL)
          {
              System.out.println("SearchAction: Searching all open maps...");
          }
          
          Iterator<LWMap> maps = VUE.getLeftTabbedPane().getAllMaps();
          while(maps.hasNext())
          {
              index.index(maps.next(),metadataOnly);
          }
        }    
        else // default is SEARCH_SELECTED_MAP
        {    
          index.index(VUE.getActiveMap(),metadataOnly);
        }
        
        
        if(DEBUG_LOCAL)
        {    
          System.out.println("SearchAction: index - " + index);
        }
        if(DEBUG.RDF)System.out.println("Performed Index:"+(System.currentTimeMillis()-t0));
        } // end synchronized block
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
            //System.out.println("query result " + index.search(query) + " for query " + query.createSPARQLQuery());
            
            if(actualCriteriaAdded && crossTermOperator == AND)
            {    
              finds.add(index.search(query));
            }
            else if(actualCriteriaAdded && crossTermOperator == OR)
            {
              Iterator<Query> queries = queryList.iterator();
              while(queries.hasNext())
              {
                  finds.add(index.search(queries.next()));
              }
            }
            
            boolean firstFinds = true;
            // may need to do AND by hand here...
            // this is OR 
            // do as list of text to finds? or cull the list before
            // doing this loop for ands? thats in both createquery and createqueries right now...
            if(textToFind.size() != 0)
            {
               Iterator<String> textIterator = textToFind.iterator(); 
               while(textIterator.hasNext())
               {
                   String text = textIterator.next();
                   
                   if(DEBUG_LOCAL)
                   {    
                     System.out.println("\n\n**********\n Searching all resources for: " + text );
                   }
                   
                   found = index.searchAllResources(text);
                   
                   
                   if(crossTermOperator == OR || firstFinds == true)
                   {
                      finds.add(found);   
                      firstFinds = false;
                   }
                   else
                   {
                      // note: this iterator should usually have only one element in this case
                      Iterator<List<URI>> findsIterator = finds.iterator();
                      while(findsIterator.hasNext())
                      {
                          List<URI> current = findsIterator.next();
                          Iterator<URI> alreadyFound = current.iterator();
                          List<URI> toBeRemoved = new ArrayList<URI>();
                          while(alreadyFound.hasNext())
                          {
                            URI currentURI = alreadyFound.next();
                            
                            if(DEBUG_LOCAL)
                            {
                               System.out.println("SearchAction - already found " + currentURI + "," + text);
                            }
                            
                            if(!found.contains(currentURI))
                            {
                                if(DEBUG_LOCAL)
                                {
                                    System.out.println("SearchAction - scheduling uri to be removed: (text follows) " + currentURI + "," + text);
                                }
                                
                                toBeRemoved.add(currentURI);
                            }
                          }
                          
                          Iterator<URI> removeThese = toBeRemoved.iterator();
                          while(removeThese.hasNext())
                          {
                            current.remove(removeThese.next());
                          }
                      }
                   }
                   
                   firstFinds = false;
               }
               
               /*
               while(textIterator.hasNext())
               {
                 //loadKeywords(textIterator.next());
                 //for(int i=0;i<tags.size();i++)
                 //{    
                   //System.out.println("tags.get(i)" + tags.get(i));
                   //found = index.searchAllResources(tags.get(i));
                   found = index.searchAllResources(textIterator.next());
                   //System.out.println("found " + found);
                   //finds.add(found);
                 //}
               }*/
            }
           
        } 

        Iterator<List<URI>> findsIterator = finds.iterator();
        
        comps = new ArrayList<LWComponent>();
        
        Iterator<LWMap> mapsIterator = VUE.getLeftTabbedPane().getAllMaps();
        
        ArrayList<LWMap> maps = new ArrayList<LWMap>();
        while(mapsIterator.hasNext())
        {
            maps.add(mapsIterator.next());
        }
        
        while(findsIterator.hasNext()) {
            found = findsIterator.next();
            if(found !=null) {
                Iterator<URI> foundIterator = found.iterator();
                while(foundIterator.hasNext()) {
                    URI uri = foundIterator.next();
                    if(DEBUG_LOCAL)
                    {    
                      System.out.println("SearchAction: uri found - " + uri);
                    }
                    LWComponent r = (LWComponent)edu.tufts.vue.rdf.VueIndexedObjectsMap.getObjectForID(uri);
                    //if(r!=null && (r.getMap() != null) && r.getMap().equals(VUE.getActiveMap())) {
                    if(r!=null && (r.getMap() != null) && maps.contains(r.getMap())) {
                        comps.add(r);
                    }
                    //else if(r != null && (r.getMap() !=null) && maps.contains(r.getMap()))
                }
            }
        }
        
        // System.out.println("VUE Object Index: " + edu.tufts.vue.rdf.VueIndexedObjectsMap.objs);
        
        //SwingUtilities.invokeLater(new Thread(){
        //   public void run()
        //   {
               displaySearchResults();
        //   }
        //});
    }
    
    public String getName() {
        return "Search";
    }
    
    public void setLocationType(int type)
    {
        searchLocationType = type;
    }
    
    public void actionPerformed(ActionEvent e) {
        
       // runIndex();
        
        index = new  edu.tufts.vue.rdf.RDFIndex();
        //edu.tufts.vue.rdf.VueIndexedObjectsMap.clear();
        
        VUE.getSelection().clear();

        if(searchType == FIELD)
        {
          revertSelections();
          loadKeywords(searchInput.getText());
        }
        performSearch(searchLocationType);
        
    }
    
    public void displaySearchResults()
    {
        if(searchLocationType == SEARCH_ALL_OPEN_MAPS)
        {
            Iterator<LWMap> allOpenMaps = VUE.getLeftTabbedPane().getAllMaps();
            
            LWMap searchResultMap = new LWMap("Search Result #" + searchResultsMaps++);
            
            while(allOpenMaps.hasNext())
            {
                Iterator<LWComponent> components = allOpenMaps.next().getAllDescendents(LWComponent.ChildKind.PROPER).iterator();   
                while(components.hasNext())
                {
                    LWComponent next = components.next();
                    if(comps.contains(next))
                    {
                        searchResultMap.add(next.duplicate());
                    }
                }
            }
            
            VUE.displayMap(searchResultMap);
            
            return;
        }
        
        if(resultsType == COPY_ACTION)
        {
            LWMap searchResultMap = new LWMap("Search Result #" + searchResultsMaps++);
            
            Iterator<LWComponent> components = VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.PROPER).iterator();   
            while(components.hasNext())
            {
                LWComponent next = components.next();
                if(comps.contains(next))
                {
                   searchResultMap.add(next.duplicate());
                }
            }
            
            VUE.displayMap(searchResultMap);
            
            return; 
        }
        
        if(DEBUG_LOCAL)
        {
          System.out.println("SearchAction: comps size after perform search - " + comps.size());
        }
        
        revertGlobalSearchSelection();
        globalResultsType = resultsType;
        
        // find Image Nodes within comps and add images to list (then add list)
        ArrayList<LWImage> images = new ArrayList<LWImage>();
        Iterator<LWComponent> compsIterator = comps.iterator();
        while(compsIterator.hasNext())
        {
            LWComponent current = compsIterator.next();
            if(current instanceof LWNode)
            {
                LWNode currentNode = (LWNode)current;
                if(LWNode.isImageNode(currentNode))
                {
                    images.add(currentNode.getImage());
                }
            }
        }
        
        comps.addAll(images);
        
        Iterator<LWComponent> it = comps.iterator();
        
        if(resultsType == HIDE_ACTION || resultsType == SELECT_ACTION)
        {    
          while(it.hasNext())
          {
             if(resultsType == SELECT_ACTION)
             {
               if(MARQUEE == false)
               {
                 it.next().setSelected(true);
               }
               else
               {
                 VUE.getSelection().add(it.next());
               }
             }
             if(resultsType == HIDE_ACTION)
             {
               it.next().setHidden(LWComponent.HideCause.DEFAULT);  
               //it.next().setFiltered(true))
             }
          }
        }
        
        globalResults = comps;
        
        if(resultsType == SHOW_ACTION)
        {    
          //globalHides = // opposite of comps 
          Collection<LWComponent> allComps = tufts.vue.VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.PROPER);
          globalHides = new ArrayList();
          Iterator<LWComponent> allIt = allComps.iterator();
          while(allIt.hasNext())
          {
              LWComponent comp = allIt.next();
              if(!comps.contains(comp))
              {
                  if(DEBUG_LOCAL)
                  {
                      System.out.println("SearchAction adding " + comp.getLabel() + " to globalHides and hiding");
                  }
                  
                  comp.setHidden(LWComponent.HideCause.DEFAULT);
                  //comp.setFiltered(true);
                  globalHides.add(comp);
              }

          }
        }
        else if(resultsType == HIDE_ACTION)
        {
          globalHides = comps; 
        }    
        
        // also need to save last results type...
        globalResultsType = resultsType;
        
        //VueToolbarController.getController().selectionChanged(VUE.getSelection());
        //VUE.getActiveViewer().requestFocus();
        VUE.getActiveViewer().grabVueApplicationFocus("search",null);
        VUE.getActiveViewer().repaint();

    }
    
    public void revertSelections()
    {   
        revertGlobalSearchSelection();
    }
    
    public static void revertSelections(List<LWComponent> toBeReverted)
    {
        if(MARQUEE)
            return;
        
        if(toBeReverted == null)
            return;
        Iterator<LWComponent> it = toBeReverted.iterator();
        while(it.hasNext())
        {
            if(MARQUEE == false)
            {
              it.next().setSelected(false);
            }
            else
            {
              // already done on click on VUE map
              //VUE.getSelection().clear();
            }
        } 
    }
    
    public static void revertSelectionsFromMSGUI(List<LWComponent> toBeReverted)
    {
        //if(MARQUEE)
        //    return;

        if(MARQUEE)
        {
          VUE.getSelection().clear();
          VUE.getActiveViewer().repaint();
          return;
        }
        
        if(toBeReverted == null)
            return;
        Iterator<LWComponent> it = toBeReverted.iterator();
        while(it.hasNext())
        {
            if(MARQUEE == false)
            {
              it.next().setSelected(false);
            }
            else
            {
              /*Thread t = new Thread()
              {
                public void run()
                {
                  //VUE.getSelection().clear();
                }
              };
              try
              {        
                //SwingUtilities.invokeLater(t);
                //t.start();
              }
              catch(Exception e)
              {
                  System.out.println("SearchAction - Exception trying to clear selection: " + e);
              }*/
            }
        } 

    }
    
    public void setResultsType(String type)
    {
        if(type.equals("Show"))
            resultsType = SHOW_ACTION;
        if(type.equals("Hide"))
            resultsType = HIDE_ACTION;
        if(type.equals("Select"))
            resultsType = SELECT_ACTION;
        if(type.equals("Copy to new map"))
            resultsType = COPY_ACTION;
        
        //globalResultsType = resultsType;
    }
    
    public static void showHiddenComponents(Collection<LWComponent> toBeReverted)
    {
        if(toBeReverted == null)
            return;
        Iterator<LWComponent> it = toBeReverted.iterator();
        while(it.hasNext())
        {
            it.next().clearHidden(LWComponent.HideCause.DEFAULT);
            //it.next().setFiltered(false);
        } 
    }
    
    public static void revertGlobalSearchSelection()
    {
        if(globalResultsType == SELECT_ACTION)
          revertSelections(globalResults);
        if(globalResultsType == HIDE_ACTION)
          showHiddenComponents(globalResults);
        if(globalResultsType == SHOW_ACTION)
          showHiddenComponents(globalHides);
    }
    
    public static void revertGlobalSearchSelectionFromMSGUI()
    {
        if(globalResultsType == SELECT_ACTION)
          revertSelectionsFromMSGUI(globalResults);
        if(globalResultsType == HIDE_ACTION)
          showHiddenComponents(globalResults);
        if(globalResultsType == SHOW_ACTION)
          showHiddenComponents(globalHides);
    }
    
}
