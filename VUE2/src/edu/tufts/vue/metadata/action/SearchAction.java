
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

package edu.tufts.vue.metadata.action;

import edu.tufts.vue.rdf.*;
import edu.tufts.vue.layout.Cluster2Layout;
import edu.tufts.vue.metadata.*;

import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import tufts.vue.*;
import tufts.vue.LWComponent.HideCause;

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
    
    // note: default for other nested nodes is currently to treat
    // them as stand alone components for purposes of search
    // considering perhaps showing parent for any component
    // found nested within another as issue for dec19-2007 team
    // meeting.
    private final static boolean AUTO_SHOW_NESTED_IMAGES = true;
    private final static boolean DO_NOT_SELECT_NESTED_IMAGES = true;
    private final static boolean DO_NOT_SELECT_SLIDE_COMPONENTS = true;
    
    public static final int FIELD = 0;
    public static final int QUERY = 1;
    
    public static final int SHOW_ACTION = 0;
    public static final int HIDE_ACTION = 1;
    public static final int SELECT_ACTION = 2;
    public static final int COPY_ACTION = 3;
    public static final int CLUSTER_ACTION = 4;
    
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
    
    private boolean everything = false;
    private boolean searchBtnClicked = false;
    public SearchAction(JTextField searchInput) {
        super(VueResources.getString("searchgui.search"));
        this.searchInput = searchInput;
        runIndex();
        searchType = FIELD; 
    }
    
    public SearchAction(java.util.List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms)
    {  
        super(VueResources.getString("searchgui.search"));
        runIndex();
        searchType = QUERY;
        this.searchTerms = searchTerms;
    }
    
    public static int getGlobalResultsType()
    {
        return globalResultsType;
    }
    
    public void setEverything(boolean everything)
    {
        this.everything = everything;
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
                         index.index(maps.next(),metadataOnly,everything,false);
                    }
             }    
             else // default is SEARCH_SELECTED_MAP
             {
            	 if(VUE.getActiveMap()!=null){
                    index.index(VUE.getActiveMap(),metadataOnly,everything,true);
            	 }
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
        if(searchType == QUERY && VUE.getMetadataSearchMainPanel().getSelectedOperator() == AND)
        {        	
            createQuery();
        }
        else // todo: AND in first query
        if(searchType == QUERY && VUE.getMetadataSearchMainPanel().getSelectedOperator() == OR)
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
              index.index(maps.next(),metadataOnly,everything,false);
          }
        }    
        else // default is SEARCH_SELECTED_MAP
        {    
          index.index(VUE.getActiveMap(),metadataOnly,everything,true);
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
            
            if(actualCriteriaAdded && VUE.getMetadataSearchMainPanel().getSelectedOperator() == AND)
            {    
              finds.add(index.search(query));
            }
            else if(actualCriteriaAdded && VUE.getMetadataSearchMainPanel().getSelectedOperator() == OR)
            {
              Iterator<Query> queries = queryList.iterator();
              while(queries.hasNext())
              {
                  finds.add(index.search(queries.next()));
              }
            }
            
            boolean firstFinds = true;
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
        return VueResources.getString("searchgui.search");
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
                Iterator<LWComponent> components = allOpenMaps.next().getAllDescendents(LWComponent.ChildKind.EDITABLE).iterator();   
                while(components.hasNext())
                {
                    LWComponent next = components.next();
                    if(comps.contains(next))
                    {
                        
                       LWComponent duplicate = next.duplicate();
                       LWComponent parent = next.getParent();
                       if(parent !=null && !comps.contains(parent))
                       {
                         if(parent instanceof LWNode)
                         {    
                           duplicate.setLocation(parent.getLocation());
                         }
                         
                         
                         if(LWNode.isImageNode(parent))
                         {
                           if(!comps.contains(parent))
                           {
                               LWComponent dup = parent.duplicate();
                               if(!(dup instanceof LWSlide) && !dup.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                                 && (!(dup.hasAncestorOfType(LWSlide.class))) )  
                                 searchResultMap.add(dup);
                           }
                         }   
                         else
                         {    
                           if(!(duplicate instanceof LWSlide) && !duplicate.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                              && (!(duplicate.hasAncestorOfType(LWSlide.class))) )    
                             searchResultMap.add(duplicate);
                         }
                         
                         /*if(next.hasFlag(LWComponent.Flag.SLIDE_STYLE))
                         {
                            LWSlide slide = (LWSlide)next.getParentOfType(LWSlide.class);
                            //searchResultMap.add(slide);
                            searchResultMap.add(slide.getSourceNode());
                         }*/
                         
                       } 

                    }
                }
            }
            
            VUE.displayMap(searchResultMap);
            
            return;
        }
        
        if(resultsType == COPY_ACTION)
        {
            LWMap searchResultMap = new LWMap("Search Result " + searchResultsMaps++);
            
            HashMap<LWComponent,LWComponent> duplicates = new HashMap<LWComponent,LWComponent>();
            
            Iterator<LWComponent> components = VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.EDITABLE).iterator();   
            while(components.hasNext())
            {
                LWComponent next = components.next();
                if(comps.contains(next))
                {
                   LWComponent duplicate = next.duplicate();
                   
                   duplicates.put(next,duplicate);
                   
                   LWComponent parent = next.getParent();
                   if(parent !=null && !comps.contains(parent))
                   {
                       if(parent instanceof LWNode)
                       {    
                         duplicate.setLocation(parent.getLocation());
                       }
                       
                       /*if(next instanceof LWLink)
                       {
                         LWLink link = (LWLink)next;
                         LWComponent head = link.getHead();
                         if(head != null && comps.contains(head))
                             ((LWLink)duplicate).setHead(head); // OOPS needs to be
                                                                // head's duplicate
                         LWComponent tail = link.getTail();
                         if(tail != null && comps.contains(tail))
                             ((LWLink)duplicate).setTail(tail); // double OOPS
                       }*/
                       
                      
                       
                       // do we need this code any more? see
                       // "raw image" search bug in jira
                       // if we do, links may have to be handled
                       // correctly for these nodes as well
                       if(LWNode.isImageNode(parent))
                       {
                         if(!comps.contains(parent))
                         {
                               if(!(parent instanceof LWSlide) && !parent.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                                 && (!parent.hasAncestorOfType(LWSlide.class)))   
                                 searchResultMap.add(parent.duplicate());
                         }
                       }   
                       else
                       {    
                           if(!(duplicate instanceof LWSlide) && !duplicate.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                               && (!(duplicate.hasAncestorOfType(LWSlide.class))))  
                             searchResultMap.add(duplicate);
                       }

                       
                   }
                  
                }
            }
            
            
            Iterator<LWComponent> components2 = VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.EDITABLE).iterator(); 
            while(components2.hasNext())
            {
                       LWComponent next = components2.next();
                
                       if(next instanceof LWLink && duplicates.get(next) != null)
                       {
                         LWLink link = (LWLink)next;
                         LWComponent head = link.getHead();
                         if(head != null && comps.contains(head))
                             ((LWLink)duplicates.get(next)).setHead(duplicates.get(head)); 
                         LWComponent tail = link.getTail();
                         if(tail != null && comps.contains(tail))
                             ((LWLink)duplicates.get(next)).setTail(duplicates.get(tail)); 
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
        
        ArrayList<LWComponent> images = new ArrayList<LWComponent>();
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
            
            /*if(current.hasFlag(LWComponent.Flag.SLIDE_STYLE))
            {
                            LWSlide slide = (LWSlide)current.getParentOfType(LWSlide.class);
                            images.add(slide);
                            //LWNode source  = ((LWNode)slide.getSourceNode());
                            //images.add(source);
                            if(LWNode.isImageNode(source))
                            {
                                images.add(source.getImage());
                            }
                            
            }*/
            
            if(current instanceof LWImage)
            {
                LWImage currentImage = (LWImage)current;
                if(currentImage.isNodeIcon() && currentImage.getParent() != null)
                {
                  images.add(currentImage.getParent());
                }
            }
            
        }
        
        comps.addAll(images);
        
        if(resultsType == SELECT_ACTION && DO_NOT_SELECT_SLIDE_COMPONENTS)
        {
                 
           Iterator<LWComponent> it = comps.iterator();        
                 
           List<LWComponent> slideComponents = new ArrayList<LWComponent>();
           
           while(it.hasNext())
           {
              LWComponent next = it.next();
              if(next.hasFlag(LWComponent.Flag.SLIDE_STYLE))
              {
                  slideComponents.add(next);
              }
           }
           
           Iterator<LWComponent> slides = slideComponents.iterator();
           while(slides.hasNext())
           {
              LWComponent slide = slides.next();
              if(!(slide instanceof LWSlide))
                comps.remove(slide);
           }
           
        }
        
        if(resultsType == SELECT_ACTION && DO_NOT_SELECT_NESTED_IMAGES)
        {
           Iterator<LWComponent> it2 = comps.iterator(); 
           
           List<LWComponent> toNotBeSelected = new ArrayList<LWComponent>();
           
           while(it2.hasNext())
           {
              LWComponent next = it2.next();
              
              Iterator<LWComponent> nestedComponents = next.getAllDescendents().iterator();
              while(nestedComponents.hasNext())
              {
                  LWComponent nextNested = nestedComponents.next();
                  if(comps.contains(nextNested))
                  {
                      toNotBeSelected.add(nextNested);
                  }
              }
              
 
              if(next instanceof LWNode && LWNode.isImageNode(next))
              {
                  toNotBeSelected.add(((LWNode)next).getImage());
              }

           }
           
           Iterator<LWComponent> dontSelect = toNotBeSelected.iterator();
           while(dontSelect.hasNext())
           {
              LWComponent removeThis = dontSelect.next();
              if(comps.contains(removeThis))
              {
                  comps.remove(removeThis);
              }
           }
           
        }
        
        // also hide or show elements inside of groups when group is also
        // in the search results set
        if(resultsType == HIDE_ACTION || resultsType == SHOW_ACTION )
        {
            List<LWComponent> groupDescendants = new ArrayList<LWComponent>();
            
            Iterator<LWComponent> groupSearch = comps.iterator();
            
            while(groupSearch.hasNext())
            {
                LWComponent next = groupSearch.next();
                if(next instanceof LWGroup)
                {
                    // todo: don't add duplicates!'
                    groupDescendants.addAll(next.getAllDescendents(LWComponent.ChildKind.EDITABLE));
                }
            }
            
            comps.addAll(groupDescendants);
        }
        
        if(resultsType == HIDE_ACTION || resultsType == SELECT_ACTION || resultsType == CLUSTER_ACTION)
        {    
            
          Iterator<LWComponent> it3 = comps.iterator();  
            
          while(it3.hasNext())
          {
             if(resultsType == SELECT_ACTION || resultsType == CLUSTER_ACTION)
             {
               if(MARQUEE == false)
               {
                 it3.next().setSelected(true);
               }
               else
               {
                 LWComponent comp = it3.next();
                 
                 if(!comp.getLayer().isHidden() && !(comp instanceof LWSlide) && !comp.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                   && (!(comp.hasAncestorOfType(LWSlide.class))))  
                   VUE.getSelection().add(comp);
               }
             }
             if(resultsType == HIDE_ACTION)
             {
               // VUE-892 -- switch back to setFiltered (needs change in LWImage to work for image nodes, but this
               // will handle child nodes/images correctly in non image nodes)
               //it.next().setHidden(LWComponent.HideCause.DEFAULT);
                 
                 
               LWComponent comp = it3.next();
                 
               if(!(comp instanceof LWSlide) && !comp.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                 && (!(comp.hasAncestorOfType(LWSlide.class))) && !(comp instanceof LWGroup) )  
                   comp.setFiltered(true);
             }
          }
        }
        
        globalResults = comps;
        
        if(resultsType == SHOW_ACTION)
        {    
          
          Iterator<LWComponent> it4 = comps.iterator();  
            
          if(AUTO_SHOW_NESTED_IMAGES)
          {    
            // checking all children of nodes in search results to see if they
            // are images or image nodes
            // to be done: for select, possibly actually remove selection for any 
            // children of search results
            // also to be done: image or image node results should also show
            // parents (but not non image results)
            List<LWComponent> toBeAdded = new ArrayList<LWComponent>();  
            
            while(it4.hasNext())
            {
              LWComponent current = it4.next();
              Iterator<LWComponent> children = current.getAllDescendents().iterator();
              while(children.hasNext())
              {
                  LWComponent next = children.next();
                  if (( (next instanceof LWImage) || LWNode.isImageNode(next))
                       && !comps.contains(next) )
                  {
                    toBeAdded.add(next);         
                  }
              }
            }
          
            Iterator<LWComponent> addThese = toBeAdded.iterator();
            while(addThese.hasNext())
            {
              comps.add(addThese.next());
            }
          }
            
            
          //globalHides = // opposite of comps 
          Collection<LWComponent> allComps = tufts.vue.VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.EDITABLE);
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
                  
                  // VUE-892 -- switch back to setFiltered (needs change in LWImage to work for image nodes, but this
                  // will handle child nodes/images correctly in non image nodes)
                  //comp.setHidden(LWComponent.HideCause.DEFAULT);
                  if(!(comp instanceof LWSlide) && !comp.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                    && (!(comp.hasAncestorOfType(LWSlide.class))) && !(comp instanceof LWGroup) )  
                  {
                    comp.setFiltered(true);
                    globalHides.add(comp);
                  }
              }

          }
        }
        else if(resultsType == HIDE_ACTION)
        {
          globalHides = comps; 
        }    
        
        if (resultsType == CLUSTER_ACTION) {
        	Cluster2Layout layout = new Cluster2Layout();

        	layout.layout(VUE.getSelection());
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
        if(type.equals(VueResources.getString("searchgui.show")))
            resultsType = SHOW_ACTION;
        else if(type.equals(VueResources.getString("searchgui.hide")))
            resultsType = HIDE_ACTION;
        else if(type.equals(VueResources.getString("searchgui.select")))
            resultsType = SELECT_ACTION;
        else if(type.equals(VueResources.getString("searchgui.copynewmap")))
            resultsType = COPY_ACTION;
        else if(type.equals(VueResources.getString("searchgui.cluster")))
            resultsType = CLUSTER_ACTION;
        
        //globalResultsType = resultsType;
    }
    
    public static void showHiddenComponents(Collection<LWComponent> toBeReverted)
    {
        if(toBeReverted == null)
            return;
        Iterator<LWComponent> it = toBeReverted.iterator();
        while(it.hasNext())
        {
            // VUE-892 -- switch back to setFiltered (needs change in LWImage to work for image nodes, but this
            // will handle child nodes/images correctly in non image nodes)
            //it.next().clearHidden(LWComponent.HideCause.DEFAULT);
            LWComponent comp = it.next();
            
            if(!(comp instanceof LWSlide) && !comp.hasFlag(LWComponent.Flag.SLIDE_STYLE)
               && (!(comp.hasAncestorOfType(LWSlide.class))))  
              comp.setFiltered(false);
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
