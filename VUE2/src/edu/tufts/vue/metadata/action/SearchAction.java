
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
import tufts.vue.LWComponent.Flag;
import tufts.vue.LWMap.Layer;
import tufts.vue.gui.GUI;
import tufts.vue.gui.GUI.ComboKey;
import tufts.Util;

// todo: #data=foo, #content=bar, #key=, etc, should search  (or: search syntax: #data/Spanish ?)
// the entire DOMAINS of data-set data, resource meta-data, VUE keywords (if we keep that domain)
// #data/Region=East Coast would work as well, etc.  An EMPTY SEARCH in a domain would
// actually in fact generate a hit on ALL nodes that had ANYTHING in that domain --
// very useful for discovering where data is in a map.  Could even allow for
// type=node/link/group/slide/image

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

    //----------------------------------------------------------------------------------------
    // constants
    //----------------------------------------------------------------------------------------
    
    public static final ComboKey SEARCH_SCOPE_CURRENT_MAP = new ComboKey("searchgui.currentmap");
    public static final ComboKey SEARCH_SCOPE_ALL_OPEN_MAPS = new ComboKey("searchgui.allopenmaps");

    /** search-types: which kind of fields to include in the search (domains/name-spaces) Note that Cat+Key actually modifies the search query, not the index set */
    public static final SearchAction.SearchType
        SEARCH_EVERYTHING =      new SearchType("searchgui.searcheverything"),
        SEARCH_ONLY_LABELS =     new SearchType("searchgui.labels"),
        SEARCH_ONLY_KEYWORDS =   new SearchType("searchgui.keywords"), // "VUE" keywords, which means just the VueMetadataElements / Keywords Widget
        SEARCH_WITH_CATEGORIES = new SearchType("searchgui.categories_keywords"); // search keywords with specific field labels named to search

    /** result-actions -- will appear in combo-boxes in order of declaration.  */
    public static final SearchAction.ResultOp
        RA_SELECT =  new ResultOp("searchgui.select"),
        RA_SHOW =    new ResultOp("searchgui.show"),
        RA_HIDE =    new ResultOp("searchgui.hide"),
        RA_CLUSTER = new ResultOp("searchgui.cluster"),
        RA_LINK =    new ResultOp("searchgui.link"),
        RA_COPY =    new ResultOp("searchgui.copynewmap");
    
    /** AND/OR keys for setting the cross-term logical operation */
    public enum Operator { AND("searchgui.and"), OR("searchgui.or");
        public final String key, localized;
        Operator(String k) { key = k; localized = VueResources.local(key); }
        /** return localized value -- handy for feeding to a JComboBox */
        public String toString() { return localized; }
    };
    public static final Object[] AllLogicOps = { Operator.OR, Operator.AND };
    
    /** key that SearchAction uses to put clientData in the LWMap when it owns some filtering state */
    public static final Object RevertableState = "search.revertable.state";
    
    //----------------------------------------------------------------------------------------
    // global state
    //----------------------------------------------------------------------------------------
    
    private static Collection<LWComponent> FilteredBySearch;
    private static LWMap FilteredMap;
    private static ResultOp LastAction = RA_SELECT;
    private static int ResultMapCount = 1;

    //----------------------------------------------------------------------------------------
    // implementation
    //----------------------------------------------------------------------------------------
    
    private ComboKey searchScope = SEARCH_SCOPE_CURRENT_MAP;
    private Operator crossTermOperator = Operator.AND;
    private ResultOp resultAction = RA_SELECT;
    private final List<VueMetadataElement> searchTerms;
    
    private List<String> textToFind = new ArrayList<String>();
    private boolean setBasic = true;
    private boolean textOnly = false;
    private boolean everything = false; // no longer makes functional difference
    private boolean metadataOnly = false;
    private boolean treatNoneSpecially = false;
    
    private boolean actualCriteriaAdded;
    private boolean waitCursorActivated;
    
    public SearchAction(List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms, SearchType type)  {
        super(VueResources.getString("searchgui.search"));
        //searchType = TYPE_QUERY;
        this.searchTerms = searchTerms;
        if (type != null)
            setParamsByType(type);
    }
    public SearchAction(List<edu.tufts.vue.metadata.VueMetadataElement> searchTerms) {
        this(searchTerms, null);
    }
    
    public void fire(Object source, String sourceTag) {
        actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, sourceTag));
    }
    
    public void setLocationType(Object scope) {
        if (scope == SEARCH_SCOPE_CURRENT_MAP || scope == SEARCH_SCOPE_ALL_OPEN_MAPS)
            this.searchScope = (ComboKey) scope;
        else
            throw new IllegalArgumentException(GUI.name(scope));
    }
    
    public void setOperator(Operator op) {
        this.crossTermOperator = op;
    }
    
    /** set the internal parameters automatically via the SearchType-domain */
    public void setParamsByType(SearchType type)
    {
        setBasic = textOnly = metadataOnly = everything = false;

        // treatNoneSpecially is only from old versions of VUE that could do
        // a "category" search (as opposed to "category+keywords") --
        // todo: what this actually do when it checked for the #none type?
        treatNoneSpecially = false; 

             if (type == SEARCH_ONLY_LABELS)   setBasic = true;
        else if (type == SEARCH_ONLY_KEYWORDS) textOnly = metadataOnly = true;
        else if (type == SEARCH_EVERYTHING)    textOnly = everything = true;
        else//if(type == SEARCH_WITH_CATEGORIES)
            ; // default: all-false
    }
    
    /** note: everything bit is currently ignored -- we never index slides or slide content */
    public void setEverything(boolean set) { everything = set; }
    public void setBasic(boolean set) { setBasic = set; }
    public void setNoneIsSpecial(boolean set) { treatNoneSpecially = set; }
    public void setMetadataOnly(boolean set) { metadataOnly = set; }
    public void setTextOnly(boolean set) { textOnly = set; }
    
    public void setResultAction(ResultOp ra) { this.resultAction = ra; }
    /** for taking an input directly from a JComboBox where type is unknown */
    public void setResultAction(Object ra) {
        if (ra instanceof ResultOp)
            setResultAction((ResultOp)ra);
        else
            throw new IllegalArgumentException("not a ResultOp: " + Util.tags(ra));
    }
    public ResultOp getResultAction() {
        return this.resultAction;
    }
    
    public String getName() {
        return VueResources.getString("searchgui.search");
    }
    
    public static Object getGlobalResultsType() {
        return LastAction;
    }
    

    // public void loadKeywords(String searchString) {
    //     tags = new ArrayList<String>();
    //     String[] parsedSpaces = searchString.split(" ");
    //     for(int i=0;i<parsedSpaces.length;i++) {
    //         tags.add(parsedSpaces[i]);
    //     }
    // }
    
    //----------------------------------------------------------------------------------------
    // End of the API
    //----------------------------------------------------------------------------------------
    
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
    private static final String INDEX_NO_WAIT_CURSOR = "<index-no-wait-cursor>";

    /**
     * @return an index over given search scope (e.g., the currently active map, or all open maps)
     */
    private RDFIndex getIndexForScope(final ComboKey scope)
    {
        if (DEBUG.SEARCH) Log.debug("getScopeIndex " + Util.tags(scope));

        if (scope == SEARCH_SCOPE_ALL_OPEN_MAPS) { 
            if (DEBUG.SEARCH) Log.debug("indexing all open maps...");

            this.waitCursorActivated = true;
            tufts.vue.gui.GUI.activateWaitCursor();
            
            final RDFIndex globalIndex = new RDFIndex();

            // TODO: do we really want to search amongst existing "Search Results" maps?

            for (LWMap map : VUE.getAllMaps()) {
                if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("adding to global index: " + map);
                final RDFIndex mapIndex = getIndexForMap(map, !this.metadataOnly, INDEX_NO_WAIT_CURSOR);
                globalIndex.addMapIndex(mapIndex);
            }

            if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("done indexing all maps.");

            return globalIndex;
            
        } else { // default SEARCH_SCOPE_CURRENT_MAP
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
            
            if (crossTermOperator == Operator.AND) {
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
                if (crossTermOperator == Operator.AND) {
                    // In this case, the AND is built into the query via multiple SPARQL sub-statements:
                    results.addAll(index.search(query));
                } else if (crossTermOperator == Operator.OR) {
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
                    if (firstTerm || crossTermOperator == Operator.OR)
                        results.addAll(index.searchAllValues(textTerm));
                    else if (crossTermOperator == Operator.AND)
                        results.retainAll(index.searchAllValues(textTerm));
                    else
                        Log.error("Unhandled operator: " + Util.tags(crossTermOperator));
                }
                firstTerm = false;
            }
        }
        return index.decodeVueResults(results);
    }

    /**
     * Entry point for running the actual search.
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (DEBUG.SEARCH || DEBUG.RDF) {
            final Object s = ae.getSource();
            Log.debug("AE: "
                      + "srcTag=" + Util.tags(ae.getActionCommand())
                      + " src=" + (s instanceof java.awt.Component ? GUI.name(s) : Util.tags(s)));
        }

        final LWSelection selection = VUE.getSelection();
        final LWMap activeMap = VUE.getActiveMap();
        
        // selection.clear();
        // let the selection be replaced in one fell swoop: less UI flickering
        waitCursorActivated = false;
        
        Collection<LWComponent> hits = null;

        try {
            // getIndex will have activated wait-cursor if new indexing took place
            hits = runSearch(getIndexForScope(this.searchScope), this.searchTerms);
        } catch (Throwable t) {
            Log.error("search error, src=(" + ae + ")", t);
        } finally {
            if (waitCursorActivated) tufts.vue.gui.GUI.clearWaitCursor();
        }
        
        if (DEBUG.SEARCH) {
            Log.debug("results are in for scope " + Util.tags(searchScope) + "; willProcessAs: " + GUI.name(resultAction));
            if (true || DEBUG.RDF) {
                // note: in may cases now, DEBUG.RDF means "DEBUG.SEARCH.EXTRA"
                Log.debug("=raw-results:");
                Util.dump(hits);
            } else {
                Log.debug("raw-results: " + Util.tags(hits));
            }
        }

        if (hits == null) {
            // Should only happen on exception:
            Log.warn("hits is null back from RDFIndex");
            java.awt.Toolkit.getDefaultToolkit().beep();            
            return;
        }

        if (searchScope == SEARCH_SCOPE_ALL_OPEN_MAPS) {
            // Only one result-action exists for this case -- essentially (or is it exactly?): RA_COPY
            selection.clear();
            displayAllMapsSearchResults(hits);
        } else if (resultAction == RA_COPY) {
            selection.clear();
            produceSearchResultCopyMap(hits);
        } else {
            processResultsToMap(resultAction, hits, activeMap, selection);
        }
    }

    private void processResultsToMap(
        final ResultOp action,
        final Collection<LWComponent> hits,
        final LWMap map,
        final LWSelection selection)
    {
        revertSearchState(false);
        LastAction = resultAction;

        if (hits.isEmpty()) {
            selection.clear();
        } else {
            final Collection<LWComponent> toSelect = targetNodesForAction(action, hits, map);

            if (DEBUG.SEARCH) {
                // note: in may cases now, DEBUG.RDF means "DEBUG.SEARCH.EXTRA"
                if (toSelect == null) {
                    Log.debug("nothing returned for selection.");
                } else {
                    Log.debug("to-select:");
                    Util.dump(toSelect);
                }
            }

            if (action == RA_CLUSTER) {
                final Cluster2Layout layout = new Cluster2Layout();
                layout.layout(selection);
                VUE.getUndoManager().mark(VueResources.getString("searchgui.cluster"));
            } else if (action == RA_LINK) {
                
                // now need to check toAdd
                //if (selection.count(LWNode.class) > 0) {
                if (false) {
                    if (DEBUG.SEARCH) Log.debug("invokeLater " + LinkResultAction.class);
                    // why invoke later?
                    //GUI.invokeAfterAWT(new LinkResultAction(map, selection));
                    new LinkResultAction(map, selection).run();
                } else {
                    if (DEBUG.SEARCH) Log.debug("no LWNode's in selection: " + selection);
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
            
            // Be sure to update the VUE selection in a SINGLE call -- not one element at a time,
            // which is needlessly slow / causes map updates on every add.
            if (toSelect != null)
                selection.setWithDescription(toSelect, "*** Search Results ***"); // TODO: something descriptive here
            else
                selection.clear();
            
            //VueToolbarController.getController().selectionChanged(VUE.getSelection());
            //VUE.getActiveViewer().requestFocus();
        }
            

        map.notify(this, LWKey.Repaint);

        final MapViewer viewer = VUE.getActiveViewer();
        if (viewer != null) {
            viewer.grabVueApplicationFocus("search", null);
            // This repaint should help in the case the active focal isn't a map / serves as backup
            viewer.repaint();
        }
    }

    /** We must use PROPER (or ANY) to see node icons (EDITABLE leaves out node-icons), ANY would
     * include slides, which we don't want. */
    private static final LWComponent.ChildKind SearchRelevant = LWComponent.ChildKind.PROPER;
    
    private static final int TARGETING_DENIED = Flag.FILTERED.bit
        | Flag.LOCKED.bit
        // | Flag.ICON.bit // handling separately
        // We shouldn't ever see any these, but just in case:
        | Flag.STYLE.bit // should never see unless index is bad (e.g., indexed ChildKind.ANY v.s. PROPER)
        | Flag.DATA_STYLE.bit // should never see unless index is bad
        | Flag.SLIDE_STYLE.bit // should never see unless index is bad
        | Flag.INTERNAL.bit // should never see
        | Flag.DELETED.bit  // should never see
        | Flag.PRUNED.bit; // should never need (already hidden if this is set)
            
            
    /**
     * Take the given set of hits from the RDFIndex based search, and process them depending on the
     * the user-selected resulting action.  Any returned collection of LWComponents are items to
     * actually be selected, which can vary from the LWComponents that were registered as hits.
     * (E.g., a hit on a node-icon will actually select it's parent node).
     *
     * At the moment, this method will also take aciton in the cases of RA_HIDE/RA_SHOW by
     * setting the appropriate FILTERED flags.
     */
    private Collection<LWComponent> targetNodesForAction(final ResultOp action, final Collection<LWComponent> hits, final LWMap map)
    {
        if (DEBUG.SEARCH) Log.debug(Util.color(GUI.name(resultAction), Util.TERM_GREEN));

        // THE PROBLEM WITH NODE ICONS: To review this ancient and regretful hack: When a tufts.vue.Resource is
        // set on a node that appears to be a reference to image content, we want to show that content right in
        // the node.  To do this, we create an LWImage with an identical Resource, and add it as the first
        // child of that node.  There are hacks abound through VUE to deal with this.  These days, these
        // special LWImage's should also all have Flag.ICON set, a flag created for that purpose.  Note that it
        // is of course possible to have an LWImage with a DIFFERENT resource than a node, added as a child.
        // This is especially wierd if the node had an image-icon-node, but it was deleted -- the resulting
        // LWImage will look exacly like a node-icon, but it won't be.

        // In any case, if we are going to be SELECTING, we need to turn any hit on a node-icon image to a hit
        // on the node.  If we are going to be FILTERING, we need the reverse: to turn any filter on a node to
        // also filter the node-icon (or do we want it both ways then?).  We need to handle the filtering case
        // in two places: when HIDING, we filter what we've hit, when SHOWING, we filter everything else.  When
        // filtering everything else, we want all node-icons hit -- we can have that handled by requesting
        // ChildKind.PROPER descendents when we ask for everything else.

        // That said, there is something else we want: in cases of SELECT/LINK/CLUSTER, lets call them
        // TARGETING actions, we do not want to hit ANY nested content.  So a single pass to add nodes for
        // their hit node icons, follwed by a pass to remove all nested content should work there.  Also, in
        // the case of TARGETING actions, we only want to pick ChildKind.EDITABLE content (e.g., nothing on
        // hidden or locked layers).  [Note that if we're were messing with the RDFindex stuff, we could have
        // such searches initially traverse only ChildKind.EDITABLE in the first place, but RDFindex simply
        // looks at all ChildKind.PROPER nodes).

        final boolean doTargeting = (action == RA_SELECT || action == RA_LINK || action == RA_CLUSTER);
        final boolean doFiltering = (action == RA_HIDE || action == RA_SHOW);

        if (doFiltering == doTargeting) throw new Error("impossible");

        final Collection<LWComponent> targets = new HashSet<LWComponent>(hits.size());

        if (doTargeting) {
            // TARGETING: remove disallowed targets, and retarget node-icons to their nodes
            for (LWComponent c : hits) {
                // First, leave out anything even remotely hidden or locked:
                if (c.isHidden() || c.hasAnyFlag(TARGETING_DENIED))
                    continue;
                final LWMap.Layer layer = c.getLayer();
                if (layer == null || layer.isHidden() || layer.isLocked())
                    continue;
                // todo: what about being hidden via ancestors?
                if (c.hasFlag(Flag.ICON))
                    targets.add(c.getParent());
                else
                    targets.add(c);
            }
            // Now remove nested targets
            final List<LWComponent> nested = new ArrayList<LWComponent>();
            for (LWComponent c : targets) {
                if (c.atTopLevel())
                    continue;
                for (LWComponent ancestor : c.getAncestors()) {
                    if (targets.contains(ancestor)) {
                        nested.add(c);
                        break;
                    }
                }
            }
            targets.removeAll(nested);
            return targets;
        } else { 
            // FILTERING: add extra targets
            for (LWComponent c : hits) {
                targets.add(c);
                if (LWNode.isImageNode(c)) {
                    // also filter the node-icon                    
                    targets.add(c.getChild(0)); 
                } else if (c instanceof LWGroup) {
                    // If a search hit is on an actual LWGroup (e.g., notes hit), and we're
                    // FILTERING that group, we ALSO want to filter all the children.  Or, if we're
                    // exclusively SHOWING that group, we want to make sure to show all the
                    // children also.
                    targets.addAll(c.getAllDescendents(SearchRelevant));
                }
            }
            if (action == RA_HIDE) {
                hideComponents(map, targets);
            } else if (action == RA_SHOW) {
                // compute inverse of targets for this map:
                final Collection<LWComponent> allOnMap = map.getAllDescendents(SearchRelevant);
                final Collection<LWComponent> toHide = new ArrayList(32);
                
                for (LWComponent c : allOnMap)
                    if (!targets.contains(c))
                        toHide.add(c);
                
                hideComponents(map, toHide);
            } else
                throw new Error(action.toString());
        }
        return null;        
    }

    private static void hideComponents(LWMap map, Collection<LWComponent> toHide)
    {
        FilteredBySearch = toHide;
        FilteredMap = map;
        for (LWComponent c : toHide) {
            // VUE-892 -- switch back to setFiltered from setHidden (needs change in LWImage to work for image nodes, but this
            // will handle child nodes/images correctly in non image nodes)
            c.setFiltered(true);
        }
        map.setClientData(RevertableState, Integer.valueOf(toHide.size()));
    }

    private static void revertPreviouslyHiddenToVisible(boolean repaint)
    {
        if (FilteredBySearch == null) {
            Log.warn("nothing was previously hidden");
            return;
        }
        for (LWComponent c : FilteredBySearch)
            c.setFiltered(false);

        FilteredMap.setClientData(RevertableState, null); // removes property
        
        if (repaint) {
            // Note: this will NOT cause a viewer repaint if its current focal isn't the whole map
            FilteredMap.notify(SearchAction.class, LWKey.Repaint);
        }
        
        FilteredBySearch = null;
        FilteredMap = null;
    }

    private class LinkResultAction implements Runnable {

        final LWMap map;
        final LWSelection selection;

        // also incoming: searchTerms, crossTermOperator

        /** only meaningful if s.count(LWNode.class) > 0, otherwise nothing happens */
        LinkResultAction(LWMap m, LWSelection s) { map = m; selection = s; }

        /** Create a new node, name it based on the search, and link the selected nodes to it. */
        public void run() {

            if (DEBUG.SEARCH) Log.debug("LinkResultAction:run");
                
            final StringBuilder name = new StringBuilder();
            final LWNode centralNode = new LWNode("-central-node-");

            boolean firstTerm = true;

            for (VueMetadataElement criteria : SearchAction.this.searchTerms) {
                final String key = criteria.getKey();
                final String value = criteria.getValue();

                if (!firstTerm) {
                    name.append(' ');
                    name.append(crossTermOperator.localized);
                    name.append(' ');
                } else {
                    firstTerm = false;
                }
                name.append(value);
                if (!key.equals(RDFIndex.VueTermOntologyNone)) {
                    centralNode.addDataValue(key, value);
                }
            }

            final List<LWComponent> newComps = new ArrayList<LWComponent>();
            final List<LWNode> selectedNodes = new ArrayList<LWNode>();

            centralNode.setLabel(name.toString());
            newComps.add(centralNode);

            // Create links from each node in selection (each LWNode hit by the search)
            // to the new central "search" node.
            for (LWNode hitNode : Util.typeFilter(selection, LWNode.class)) {
                LWLink newLink = new LWLink(hitNode, centralNode);
                selectedNodes.add(hitNode);
                newComps.add(newLink);
            }

            if (selectedNodes.size() > 0) { // wierd test, given knowns, but used ot exist for the doClusterAction call
                final Rectangle2D selectionBounds = selection.getBounds();
                final UndoManager undoManager = map.getUndoManager();
                final MapViewer activeViewer = VUE.getActiveViewer();

                centralNode.setCenterAt(selectionBounds.getCenterX(), selectionBounds.getCenterY());
                map.addChildren(newComps);
                if (undoManager != null)
                    undoManager.mark(VueResources.getString("searchgui.link"));
                // @@.;tufts.vue.Actions.MakeCluster.doClusterAction(newNode, selectedNodes);
                // undoMgr.mark(VueResources.getString("menu.format.layout.makecluster"));
                if (activeViewer.getFocal() == map) {
                    // we check activeViewer focal just in case it might have changed to another map,
                    // or another component on this map, such as a group or a slide.
                    activeViewer.scrollRectToVisible(selectionBounds.getBounds());
                 // activeViewer.scrollRectToVisible(selection.getBounds().getBounds());  // [???] must get bounds again after cluster action
                }
                selection.setWithDescription(newComps, "*** Link Search ****");
                // tufts.vue.Actions.PushOut.act();
                // undoMgr.mark(VueResources.local("menu.format.arrange.pushout"));
            }
        }
    }
    
    private void displayAllMapsSearchResults(final Collection<LWComponent> comps)
    {
        final LWMap searchResultMap = new LWMap("Search Result #" + ResultMapCount++);
            
        for (LWMap map : VUE.getAllMaps()) {
            if (DEBUG.SEARCH) Log.debug("processing " + map);

            //---------------------------------------------------------------------------------------------------
            // The outer loop looks completely redundant here -- could just iterate comps checking for EDITABLE -- SMF
            //---------------------------------------------------------------------------------------------------

            for (LWComponent next : map.getAllDescendents(LWComponent.ChildKind.EDITABLE)) { // TODO: what kind?

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
        LWMap searchResultMap = new LWMap("Search Result " + ResultMapCount++);
            
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
    
    // public void revertSelections() {   
    //     revertGlobalSearchSelection();
    // }
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

    // TODO: should only need if from same map -- tie to an enabled Revert button
    private static void revertSearchState(boolean repaint) {
        if (LastAction == RA_HIDE || LastAction == RA_SHOW)
            revertPreviouslyHiddenToVisible(repaint);

    }
    
    public static void revertGlobalSearchSelection() {
        revertSearchState(true);
    }
    
    // TODO: should only need if from same map -- tie to an enabled Revert button
    public static void revertGlobalSearchSelectionFromMSGUI()
    {
        if (LastAction == RA_SELECT || LastAction == RA_CLUSTER || LastAction == RA_LINK) {
            VUE.getSelection().clear(); // todo: won't cause panner to repaint -- must not be listening to the selection?
            // revertSelectionsFromMSGUI(globalResults);
        }
        else if (LastAction == RA_HIDE || LastAction == RA_SHOW)
            revertPreviouslyHiddenToVisible(true);

        final MapViewer viewer = VUE.getActiveViewer();
        LWMap map = null;
        LWComponent focal = null;
        if (viewer != null) {
            map = viewer.getMap();
            focal = viewer.getFocal();
            if (focal != null && focal != map)
                focal.notify(SearchAction.class, LWKey.Repaint); 
            if (map != null)
                map.notify(SearchAction.class, LWKey.Repaint);
                
            viewer.repaint(); // shouldn't need this -- but just in case
                
            // Technically, what we really want to do is to issue a repaint through both to the
            // map and to the viewer FOCAL.  (as we don't generate events for ever setFiltered
            // call, and don't want to)

            // Note: could also consider handling this via a UserActionCompleted.
        }
    }


    /** localized enum type for the kind of search (e.g., labels, everything, etc). */
    public static final class /*enum*/ SearchType extends ComboKey {
        public static final Map<String,SearchType> KeyMap=new LinkedHashMap(), ValueMap=new HashMap();
        private SearchType(String s) { super(s, KeyMap, ValueMap); }
        public static Object[] All() {
            if (KeyMap.isEmpty()) throw new InitError(SearchType.class);
            return KeyMap.values().toArray();
        }
    }
    /** localized enum type for the desired result operation: e.g., select, show, hide, etc */
    public static final class /*enum*/ ResultOp extends ComboKey {
        public static final Map<String,ResultOp> KeyMap=new LinkedHashMap(), ValueMap=new HashMap();
        private ResultOp(String s)  { super(s, KeyMap, ValueMap); }
        public static Object[] All() {
            if (KeyMap.isEmpty()) throw new InitError(ResultOp.class);
            return KeyMap.values().toArray();
        }
        //public static final ResultOp NEW_OP = new ResultOp("my.new.op"); // this works -- would ensure calls to All would never be empty
    }


	
}
