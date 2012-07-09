
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
import tufts.vue.gui.GUI.ComboKey;
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
 *
 * Some refactoring triage by S.Fraize 2012
 *
 */
public class SearchAction extends AbstractAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SearchAction.class);

    /** search domains */
    public static final String SEARCH_SELECTED_MAP = "<search-domain:active-map>";
    public static final String SEARCH_ALL_OPEN_MAPS = "<search-domain:ALL-OPEN-MAPS>";
    
    /** result-actions */
    public static final ComboKey RA_SELECT =  new ComboKey("searchgui.select");
    public static final ComboKey RA_SHOW =    new ComboKey("searchgui.show");
    public static final ComboKey RA_HIDE =    new ComboKey("searchgui.hide");
    public static final ComboKey RA_CLUSTER = new ComboKey("searchgui.cluster");
    public static final ComboKey RA_LINK =    new ComboKey("searchgui.link");
    public static final ComboKey RA_COPY =    new ComboKey("searchgui.copynewmap");
    
    /** can be fed directly to a JComboBox for localized display with constant internal keys */
    public static final ComboKey[] ResultActionTypes = {
        RA_SELECT, RA_SHOW, RA_HIDE, RA_CLUSTER, RA_LINK, RA_COPY
    };
    
    public static final String AND = "<and>";
    public static final String OR = "<or>";

    /** what's "MARQUEE" mean? in any case, don't change: not suppored as false */
    private final static boolean MARQUEE = true;
    
    // note: default for other nested nodes is currently to treat them as stand alone components
    // for purposes of search considering perhaps showing parent for any component found nested
    // within another as issue for dec19-2007 team meeting.
    private final static boolean AUTO_SHOW_NESTED_IMAGES = true;
    private final static boolean DO_NOT_SELECT_NESTED_IMAGES = true;

    /**
     * Not even sure how we'd make selecting slide-components work from a GUI perspective.
     * Note that if this is the case, we also shouldn't bother indexing any slide components.
     * Tho result types such as COPY may still be able to copy slides?
     */
    private final static boolean DO_NOT_SELECT_SLIDE_COMPONENTS = true;
    
    // private static final String TYPE_FIELD = "<search-type-FIELD>";
    // private static final String TYPE_QUERY = "<search-type-QUERY>";
    
    private final List<VueMetadataElement> searchTerms;
    //private final String searchType;
    
    private String crossTermOperator = AND;
    
    private List<String> tags;
    private static Collection<LWComponent> globalResults;
    private static Collection<LWComponent> globalHides;
    
    private JTextField searchInput;
    
    private String searchLocationType = SEARCH_SELECTED_MAP;
    
    //enable for show or hide (for now until GUI dropdown installed)pe
    private ComboKey resultAction = RA_SELECT;
    private static ComboKey globalResultsType = RA_SELECT;
    
    private static int searchResultsMaps = 1;
    
    private boolean setBasic = true;
    
    private List<String> textToFind = new ArrayList<String>();
    private boolean actualCriteriaAdded = false;
    
    private boolean treatNoneSpecially = false;
    private boolean textOnly = false;
    private boolean metadataOnly = false;
    
    private boolean everything = false;
    private boolean searchBtnClicked = false;

    private boolean waitCursorActivated;
    
    // Wierdly, SearchTextField does not use this constructor / FIELD type, but MetadataaSearchMainGUI does,
    // SOMEWHERE, but I'm not sure it's ever called...
    // public SearchAction(JTextField searchInput) {
    //     super(VueResources.getString("searchgui.search"));
    //     this.searchInput = searchInput;
    //     runIndex();
    //     //searchType = TYPE_FIELD; 
    //     this.searchTerms = Collections.EMPTY_LIST;
    //     Log.info("TYPE_FIELD SearchAction constructor", new Throwable("HERE"));
    // }
    
    public SearchAction(List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms)
    {  
        super(VueResources.getString("searchgui.search"));
        //searchType = TYPE_QUERY;
        this.searchTerms = searchTerms;
    }
    
    public static Object getGlobalResultsType() {
        return globalResultsType;
    }
    
    /** note: this bit is currently ignored -- we never index slides or slide content */
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
    // Note: the only thing we take out of the VME's these days are the values, and possibly the keys
    private List<Query> makeQueryFromCriteria
        (final List<VueMetadataElement> terms,
         final String queryType)
    {
        Query query = new Query();  // note: most of the time, this won't even be used...
        final List<Query> queryList;

        if (queryType == AS_ONE_QUERY)
            queryList = null;
        else 
            queryList = new ArrayList<Query>();
        
        this.textToFind = new ArrayList<String>(); // NOTE SIDE EFFECT
        actualCriteriaAdded = false;

        if (DEBUG.SEARCH) {
            Log.debug("makeQueryFromCriteria: " + Util.tags(queryType) + "; terms="
                      + (terms.size() == 1 ? terms.get(0) : Util.tags(terms)));
        }
        
        for (VueMetadataElement criteria : terms)
        {
            if (DEBUG.SEARCH) Log.debug("processing criteria: " + criteria);

            // [DAN] query.addCriteria(criteria.getKey(),criteria.getValue());
            // [SMF] final String[] statement = (String[])(criteria.getObject());

            if (setBasic != true) {  
                if (DEBUG.SEARCH) {    
                    Log.debug("query setBasic != true -- more than simple label-contains");
                    Log.debug("RDFIndex.VUE_ONTOLOGY+none=[" + RDFIndex.VUE_ONTOLOGY+"none]");
                }
                if (this.treatNoneSpecially && criteria.getKey().equals(RDFIndex.VueTermOntologyNone)) {
                    // Note that above is only place where treatNoneSpecially is tested
                    if (DEBUG.SEARCH) Log.debug("using manual textToFind: #none is special & no term (query will be empty)");
                    // [DAN] query.addCriteria("*",criteria.getValue(),statement[2]);
                    textToFind.add(criteria.getValue());
                    // The Query object won't even be used!
                }   
                else if (this.textOnly) {
                    // NOTE: this is the ONLY place "textOnly" is tested, and the result is redundant
                    // to the case above -- FACTOR OUT.  Note: this means that an EVERYTHING search
                    // is the same as a CATEGORY+KEYWORDS search.
                    if (DEBUG.SEARCH) Log.debug("using manual textToFind: textOnly=true (query will be empty)");
                    textToFind.add(criteria.getValue()); 
                    // The Query object won't even be used!
                }
                else {
                    query.addCriteria(criteria.getKey(), criteria.getValue(), "CONTAINS");
                    
                    // note above we're ignoring the Qualifier that exists in the VME object and
                    // using "contains", which is appropriate, as 2nd term VME's appear to be
                    // marked with STARTS_WITH as opposed to CONTAINS.  Same applies to the
                    // addCriteria below.
                    
                    // [DAN] query.addCriteria(criteria.getKey(),criteria.getValue(),statement[2]); // would be nice to be able to say
                    // [DAN] query condition here -- could do as subclass of VueMetadataElement? getCondition()? then a search
                    // [DAN] can be metadata too..
                    // [SMF] Now I see why statement[2] (VME object data) may have been ignored here -- it wasn't
                    // always being properly set in MetadataSearchMainGUI -- not that we currently have any
                    // GUI for this -- everything is always CONTAINS anyway (e.g., no STARTS_WITH).
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
                // Note that the only time we appear to add multiple criteria to a single query
                // is when doing "Labels" searches with operator AND.
                if (actualCriteriaAdded) {
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

    private static final String INDEX_WITH_WAIT_CURSOR = "<index-with-wait-cursor>";
    private static final String INDEX_SILENT = "<index-no-wait-cursor>";

    /**
     * @return an index over given search domain (e.g., the currently active map, or all open maps)
     */
    private RDFIndex getDomainIndex(final String domain)
    {
        if (DEBUG.SEARCH) Log.debug("getDomainIndex " + Util.tags(domain));

        if (domain == SEARCH_ALL_OPEN_MAPS) { 
            if (DEBUG.SEARCH) Log.debug("indexing all open maps...");

            this.waitCursorActivated = true;
            tufts.vue.gui.GUI.activateWaitCursor();
            
            final RDFIndex globalIndex = new RDFIndex();

            // TODO: do we really want to search amongst existing "Search Results" maps?

            for (LWMap map : VUE.getAllMaps()) {
                if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("adding to global index: " + map);
                final RDFIndex mapIndex = getIndexForMap(map, !this.metadataOnly, INDEX_SILENT);
                globalIndex.addMapIndex(mapIndex);
            }

            if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("done indexing all maps.");

            return globalIndex;
            
        } else {
            // default is SEARCH_SELECTED_MAP
            return getIndexForMap(VUE.getActiveMap(), !this.metadataOnly, INDEX_WITH_WAIT_CURSOR);
        }
    }


    /**
     * This will find a valid cached index for the given map, or create and populate a fresh
     * one. Note that this call can also have a crucial side effect: it will activate the global wait
     * cursor if indexing is begun, and this.waitCursorActivated must be checked later to see if it
     * should be cleared.
     */
    private RDFIndex getIndexForMap(final LWMap map, boolean include_LWC_fields, String activateWait)
    {
        final CachedIndex cache = map.getClientData(CachedIndex.class);
        final RDFIndex index;

        if (cache != null && cache.isStillValid(include_LWC_fields)) {
            if (DEBUG.SEARCH) Log.debug("found valid cached index " + Util.tags(cache));
            index = cache.index;
        } else {
            if (DEBUG.SEARCH || DEBUG.RDF) {
                if (cache != null) Log.debug("saw cached index " + Util.tags(cache) + "; but was invalid");
                Log.debug("indexing " + map + "...");
            }

            if (activateWait == INDEX_WITH_WAIT_CURSOR) {
                this.waitCursorActivated = true;
                tufts.vue.gui.GUI.activateWaitCursor(); 
            }

            // Todo: can't we get rid of the METAONLY flag by modifying the search itself to ignore
            // the specific URI keys?

            index = new RDFIndex();
            index.indexAdd(map, !include_LWC_fields, this.everything);
            // note: the "everything" is currently ignored during indexing -- e.g., we never index LWSlide content
            map.setClientData(CachedIndex.class, new CachedIndex(index, map, include_LWC_fields));
                
            if (DEBUG.SEARCH || DEBUG.RDF) Log.debug(" indexed " + map + ".");
        }
        
        return index;
    }

    private static class CachedIndex {
        final RDFIndex index;
        final LWMap map;
        final long mapChangeStateAtIndex;
        final boolean indexHad_LWC_fields;
        CachedIndex(RDFIndex i, LWMap m, boolean didIndex_LWC_fields) {
            index = i;
            map = m;
            mapChangeStateAtIndex = map.getChangeState();
            indexHad_LWC_fields = didIndex_LWC_fields;
        }
        boolean isStillValid(boolean want_LWC_fields) {
            return map.getChangeState() == mapChangeStateAtIndex && indexHad_LWC_fields == want_LWC_fields;
        }
    }


    
    
    // TODO: prevent empty searches from doing anything -- e.g., right now, they can match EVERYTHING...

    private Collection<LWComponent> runSearch(
                                              final edu.tufts.vue.rdf.RDFIndex index,
                                              final List<VueMetadataElement> terms)
    // add actualCriteraAdded & crossTermOperator as explicit inputs
    {
        if (DEBUG.SEARCH) {
            Log.debug("runSearch: term(s): " + (terms.size() > 1 ? terms.size() : terms));
            if (terms.size() > 1)
                Util.dump(terms);
        }

        // one of these two will be non-null, tho they will often be empty!
        final Query query;
        final List<Query> queryList;
        
        if (true /*searchType == TYPE_QUERY*/) {
            if (DEBUG.SEARCH) Log.debug("operator=" + Util.tags(crossTermOperator));
            // The was designed was if it's AND, we create a single query, but if OR,
            // we create multiple queries.

            // NOTE THAT A FREQUENT RESULT OF THESE createQuery CALLS IS TO SIDE-EFFECT FILL
            // "textToFind", and RETURN AN EMPTY QUERY or EMPTY QUERY-LIST.
            
            if (crossTermOperator == SearchAction.AND) {
                query = createQuery_Logical_And(terms);
                queryList = null;
            } else { // if (crossTermOperator == SearchAction.OR) {
                // Dan had said: "todo: AND in first query" -- what'd he mean by this?
                // What it seems we could be missing is to handle the OR case via a single query...
                query = null;
                queryList = createQueries_Logical_Or(terms);
            }
        }
        
        // Although we'll get multiple URI hits for single nodes with multiple matching fields, the
        // current QuerySolution's only contain the *content* of what was matched, not the keyword,
        // and even if we had the keyword, we don't currently have a use case where we'd do
        // anything with it, so to simplify things we just make the results a HashSet.  [ ED: We've
        // now added keywords to results, so we could examine them for interesting stats if we like,
        // e.g., a search for "mentor" (ClubZora test data set) might reveal that all hits happened
        // to occur on a field named "Role" ]
        
        final Collection<URI> results = new HashSet<URI>();

        // TYPE_QUERY appears to be used in most (all?) cases, which works differently than
        // TYPE_FIELD.  TYPE_FIELD, I *thought* I saw was used when we search just amongst
        // "labels", and probably other similar cases.  The code paths are more different than they
        // ought to be.  E.g., TYPE_FIELD will match the word "header" from the style on the master
        // slide -- I think that's an indexing issue -- so they can actually INDEX differently.
        // That is is being mediated by the "everything" flag.  Okay -- it turns out TYPE_FIELD is
        // never used -- was not deployed in a live codepath in MetadataSearchMainGUI.
        // 
        // SMF 2012-07 todo: could still refactor the hell out of what's left here. 
        //
        // if (searchType == TYPE_FIELD) {
        //     // Single search box case -- DOESN'T APPEAR TO BE DEPLOYED IN ANY ACTIVE VUE CODE -- SMF 2012
        //     if (DEBUG.Enabled) Log.info("TYPE_FIELD IN USE", new Throwable("HERE"));
        //     for (String tag : tags) {
        //         if (DEBUG.RDF || DEBUG.SEARCH) Log.debug("processing tag " + Util.tags(tag));
        //         results.addAll(index.searchAllResources(tag));
        //     }
        // } else if (searchType == TYPE_QUERY) {
        
        if (true) {

            //-----------------------------------------------------------------------------
            // THIS IS WHERE THE INDEX IS ACTUALLY ASKED TO DO SEARCHES:
            //-----------------------------------------------------------------------------
            
            if (actualCriteriaAdded) {
                // This code path appears only to be traveled when we NOT searching for "everything"
                if (textToFind.size() > 0) Log.error("CRITERIA + TEXT-TO-FIND! " + Util.tags(textToFind));
                if (DEBUG.SEARCH) Log.debug("Query objects were used, operator=" + crossTermOperator);
                if (crossTermOperator == AND) {
                    // In this case, the AND is built into the query via multiple SPARQL sub-statements:
                    results.addAll(index.search(query));
                } else if (crossTermOperator == OR) {
                    // In this case, the OR is handled by running multiple single-statement queries:
                    for (Query q : queryList)
                        results.addAll(index.search(q));
                } else {
                    // should never happen:
                    Log.error("UNHANDLED CROSS TERM OPERATOR " + Util.tags(crossTermOperator));
                }
            }

            if (actualCriteriaAdded && textToFind.size() > 0)
                throw new Error("I THOUGHT ACTUAL QUERY & TEXT-TO-FIND WERE EXCLUSIVE CASES");

            boolean firstTerm = true;
            for (String textInput : textToFind) {
                final String textTerm = textInput.trim();
                if (DEBUG.SEARCH) Log.debug("textToFind has " + Util.tags(textTerm));
                if (textTerm.length() > 0) {
                    if (firstTerm || crossTermOperator == SearchAction.OR)
                        results.addAll(index.searchAllValues(textTerm));
                    else if (crossTermOperator == SearchAction.AND)
                        results.retainAll(index.searchAllValues(textTerm));
                    else
                        Log.error("Unhandled operator: " + Util.tags(crossTermOperator));
                }
                firstTerm = false;
            }
        }
        return index.decodeVueResults(results);
    }

    public String getName() {
        return VueResources.getString("searchgui.search");
    }
    
    public void setLocationType(String type) {
        searchLocationType = type;
    }
    
    // private void performSearch(final String searchLocationType) {
    //     // // runIndex();
    //     //this.index = new edu.tufts.vue.rdf.RDFIndex();
    //     // // edu.tufts.vue.rdf.VueIndexedObjectsMap.clear();
    //     // if (searchType == TYPE_FIELD) {
    //     //     // Note: TYPE_FIELD doesn't currently appear to be deployed in any code -- SMF 2012
    //     //     revertSelections();
    //     //     loadKeywords(searchInput.getText());
    //     // }
    //     runSearch(searchLocationType);               
    // }   
    
    public void actionPerformed(ActionEvent ae)
    {
        if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("actionPerformed, ae=" + ae);
        
        VUE.getSelection().clear();
        
        waitCursorActivated = false;
        
        Collection<LWComponent> hits = null;

        try {
            final RDFIndex index = getDomainIndex(this.searchLocationType);
            // getIndex will have activated wait-cursor if new indexing took place
            hits = runSearch(index, this.searchTerms);
        } catch (Throwable t) {
            Log.error("search error, src=(" + ae + ")", t);
        } finally {
            if (waitCursorActivated) tufts.vue.gui.GUI.clearWaitCursor();
        }
        
        if (hits != null)
            displaySearchResults(hits, VUE.getSelection());
    }

    private void displaySearchResults(final Collection<LWComponent> comps, final LWSelection selection)
    {
        if (DEBUG.SEARCH) {
            Log.debug("displaySearchResults: loc=" + Util.tags(searchLocationType) + " resultAction=" + GUI.name(resultAction));
            if (DEBUG.RDF) {
                // note: in may cases now, DEBUG.RDF means "DEBUG.SEARCH.EXTRA"
                Log.debug("DSR: raw results: " + Util.tag(comps));
                Util.dump(comps);
            } else {
                Log.debug("DSR: raw results: " + Util.tags(comps));
            }
        }
        
        if (searchLocationType == SEARCH_ALL_OPEN_MAPS) {
            displayAllMapsSearchResults(comps);
            return;
        }

        if (resultAction == RA_COPY) {
            produceSearchResultCopyMap(comps);
            return;
        }
        
        revertGlobalSearchSelection();
        globalResultsType = resultAction;


        // If an LWIMage that is a node-icon is in the list, add it's parent LWNode, and vice-versa
        // (if an LWNode that has a node-icon is in the list, add it's node-icon LWImage).  I'm
        // guessing we do this so if we get a hit on a node-icon LWImage, it can hit parent LWNode
        // as we'd actually like -- don't know why the reverse case is done, however.
        // SMF 2012
        
        final Collection<LWComponent> imageAssoc = new HashSet<LWComponent>();

        for (LWComponent c : comps) {
            // Note that this may be explicitly un-done below!
            if (c instanceof LWNode && LWNode.isImageNode(c))
                imageAssoc.add(((LWNode)c).getImage());
            
            if (c instanceof LWImage) {
                LWImage image = (LWImage) c;
                if (image.isNodeIcon() && image.getParent() != null)
                    imageAssoc.add(image.getParent());
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
        
        if (DEBUG.SEARCH) {
            Log.debug("displaySearchResults: +imageAssoc: " + Util.tag(imageAssoc));
            Util.dump(imageAssoc);
            //Log.debug("displaySearchResults: results with images: " + Util.tags(comps));
        }

        comps.addAll(imageAssoc);
        
        if (DEBUG.SEARCH) {
            Log.debug("displaySearchResults: results adjusted for node-icons:");
            Util.dump(comps);
        }
        
        if (resultAction == RA_SELECT || resultAction == RA_CLUSTER || resultAction == RA_LINK) {
            
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
        if (resultAction == RA_HIDE || resultAction == RA_SHOW ) {
            final Collection<LWComponent> groupDescendants = new HashSet<LWComponent>(); // no duplicates
            
            for (LWGroup group : Util.typeFilter(comps, LWGroup.class))
                groupDescendants.addAll(group.getAllDescendents(LWComponent.ChildKind.EDITABLE));

            comps.addAll(groupDescendants);
        }
        
        globalResults = comps;

        if (resultAction == RA_SELECT || resultAction == RA_CLUSTER || resultAction == RA_LINK) {
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
            selection.setWithDescription(toAdd, "Search Results");
        } else if (resultAction == RA_HIDE) {
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
        } else if (resultAction == RA_SHOW) { 
          
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
                    if (DEBUG.SEARCH) Log.debug("adding " + c.getLabel() + " to globalHides and hiding");
                  
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
            
        } else if (resultAction == RA_HIDE) {

            globalHides = comps;
            
        } else if (resultAction == RA_CLUSTER) {
            
            final Cluster2Layout layout = new Cluster2Layout();
            layout.layout(selection);
            VUE.getUndoManager().mark(VueResources.getString("searchgui.cluster"));
            
        } else if (resultAction == RA_LINK) {
            
            // Create a new node, name it based on the search, and link the selected nodes to it.
            GUI.invokeAfterAWT(new Runnable() { public void run() {
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
        globalResultsType = resultAction;
        
        //VueToolbarController.getController().selectionChanged(VUE.getSelection());
        //VUE.getActiveViewer().requestFocus();
        VUE.getActiveViewer().grabVueApplicationFocus("search",null);
        VUE.getActiveViewer().repaint();

    }
    
    private void displayAllMapsSearchResults(final Collection<LWComponent> comps)
    {
        final LWMap searchResultMap = new LWMap("Search Result #" + searchResultsMaps++);
            
        for (LWMap map : VUE.getAllMaps()) {
            if (DEBUG.SEARCH) Log.debug("processing " + map);

            //---------------------------------------------------------------------------------------------------
            // The outer loop looks completely redundant here -- could just iterate comps checking for EDITABLE -- SMF
            //---------------------------------------------------------------------------------------------------

            for (LWComponent next : map.getAllDescendents(LWComponent.ChildKind.EDITABLE)) {
                
                if (comps.contains(next)) {

                    // Todo: could use copy-context to include
                    // links that exist between items in the set of hits.
                    
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
    
    private void produceSearchResultCopyMap(final Collection<LWComponent> comps) {
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
        
    
    public void revertSelections() {   
        revertGlobalSearchSelection();
    }
    
    // /*public:deprecating*/ private void revertSelections_does_nothing(List<LWComponent> toBeReverted)
    // {
    //     if (MARQUEE || toBeReverted == null)
    //         return;
    //     for (LWComponent c : toBeReverted) {
    //         if (MARQUEE == false) {
    //             // PROBLEM: only the LWSelection should be calling setSelected
    //             // (further problem: given that, that API should have restricted this)
    //             // Dan may have been trying to make use of just the selection halo's
    //             // alone, tho that's quite confusing from a UX perspective.
    //             c.setSelected(false);
    //         } else {
    //             // already done on click on VUE map
    //             //VUE.getSelection().clear();
    //         }
    //     } 
    // }
    // /*public*/ private static void revertSelectionsFromMSGUI(Collection<LWComponent> toBeReverted) {
    //     if (MARQUEE) {
    //         VUE.getSelection().clear();
    //         VUE.getActiveViewer().repaint();
    //         return;
    //     }
    //     if (toBeReverted == null)
    //         return;
    //     final Iterator<LWComponent> it = toBeReverted.iterator();
    //     while (it.hasNext()) {
    //         if(MARQUEE == false) {
    //             it.next().setSelected(false);
    //         } else {
    //             /*Thread t = new Thread() {
    //               public void run() {
    //               //VUE.getSelection().clear();
    //               }
    //               };
    //               try {        
    //               //SwingUtilities.invokeLater(t);
    //               //t.start();
    //               } catch(Exception e) {
    //               System.out.println("SearchAction - Exception trying to clear selection: " + e);
    //               }*/
    //         }
    //     } 
    // }
    
    public void setResultAction(ComboKey ra_key) {
        this.resultAction = ra_key;
    }
    
    public void setResultAction(Object ra_key) {
        if (ra_key instanceof ComboKey)
            setResultAction((ComboKey)ra_key);
        else
            Log.error("invalid result-action key: " + Util.tags(ra_key), new Throwable("HERE"));
    }
    public void setResultActionFromSaved(String savedString) {
        setResultAction(getResultActionFromSaved(savedString));
    }

    /**
     * All saved searches up till summer 2012 used the LOCALIZED string value of the result-action
     * type! That was/is a bug -- it only ever worked if we're in the same locale.
     *
     * RA_SELECT is returned as a default if the string cannot be understood.
     */
    public ComboKey getResultActionFromSaved(String savedString) {
        for (ComboKey ck : ResultActionTypes)
            if (ck.localized.equals(savedString))
                return ck;
        return RA_SELECT; // fallback default
    }

    /** clear the filter flag on previously filtered LWComponents */
    private static void showHiddenComponents(Collection<LWComponent> toBeReverted)
    {
        if (toBeReverted == null)
            return;

        for (LWComponent c : toBeReverted) {
            // VUE-892 -- switch back to setFiltered (needs change in LWImage to work for image nodes, but this
            // will handle child nodes/images correctly in non image nodes)
            //c.clearHidden(LWComponent.HideCause.DEFAULT);

            if (c instanceof LWSlide || c.hasFlag(LWComponent.Flag.SLIDE_STYLE) || c.hasAncestorOfType(LWSlide.class))
                ; // do nothing -- note: shouldn't these have been previously filtered out?
            else
                c.setFiltered(false); // Revert filter
        } 
    }
    
    public static void revertGlobalSearchSelection()
    {
        if (globalResultsType == RA_SELECT || globalResultsType == RA_CLUSTER || globalResultsType == RA_LINK)
            ; // does nothing: revertSelections_does_nothing(globalResults);
        else if (globalResultsType == RA_HIDE)
          showHiddenComponents(globalResults);
        else if (globalResultsType == RA_SHOW)
          showHiddenComponents(globalHides);
    }
    
    public static void revertGlobalSearchSelectionFromMSGUI()
    {
        if (globalResultsType == RA_SELECT || globalResultsType == RA_CLUSTER || globalResultsType == RA_LINK) {
            VUE.getSelection().clear();
            VUE.getActiveViewer().repaint();
            //revertSelectionsFromMSGUI(globalResults);
        }
        else if (globalResultsType == RA_HIDE)
            showHiddenComponents(globalResults);
        else if (globalResultsType == RA_SHOW)
            showHiddenComponents(globalHides);
    }
	
}
