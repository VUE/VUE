
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

package edu.tufts.vue.metadata.action;

import edu.tufts.vue.rdf.*;
import edu.tufts.vue.layout.Cluster2Layout;
import edu.tufts.vue.metadata.*;

import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.net.*;
import java.util.*;

import javax.swing.*;

import tufts.vue.*;
import tufts.vue.LWComponent.HideCause;
import tufts.vue.LWMap.Layer;
import tufts.vue.gui.GUI;
import tufts.Util;

/**
 * SearchAction.java
 *
 * Created on July 27, 2007, 2:21 PM
 *
 * @author dhelle01
 *
 * Todo: RDF is overkill for our purposes -- a simple parameterized traversal would be
 * much simpler.
 */
public class SearchAction extends AbstractAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SearchAction.class);

    private final static boolean DEBUG_LOCAL = false;

    private final static boolean MARQUEE = true;  // do not change -- no longer supported as false
    
    // note: default for other nested nodes is currently to treat
    // them as stand alone components for purposes of search
    // considering perhaps showing parent for any component
    // found nested within another as issue for dec19-2007 team
    // meeting.
    private final static boolean AUTO_SHOW_NESTED_IMAGES = true;
    private final static boolean DO_NOT_SELECT_NESTED_IMAGES = true;
    private final static boolean DO_NOT_SELECT_SLIDE_COMPONENTS = true;
    
    private static final String TYPE_FIELD = "<search-type-field>";
    private static final String TYPE_QUERY = "<search-type-query>";
    
    public static final int SHOW_ACTION = 0;
    public static final int HIDE_ACTION = 1;
    public static final int SELECT_ACTION = 2;
    public static final int COPY_ACTION = 3;
    public static final int CLUSTER_ACTION = 4;
    public static final int LINK_ACTION = 5;
    
    public static final String SEARCH_SELECTED_MAP = "<search-selected-map>";
    public static final String SEARCH_ALL_OPEN_MAPS = "<search-all-open-maps>";
    
    public static final String AND = "<and>";
    public static final String OR = "<or>";
    
    private final List<VueMetadataElement> searchTerms;
    private final String searchType;
    
    private String crossTermOperator = AND;
    
    private List<String> tags;
    private static List<LWComponent> globalResults;
    private static List<LWComponent> globalHides;
    
    private JTextField searchInput;
    
    private String searchLocationType = SEARCH_SELECTED_MAP;
    
    //enable for show or hide (for now until GUI dropdown installed)
    private int resultsType = SHOW_ACTION;
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

    private static edu.tufts.vue.rdf.RDFIndex lastMapIndex;
    private static tufts.vue.LWMap lastIndexedMap;
    private static long lastIndexedMapState = -1;
    
    // Wierdly, SearchTextField does not use this constructor / FIELD type, but MetadataaSearchMainGUI does...
    public SearchAction(JTextField searchInput) {
        super(VueResources.getString("searchgui.search"));
        this.searchInput = searchInput;
        runIndex();
        searchType = TYPE_FIELD; 
        this.searchTerms = Collections.EMPTY_LIST;
    }
    
    public SearchAction(List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms)
    {  
        super(VueResources.getString("searchgui.search"));
        runIndex();
        searchType = TYPE_QUERY;
        this.searchTerms = searchTerms;
    }
    
    public static int getGlobalResultsType() {
        return globalResultsType;
    }
    
    public void setEverything(boolean everything) {
        this.everything = everything;
    }
    
    public void setOperator(int op) {
        if (op == 0) setOperator(AND);
        else if (op == 1) setOperator(OR);
        else setOperator("bad op value: " + op);
    }
    public void setOperator(String operator) {
        if (operator != AND && operator != OR)
            throw new Error("invalid x-term-operator: " + operator);
        crossTermOperator = operator;
    }
    
    public void setBasic(boolean basic) {
        setBasic = basic;
    }
    
    public void setNoneIsSpecial(boolean set) {
        treatNoneSpecially = set;
    }
    
    // runs special index with only metadata
    public void setMetadataOnly(boolean set) {
        metadataOnly = set;
    }
    
    public void setTextOnly(boolean set) {
        textOnly = set;
    }
    
    private void runIndex()
    {
        if (true) {
            // This appears undeeded for at least standard searches... are there any searches that require this?
            // There may be synchronization issues, and most importantly, this.index is RESET TO A NEW
            // EMPTY INDEX when search results are displayed.
            return;
        }

        // Also, there is no control over this thread:

        /*-------------------------------------------------------
         * Disbaled SMF 2012-06-12 16:20.56 Tuesday SFAir.local
        
        new Thread() {
                public void run() {
                    if (DEBUG.SEARCH) Log.debug("Running index of SearchAction creation...");
                    
                    SearchAction.this.index = new edu.tufts.vue.rdf.RDFIndex();
                    
                    // //index.index(VUE.getActiveMap());
                    
                    if (searchLocationType == SEARCH_ALL_OPEN_MAPS) { 
                        if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("Searching all open maps...");
                        Iterator<LWMap> maps = VUE.getAllMaps().iterator();
                        while (maps.hasNext())
                            index.index(maps.next(),metadataOnly,everything,false);
                    }    
                    else { // default is SEARCH_SELECTED_MAP
                        if(VUE.getActiveMap()!=null){
                            index.index(VUE.getActiveMap(),metadataOnly,everything,true);
                        }
                    }
                    if (DEBUG.SEARCH) Log.debug("SearchAction index completed.");
                }
            }
        .start();

        -------------------------------------------------------*/
    }
    
    public void loadKeywords(String searchString) {
        
        tags = new ArrayList<String>();
        String[] parsedSpaces = searchString.split(" ");
        for(int i=0;i<parsedSpaces.length;i++) {
            tags.add(parsedSpaces[i]);
        }
        
    }
    

    private static final String AS_ONE_QUERY = "as-one-query(logical-and)";
    private static final String AS_MULTIPLE_QUERIES = "as-query-list(logical-or)";

    /*
     * this has been boiled down, tho still needs a rewrite -- way to complex
     */
    private List<Query> makeQueryFromCriteria
        (final List<VueMetadataElement> terms,
         final String resultType)
    {
        Query query = new Query();  // note: most of the time, this won't even be used...
        final List<Query> queryList;

        if (resultType == AS_ONE_QUERY)
            queryList = null;
        else 
            queryList = new ArrayList<Query>();
        
        this.textToFind = new ArrayList<String>(); // NOTE SIDE EFFECT
        actualCriteriaAdded = false;

        if (DEBUG.SEARCH) {
            Log.debug("makeQueryFromCriteria: " + Util.tags(resultType) + "; terms="
                      + (terms.size() == 1 ? terms.get(0) : Util.tags(terms)));
        }
        
        for (VueMetadataElement criteria : terms)
        {
            if (DEBUG.SEARCH) Log.debug("processing criteria: " + criteria);

            // [DAN] query.addCriteria(criteria.getKey(),criteria.getValue());
            final String[] statement = (String[])(criteria.getObject());

            if (setBasic != true) {  
                if (DEBUG.SEARCH) {    
                    Log.debug("query setBasic != true -- more than simple label-contains");
                    Log.debug("RDFIndex.VUE_ONTOLOGY+none=[" + RDFIndex.VUE_ONTOLOGY+"none]");
                }
                if (this.treatNoneSpecially && criteria.getKey().equals(RDFIndex.VueTermOntologyNone)) {
                    if (DEBUG.SEARCH) Log.debug("using manual textToFind: #none is special & no term (query will be empty)");
                    // [DAN] query.addCriteria("*",criteria.getValue(),statement[2]);
                    textToFind.add(criteria.getValue());
                    // The Query object won't even be used!
                }   
                else if (this.textOnly) {
                    if (DEBUG.SEARCH) Log.debug("using manual textToFind: textOnly=true (query will be empty)");
                    textToFind.add(criteria.getValue()); 
                    // The Query object won't even be used!
                }
                else {
                    query.addCriteria(criteria.getKey(), criteria.getValue(), "CONTAINS");  
                    // [DAN] query.addCriteria(criteria.getKey(),criteria.getValue(),statement[2]); // would be nice to be able to say
                    // [DAN] query condition here -- could do as subclass of VueMetadataElement? getCondition()? then a search
                    // [DAN] can be metadata too..
                    actualCriteriaAdded = true;
                }
            }
            else {
                query.addCriteria(RDFIndex.VUE_ONTOLOGY+Constants.LABEL, criteria.getValue(), "CONTAINS");
                // [DAN] query.addCriteria(RDFIndex.VUE_ONTOLOGY+Constants.LABEL,criteria.getValue(),statement[2]); // see comments just above
                actualCriteriaAdded = true;
            }
            if (queryList != null) {
                // Instead of continuing to add criteria to the current query, add this
                // single-criteria query to the list, and start next iteration with a fresh query.
                if (query != null) {
                    queryList.add(query);
                    query = new Query();
                }
            }
        }
        
        if (DEBUG.SEARCH) {
            // Note that most of the time, all the queries are empty and won't even be used!
            if (queryList != null) {
                int i = 0;
                for (Query q : queryList)
                    Log.debug("query " + (i++) + ": " + q.createSPARQLQuery());
            } else {
                Log.debug("query: " + (query == null ? "<NO QUERY>" : query.createSPARQLQuery()));
            }
        }

        if (queryList == null) {
            return query == null ? null : Collections.singletonList(query);
        } else
            return queryList;
    }

    /** create one query with multiple criteria */
    private Query createQuery_Logical_And(List<VueMetadataElement> terms)
    {
        return makeQueryFromCriteria(terms, AS_ONE_QUERY).get(0);
    }
    
    /** create one query for each criteria */
    private List<Query> createQueries_Logical_Or(List<VueMetadataElement> terms)
    {
        return makeQueryFromCriteria(terms, AS_MULTIPLE_QUERIES);
    }
    
    private void runSearch(final String where)
    {
        if (DEBUG.SEARCH) Log.debug("runSearch:\n\twhere=" + where + "\n\ttype=" + this.searchType + "\n\tterms=" + searchTerms);

        Query query = null;
        List<Query> queryList = null;
        
        if (searchType == TYPE_QUERY) {
            if (DEBUG.SEARCH) Log.debug("operator=" + crossTermOperator);
            // The was designed was if it's AND, we create a single query, but if OR,
            // we create multiple queries.
            if (crossTermOperator == SearchAction.AND) {
                query = createQuery_Logical_And(searchTerms);
            } else if (crossTermOperator == SearchAction.OR) {
                // Dan had said: "todo: AND in first query" -- what'd he mean by this?
                // What it seems we could be missing is to handle the OR case via a single query...
                queryList = createQueries_Logical_Or(searchTerms);
            }
        }

        /*-------------------------------------------------------*
         * SMF: WAS ALREADY COMMENTED OUT:
         *-------------------------------------------------------*
        // else // todo: for gathering text into its own list
        // // hmm seems to suggest a superclass for Query?
        // if(searchType == QUERY && crossTermOperation == OR)
        //     getSearchStrings();
        // edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().index(VUE.getActiveMap());
        *-------------------------------------------------------*/
        
        /*-------------------------------------------------------
         * SMF DISABLED SYNCHRONIED 2012-06-12 16:24.25 Tuesday SFAir.local */
        //synchronized(this.index) { // thread currently unused
        
        // The last working impl always created a new RDFIndex from scratch.  We normally still do this unless
        // the last index was from the same map and the map is in EXACTLY the same state it was when that index
        // was run.
        final edu.tufts.vue.rdf.RDFIndex index;

        if (true) { // old sync block
            //-----------------------------------------------------------------------------
            // I presume this is a means of just clearing the entire index? Shouldn't
            // it already be new, fresh, and empty?
            //this.index.remove(this.index); 
            //-----------------------------------------------------------------------------

            // TODO: indexing pulls list of all components from map -- for big maps we could speed this up by
            // pulling them once here (passing in to index(...)), and passing that same list instance further
            // down, where we iterate it many times...
            
            if (searchLocationType == SEARCH_ALL_OPEN_MAPS) { 
                if(DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("Searching all open maps...");
                index = new RDFIndex();
                for (LWMap map : VUE.getAllMaps()) {
                    if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("Adding to RDFIndex: " + map);
                    index.index(map, metadataOnly, everything, false);
                }
                if (DEBUG.SEARCH || DEBUG.RDF || DEBUG_LOCAL) Log.debug("Done indexing all maps.");
            } else { // default is SEARCH_SELECTED_MAP

                final LWMap map = VUE.getActiveMap();

                if (map == lastIndexedMap && map.getChangeState() == lastIndexedMapState) {
                    index = lastMapIndex;
                } else {
                    if (DEBUG.SEARCH || DEBUG.RDF || DEBUG_LOCAL) Log.debug("Indexing " + map);
                    index = new RDFIndex();
                    lastIndexedMap = map;
                    lastIndexedMapState = map.getChangeState();
                    lastMapIndex = index;
                    index.index(map, metadataOnly, everything, true);
                    if (DEBUG.SEARCH || DEBUG.RDF || DEBUG_LOCAL) Log.debug("Done indexing " + map);
                }
            }
        } // end synchronized block

        /*-------------------------------------------------------*/
        
        final List<List<URI>> resultSets = new ArrayList<List<URI>>();
        
        if (searchType == TYPE_FIELD) {
            List<URI> found = null;
            for(int i=0;i<tags.size();i++) {
                //found = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().search(tags.get(i));
                if(DEBUG.RDF || DEBUG.SEARCH) Log.debug("RDFIndex.searchAllResources...");
                found = index.searchAllResources(tags.get(i));
                if(DEBUG.RDF || DEBUG.SEARCH ) Log.debug("RDFIndex.searchAllResources completed.");
                if (found != null)
                    resultSets.add(found);
            }
        }
        else if (searchType == TYPE_QUERY) {
            //System.out.println("query result " + index.search(query) + " for query " + query.createSPARQLQuery());
            
            if (actualCriteriaAdded && crossTermOperator == AND) {    
                resultSets.add(index.search(query));
            }
            else if (actualCriteriaAdded && crossTermOperator == OR) {
                for (Query q : queryList)
                    resultSets.add(index.search(q));
            }
            
            boolean firstFinds = true;
            // This textToFind hack and it's creator should both be tossed into the realms of hell to burn for eternity.
            if (textToFind.size() != 0) {
                List<URI> found = null;
                Iterator<String> textIterator = textToFind.iterator(); 
                while(textIterator.hasNext())                {
                    String text = textIterator.next();
                    if (DEBUG_LOCAL) { Log.debug("\n\n**********\n Searching all resources for: " + text ); }
                    found = index.searchAllResources(text);
                    if (crossTermOperator == OR || firstFinds == true) {
                        if (found != null)
                            resultSets.add(found);   
                        firstFinds = false;
                    }
                    else {
                        // note: this iterator should usually have only one element in this case
                        Iterator<List<URI>> findsIterator = resultSets.iterator();
                        while(findsIterator.hasNext()) {
                            List<URI> current = findsIterator.next();
                            Iterator<URI> alreadyFound = current.iterator();
                            List<URI> toBeRemoved = new ArrayList<URI>();
                            while(alreadyFound.hasNext()) {
                                URI currentURI = alreadyFound.next();
                                if(DEBUG_LOCAL) { Log.debug("already found " + currentURI + "," + text); }
                                if (!found.contains(currentURI)) {
                                    if(DEBUG_LOCAL) { Log.debug("scheduling uri to be removed: (text follows) " + currentURI + "," + text); }
                                    toBeRemoved.add(currentURI);
                                }
                            }
                          
                            Iterator<URI> removeThese = toBeRemoved.iterator();
                            while(removeThese.hasNext()) {
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
        
        final List<LWComponent> hits = new ArrayList<LWComponent>();
        final Collection<LWMap> maps = new HashSet(VUE.getAllMaps());

        // Merge all the result sets into one hit-list, making sure it only contains
        // hits from our maps (suggests that the index / search results could come from
        // multiple other maps that aren't even currently open?)  SMF 2012-06-24
        
        for (List<URI> hitList : resultSets) {
            if (hitList == null)
                continue;
            for (URI uri : hitList) {
                if(DEBUG_LOCAL) Log.debug("uri found - " + uri);
                
                // this logic is strange: it suggests we can't know or control if the index was run across a
                // single map or a set of maps, and thus have to double check the results here? Or is this to
                // guard against pollution in the SINGLETON VueIndexedObjectsMap, which might contain stuff from
                // multiple searches?  Christ -- tho RDFIndex does clear that map on indexing.  This is a
                // roundabout way of returning LWComponent results via a side effect to a separate map -- just
                // so we can pass around URI Lists?  SMF 2012-06-24

                // TODO: we should be able to simply get rid of VueIndexedObjectsMap...
                
                final LWComponent c = (LWComponent) edu.tufts.vue.rdf.VueIndexedObjectsMap.getObjectForID(uri);
                // Your standard (non RDF?) search wouldn't need this separate map of big URI's...
                if (c != null) {
                    final LWMap cMap = c.getMap();
                    if (cMap != null && maps.contains(cMap))
                        hits.add(c);
                }
                //else if(r != null && (r.getMap() !=null) && maps.contains(r.getMap()))
            }
        }
        
        // System.out.println("VUE Object Index: " + edu.tufts.vue.rdf.VueIndexedObjectsMap.objs);
        
        //SwingUtilities.invokeLater(new Thread(){ // starting a thread, AND "doing it later"?
        //   public void run()  {
        displaySearchResults(hits);
        // }});
    }
    
    public String getName() {
        return VueResources.getString("searchgui.search");
    }
    
    public void setLocationType(String type)
    {
        searchLocationType = type;
    }
    
    private void performSearch(final String searchLocationType)
    {
        // // runIndex();
        //this.index = new edu.tufts.vue.rdf.RDFIndex();
        // // edu.tufts.vue.rdf.VueIndexedObjectsMap.clear();
        
        VUE.getSelection().clear();

        if (searchType == TYPE_FIELD) {
            revertSelections();
            loadKeywords(searchInput.getText());
        }
        runSearch(searchLocationType);               
    }   
    
    public void actionPerformed(ActionEvent ae)
    {
        if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("actionPerformed, ae=" + ae);
        
        tufts.vue.gui.GUI.activateWaitCursor();
        try {
            performSearch(searchLocationType);
        } catch (Throwable t) {
            Log.error("search error, src=(" + ae + ")", t);
        } finally {
            tufts.vue.gui.GUI.clearWaitCursor();
        }
    }

    private void displayAllMapsSearchResults(final List<LWComponent> comps)
    {
        final Iterator<LWMap> allOpenMaps = VUE.getAllMaps().iterator();
            
        final LWMap searchResultMap = new LWMap("Search Result #" + searchResultsMaps++);
            
        while(allOpenMaps.hasNext()) {
            Iterator<LWComponent> components = allOpenMaps.next().getAllDescendents(LWComponent.ChildKind.EDITABLE).iterator();   
            while(components.hasNext()) {
                LWComponent next = components.next();
                if(comps.contains(next)) {
                        
                    LWComponent duplicate = next.duplicate(); // why are we duplicating for this???  was this a bad cut/paste from the copy code?
                    LWComponent parent = next.getParent();
                    if(parent !=null && !comps.contains(parent)) {
                        if(parent instanceof LWNode) {    
                            duplicate.setLocation(parent.getLocation());
                        }
                         
                        if (LWNode.isImageNode(parent)) {
                            if(!comps.contains(parent)) {
                                LWComponent dup = parent.duplicate();
                                if(!(dup instanceof LWSlide) && !dup.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                                   && (!(dup.hasAncestorOfType(LWSlide.class))) )  
                                    searchResultMap.add(dup);
                            }
                        } else {    
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
    }
    
    private void produceSearchResultCopyMap(final List<LWComponent> comps) {
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
            
    }
        
    private void displaySearchResults(final List<LWComponent> comps)
    {
        if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("displaySearchResults: result set at start: " + Util.tags(comps));
        
        if (searchLocationType == SEARCH_ALL_OPEN_MAPS) {
            displayAllMapsSearchResults(comps);
            return;
        }

        if (resultsType == COPY_ACTION) {
            produceSearchResultCopyMap(comps);
            return;
        }
        
        revertGlobalSearchSelection();
        globalResultsType = resultsType;
        
        final Collection<LWComponent> imageNodes = new HashSet<LWComponent>();

        for (LWComponent c : comps) {
            // Note that this may be explicitly un-done below!
            if (c instanceof LWNode && LWNode.isImageNode(c))
                imageNodes.add(((LWNode)c).getImage());
            
            if (c instanceof LWImage) {
                LWImage image = (LWImage) c;
                if (image.isNodeIcon() && image.getParent() != null)
                    imageNodes.add(image.getParent());
            }
            
            /*if(current.hasFlag(LWComponent.Flag.SLIDE_STYLE)) {
                    LWSlide slide = (LWSlide)current.getParentOfType(LWSlide.class);
                    images.add(slide);
                    //LWNode source  = ((LWNode)slide.getSourceNode());
                    //images.add(source);
                    IF(LWNode.isImageNode(source))
                         images.add(source.getImage());
            }*/
        }
        
        comps.addAll(imageNodes);
        
        if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("displaySearchResults: results with images: " + Util.tags(comps));
        
        if (resultsType == SELECT_ACTION || resultsType == CLUSTER_ACTION || resultsType == LINK_ACTION) {
            
            if (DO_NOT_SELECT_SLIDE_COMPONENTS) {
                // Don't select slides, or anything ON a slide?  Original logic (Dan) collected all SLIDE_STYLE
                // components for removal from consideration, (LWComonents that are on a slide) but then went
                // thru that collection and rejected all components that weren't actual LWSlides themselves,
                // defeating the purpose of removing the SLIDE_STYLE components...
                final List<LWComponent> slideContent = new ArrayList<LWComponent>();
                for (LWComponent c : comps) {
                    if (c.hasFlag(LWComponent.Flag.SLIDE_STYLE) || c instanceof LWSlide)
                        slideContent.add(c);
                }
                comps.removeAll(slideContent);
            }
            
            if (DO_NOT_SELECT_NESTED_IMAGES) {
                // todo: above we add in node images -- could refactor in light of that code
                final List<LWComponent> nesters = new ArrayList<LWComponent>();
                for (LWComponent c : comps) {
                    // Note: this appears to clear ALL nested items?
                    for (LWComponent nested : c.getAllDescendents()) {
                        if (comps.contains(nested))
                            nesters.add(nested);
                    }
                    if (c instanceof LWNode && LWNode.isImageNode(c)) {
                        // note that this UNDOES what we did above to
                        // explicitly add these in!
                        nesters.add(((LWNode)c).getImage());
                    }
                }
                comps.removeAll(nesters);
                // Old version:
                // Iterator<LWComponent> it2 = comps.iterator(); 
                // List<LWComponent> toNotBeSelected = new ArrayList<LWComponent>();
                // while(it2.hasNext()) {
                //     LWComponent next = it2.next();
                //     Iterator<LWComponent> nestedComponents = next.getAllDescendents().iterator();
                //     // Note: this appears to add ALL nested items??
                //     while(nestedComponents.hasNext()) {
                //         LWComponent nextNested = nestedComponents.next();
                //         if(comps.contains(nextNested))
                //             toNotBeSelected.add(nextNested);
                //     }
                //     if(next instanceof LWNode && LWNode.isImageNode(next))
                //         toNotBeSelected.add(((LWNode)next).getImage());
                // }
                // Iterator<LWComponent> dontSelect = toNotBeSelected.iterator();
                // while(dontSelect.hasNext()) {
                //     LWComponent removeThis = dontSelect.next();
                //     if(comps.contains(removeThis))
                //         comps.remove(removeThis);
                // }
            }
        }
        
        // also hide or show elements inside of groups when group is also
        // in the search results set
        if (resultsType == HIDE_ACTION || resultsType == SHOW_ACTION ) {
            final Collection<LWComponent> groupDescendants = new HashSet<LWComponent>(); // no duplicates
            
            for (LWGroup group : Util.typeFilter(comps, LWGroup.class))
                groupDescendants.addAll(group.getAllDescendents(LWComponent.ChildKind.EDITABLE));

            comps.addAll(groupDescendants);
        }
        
        globalResults = comps;

        if (resultsType == SELECT_ACTION || resultsType == CLUSTER_ACTION || resultsType == LINK_ACTION) {
            final List<LWComponent> toAdd = new ArrayList<LWComponent>(comps.size());
            for (LWComponent c : comps) {
                LWMap.Layer layer = c.getLayer();
                if(!(layer != null && layer.isHidden()) &&
                   !(c instanceof LWSlide) &&
                   !c.hasFlag(LWComponent.Flag.SLIDE_STYLE) &&
                   (!(c.hasAncestorOfType(LWSlide.class))))  
                    toAdd.add(c);
            }
            // Be sure to update the VUE selection in a SINGLE call -- not one element at a time,
            // which is needlessly slow:
            VUE.getSelection().add(toAdd);
        } else if (resultsType == HIDE_ACTION) {
            for (LWComponent c : comps) {
                // VUE-892 -- switch back to setFiltered (needs change in LWImage to work for image nodes, but this
                // will handle child nodes/images correctly in non image nodes)
                //it.next().setHidden(LWComponent.HideCause.DEFAULT);
                if(!(c instanceof LWSlide) &&
                   !c.hasFlag(LWComponent.Flag.SLIDE_STYLE) &&
                   !c.hasAncestorOfType(LWSlide.class) &&
                   !(c instanceof LWGroup) )  
                    c.setFiltered(true);
            }
        } else if (resultsType == SHOW_ACTION) {    
          
            if (AUTO_SHOW_NESTED_IMAGES) {    
                // checking all children of nodes in search results to see if they are images or image nodes to
                // be done: for select, possibly actually remove selection for any children of search results
                // also to be done: image or image node results should also show parents (but not non image
                // results)

                // TODO: THIS CODE IS REPEATED ELSEWHERE!  And semantically, applied / re-applied multiple times!
                
                final List<LWComponent> imageNodes2 = new ArrayList<LWComponent>();  
            
                for (LWComponent c : comps) {
                    for (LWComponent child : c.getAllDescendents()) {
                        if ( ( (child instanceof LWImage) || LWNode.isImageNode(child)) && !comps.contains(child) ) {
                            imageNodes2.add(child);
                        }
                    }
                }
                comps.addAll(imageNodes2);
            }
            
            //globalHides = // opposite of comps

            // todo: really, we want to do this on VISIBLE, not EDITABLE (e.g., even
            // filter locked objects), but we would need to change all other duplicate code
            // references, and then regression test.
            //Collection<LWComponent> allComps = tufts.vue.VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.VISIBLE);

            // todo: someday, pass in an application / map context -- don't want to be pulling active map here from global state
            // -- if this code is ever changed to threaded, the active map could change during the search!
          
            final Collection<LWComponent> allComps = tufts.vue.VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.EDITABLE);
            globalHides = new ArrayList();
            for (LWComponent c : allComps) {
                if (!comps.contains(c)) {
                    if (DEBUG_LOCAL) Log.debug("adding " + c.getLabel() + " to globalHides and hiding");
                  
                    // VUE-892 -- switch back to setFiltered (needs change in LWImage to work for image nodes, but this
                    // will handle child nodes/images correctly in non image nodes)
                    //comp.setHidden(LWComponent.HideCause.DEFAULT);
                    if (!(c instanceof LWSlide) && !c.hasFlag(LWComponent.Flag.SLIDE_STYLE)
                       && (!(c.hasAncestorOfType(LWSlide.class))) && !(c instanceof LWGroup) ) {
                            c.setFiltered(true); // SHOW/HIDE
                            globalHides.add(c);
                    }
                }
            }
            
        } else if (resultsType == HIDE_ACTION) {

            globalHides = comps;
            
        } else if (resultsType == CLUSTER_ACTION) {
            
            Cluster2Layout layout = new Cluster2Layout();
            layout.layout(VUE.getSelection());
            VUE.getUndoManager().mark(VueResources.getString("searchgui.cluster"));
            
        } else if (resultsType == LINK_ACTION) {
            
            // Create a new node, name it based on the search, and link the selected nodes to it.
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                LWSelection                      selection = VUE.getSelection();

                if (selection.size() > 0) {
                    Iterator<VueMetadataElement> criterias = searchTerms.iterator();
                    String                       operator = (crossTermOperator == OR ? " or " : " and ");
                    StringBuffer                 newNodeName = new StringBuffer();
                    LWNode                       newNode = new LWNode("New Node");
                    boolean                      firstTerm = true;

                    while (criterias.hasNext()) {
                        VueMetadataElement       criteria = criterias.next();
                        String                   key = criteria.getKey(),
                                                 value = criteria.getValue();

                        if (!firstTerm) {
                            newNodeName.append(operator);
                        } else {
                            firstTerm = false;
                        }

                        newNodeName.append(value);

                        if (!key.equals(RDFIndex.VueTermOntologyNone)) {
                            newNode.addDataValue(key, value);
                        }
                    }

                    Iterator<LWComponent>        selectionIter = selection.iterator();
                    List<LWComponent>            newComps = new ArrayList<LWComponent>(),
                                                 selectedNodes = new ArrayList<LWComponent>();

                    newNode.setLabel(newNodeName.toString());
                    newComps.add(newNode);

                    while (selectionIter.hasNext()) {
                        LWComponent              selectedComp = selectionIter.next();

                        if (selectedComp instanceof LWNode) {
                            LWLink               newLink = new LWLink(selectedComp, newNode);

                            selectedNodes.add(selectedComp);
                            newComps.add(newLink);
                        }
                    }

                    if (selectedNodes.size() > 0) {
                        LWMap                    activeMap = VUE.getActiveMap();
                        Rectangle2D              selectionBounds = selection.getBounds();
                        UndoManager              undoMgr = VUE.getUndoManager();

                        newNode.setCenterAt(selectionBounds.getCenterX(), selectionBounds.getCenterY());
                        activeMap.addChildren(newComps);
                        undoMgr.mark(VueResources.getString("searchgui.link"));
//                      tufts.vue.Actions.MakeCluster.doClusterAction(newNode, selectedNodes);
//                      undoMgr.mark(VueResources.getString("menu.format.layout.makecluster"));
//                      VUE.getActiveViewer().scrollRectToVisible(VUE.getSelection().getBounds().getBounds());  // must get bounds again after cluster action
                        VUE.getActiveViewer().scrollRectToVisible(selectionBounds.getBounds());
                        selection.add(newComps);
//                      tufts.vue.Actions.PushOut.act();
//                      undoMgr.mark(VueResources.local("menu.format.arrange.pushout"));
                    }
                }
            }});
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
        else if(type.equals(VueResources.getString("searchgui.link")))
            resultsType = LINK_ACTION;
        
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
                comp.setFiltered(false); // REVERT FILTER
        } 
    }
    
    public static void revertGlobalSearchSelection()
    {
        if(globalResultsType == SELECT_ACTION || globalResultsType == CLUSTER_ACTION || globalResultsType == LINK_ACTION)
          revertSelections(globalResults);
        if(globalResultsType == HIDE_ACTION)
          showHiddenComponents(globalResults);
        if(globalResultsType == SHOW_ACTION)
          showHiddenComponents(globalHides);
    }
    
    public static void revertGlobalSearchSelectionFromMSGUI()
    {
        if(globalResultsType == SELECT_ACTION || globalResultsType == CLUSTER_ACTION || globalResultsType == LINK_ACTION)
          revertSelectionsFromMSGUI(globalResults);
        if(globalResultsType == HIDE_ACTION)
          showHiddenComponents(globalResults);
        if(globalResultsType == SHOW_ACTION)
          showHiddenComponents(globalHides);
    }
	
}
