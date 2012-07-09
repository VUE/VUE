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

package edu.tufts.vue.rdf;

import java.util.*;
import java.io.*;
import java.net.*;

import tufts.vue.DEBUG;
import tufts.vue.LWComponent;
import tufts.vue.LWPathway;
import tufts.vue.LWMap;
import tufts.vue.LWSlide;
import tufts.vue.VueResources;
import tufts.Util;
import tufts.vue.VueUtil;
import edu.tufts.vue.metadata.*;

import edu.tufts.vue.ontology.*;
import edu.tufts.vue.metadata.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.sparql.core.*;
import com.hp.hpl.jena.graph.*;

/** "ARQ - A query engine for Jena, implementing SPARQL" */
import com.hp.hpl.jena.query.*;

/**
 *
 * RDFIndex.java
 *
 * RDFIndex mainly makes use of Apache Jena for VUE map searching.  This RDF approach is overkill for
 * VUE's current search needs, but originally began as an effort to create larger, peristent, multi-map
 * indicies.  That effort was not completed.  The resulting implementation that uses this
 * class creates a new RDFIndex for every new search (over one map or multiple maps), and then runs
 * the search on that fresh index.
 *
 * We do get one very nice feature for free -- the ability to write out a full .rdf file containing
 * a set of map data, although the current impl below tosses out the the key names from OSID meta-data
 * -- just the values are written out, all called "property".
 *
 * Makes use of Apache Jena / Jena ARQ (SPARQL query engine)
 * -- todo: update that lib to latest version...
 *
 * If, long-term, we really want to keep using the RDF indexing / searching, we could
 * go ahead and just build the VUE meta-data system itself out of RDF properties.
 *
 * -- Scott Fraize 2012-July
 *
 * @author akumar03
 * @author Daniel J. Heller
 *
 */

// todo: could create domain for every repository based on class name impl.
// (As the fields will in fact vary based on the impl) -- e.g:
// for edu.tufts.osidimpl.repository.nytimes.Repository:
// http://edu/tufts/osidimpl/repository/nytimes#Creator, etc.

public class RDFIndex extends com.hp.hpl.jena.rdf.model.impl.ModelCom
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(RDFIndex.class);

    public static final int MAX_SIZE = VueResources.getInt("rdf.index.size");
    //public static final boolean AUTO_INDEX= VueResources.getBool("rdf.index.auto"); // never implememnted
    public static final String INDEX_FILE = VueUtil.getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file");
    public static final String ONT_SEPARATOR = "#";
    /** name-space used for all VUE node fields (e.g., label,notes) as well as all "proper" meta-data (not tufts.vue.Resource meta-data) */
    public static final String VUE_ONTOLOGY = Constants.ONTOLOGY_URL + ONT_SEPARATOR;

    // The naming convetion here is that a "namespace" has the # at the end, but a "prefix" does not.
    // E.g., a namespace is ready to just have a label/keyword appended, and a prefix might have further
    // URL path depth added to it first.
    private static final String VUE_GENERAL_NAMESPACE = VueResources.getString("metadata.vue.url") + ONT_SEPARATOR;
    private static final String VUE_OSID_PREFIX = "http://vue.tufts.edu/osid/"; // is adding to the path this way a reasonable RDF convention?
    private static final String VUE_OSID_UNKNOWN_NAMESPACE = VUE_OSID_PREFIX + "#";

    /** i.e.: http://vue.tufts.edu/vue.rdfs#none */
    public static final String VueTermOntologyNone = VUE_GENERAL_NAMESPACE + "none";

    /** If true, index slides and slide content.  Currently, all slide content is filtered out on the result side anyway. */
    private static final boolean INDEX_SLIDES = false;
    
    /** Include actual keyword names from tufts.vue.Resource properties as opposed to just putting all values under keyword contentPropertyOf.
     * These will be put in the a VUE_OSID namespace -- either determined from the osid, or put in a general unknown-osid namespace.
     * Todo: not exactly the right thing to do for pure web/http URLResources or local file Resources. */
    private static final boolean INDEX_VUE_RESOURCE_KEYWORDS = false;
    
    final com.hp.hpl.jena.rdf.model.Property _propertyNone = createProperty(VUE_GENERAL_NAMESPACE, "none");

    final com.hp.hpl.jena.rdf.model.Property idOf = createProperty(VUE_ONTOLOGY,Constants.ID);
    final com.hp.hpl.jena.rdf.model.Property labelOf = createProperty(VUE_ONTOLOGY,Constants.LABEL);
    final com.hp.hpl.jena.rdf.model.Property childOf = createProperty(VUE_ONTOLOGY,Constants.CHILD);
    final com.hp.hpl.jena.rdf.model.Property authorOf = createProperty(VUE_ONTOLOGY,Constants.AUTHOR);
    final com.hp.hpl.jena.rdf.model.Property colorOf = createProperty(VUE_ONTOLOGY,Constants.COLOR);
    final com.hp.hpl.jena.rdf.model.Property notesOf = createProperty(VUE_ONTOLOGY,Constants.NOTES);
    final com.hp.hpl.jena.rdf.model.Property contentPropertyOf = createProperty(VUE_ONTOLOGY,Constants.CONTENT_INFO_PROPERTY);
    final com.hp.hpl.jena.rdf.model.Property hasTag = createProperty(VUE_ONTOLOGY,Constants.TAG);

    /** will contain the URI's of everything indexed in the map, unless this is a global index,
     * in which case it will contain the merged URI's from all maps */
    private final Map<URI,LWComponent> vueComponentMap = new HashMap();

    /** a general default index -- available but unused feature up through summer 2012 */
    private static RDFIndex defaultIndex;
    
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
        Log.info("instanced from " + tufts.Util.tags(base)); // are we using this case?
    }

    public RDFIndex() {
        //super(com.hp.hpl.jena.graph.Factory.createDefaultGraph()); // createGraphMem for performance?
        super(com.hp.hpl.jena.graph.Factory.createGraphMem());
    }

    /** transpose URI results to LWComponets using our internal mapping to vue components */
    public List<LWComponent> decodeVueResults(Collection<URI> results) {
        final List<LWComponent> hits = new ArrayList<LWComponent>(results.size()+2);
        for (URI uri : results) {
            final LWComponent c = vueComponentMap.get(uri);
            if (c != null)
                hits.add(c);
            else
                Log.error("*** Internal error: couldn't find URI in results-index: " + uri + "; " + Util.tags(vueComponentMap));
        }
        return hits;
    }
                                              
    /** for creating multi-map / global indicies */
    public void addMapIndex(RDFIndex mapIndex) {
        vueComponentMap.putAll(mapIndex.vueComponentMap);
        super.add(mapIndex);
    }
    
    public void indexMap(LWMap map) {
        //this.index.remove(this.index); // I presume this is a means of just clearing the entire index?
        vueComponentMap.clear();
        removeAll();
        indexAdd(map);
    }
    
    public void indexAdd(LWMap map) {
        indexAdd(map, false, false);
    }
    
    // Todo: could change to take a LWComponent as the focal, tho creating and index for the tiny amount
    // of content on a slide is a bit overkill -- only do if easier than engaging our other DataTree
    // based search mechanism.
    public void indexAdd(final LWMap map, final boolean metadataOnly, final boolean searchEverything_IS_IGNORED)
    {
        if (DEBUG.Enabled && searchEverything_IS_IGNORED) Log.debug("Note: \"search-everything\" bit is now ignored.");
        // If we want slide content, change default index to always index everything -- will hardly make
        // a difference just adding slides, and they can be (are currently always) optionally filtered out later anyway.
        
        if (DEBUG.RDF) Log.debug("indexAdd: begin; freeMem=: "+Runtime.getRuntime().freeMemory());
        
        final com.hp.hpl.jena.rdf.model.Resource mapRoot = this.createResource(map.getURI().toString());
        
        if (DEBUG.RDF) Log.debug("index: create resource for map; freeMem=: "+Runtime.getRuntime().freeMemory());
        
        try {
            addProperty(mapRoot, idOf, map.getID());
            addProperty(mapRoot, authorOf, System.getProperty("user.name"));
            if (map.hasLabel())
                addProperty(mapRoot, labelOf,map.getLabel());

            if (DEBUG.RDF) Log.debug("index: added properties for map; freeMem="+Runtime.getRuntime().freeMemory());
            
            final Collection<LWComponent> searchSet = map.getAllDescendents();

            // final Collection<LWComponent> searchSet;
            // // We always filter out slide content anyway, so no point in allowing it un index
            // // Note that we really ought to search from the current viewer focal tho, so,
            // // if user was looking at a single slide with lots of content, the search
            // // bar would still do something meaninful.
            // if (searchEverything) {
            //     // E.g., this will search everything incuding Slides, and even the MasterSlide (which is a bug)
            //     // THIS IS A PROBLEM IN THAT A PARAMETERIZED INDEX IS NO LONGER CACHEABLE!
            //     searchSet = map.getAllDescendents(LWComponent.ChildKind.ANY);
            // } else {
            //     searchSet = map.getAllDescendents();
            // }
            
            for (LWComponent c : searchSet) {
                if (c instanceof LWPathway || c instanceof LWMap.Layer)
                    continue;
                if (!INDEX_SLIDES && c instanceof LWSlide)
                    continue;
                
                try {
                    load_VUE_component_to_RDF_index(c, mapRoot, !metadataOnly);
                } catch (Throwable t) {
                    Log.warn("indexing VUE component " + c, t);
                }

                if (size() > MAX_SIZE) {
                    Log.warn("Maximum fail-safe search capacity reached: not all nodes will be searchable. (See property rdf.index.size)");
                    break;
                }
            }    
            
            if (DEBUG.RDF) Log.debug("index: after indexing all components; freeMem="+Runtime.getRuntime().freeMemory());
            
        } catch(Exception ex) {
            Log.error("index", ex);
        }
        if(DEBUG.RDF) Log.debug("index: done -- size="+this.size());
    }

    
    public Collection<URI> searchWithSPARQL(final String queryString)
    {
        //if (DEBUG.SEARCH) Log.debug("SEARCH;  substring=" + Util.tags(substring) + " queryString:\n" + Util.tags(queryString));
        if (DEBUG.SEARCH) Log.debug("searchWithSPARQL; queryString:\n" + Util.tags(queryString));
        
        final Collection<URI> resultSet = new ArrayList<URI>();
        final com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);
        
        if (DEBUG.SEARCH) Log.debug("QF created " + Util.tag(query)
                                    + "; memory=" + Runtime.getRuntime().freeMemory()
                                    + "\n" + query.toString().trim().replaceAll("\n\n", "\n"));

        final QueryExecution qe = QueryExecutionFactory.create(query, this); // 2nd arg is for Model or for FileManager?
        if (DEBUG.SEARCH) Log.debug("created QEF " + qe + "; memory=" + Runtime.getRuntime().freeMemory());

        final ResultSet results = qe.execSelect();
        if (DEBUG.SEARCH) Log.debug("execSelect returned; memory=" + Runtime.getRuntime().freeMemory());
        
        while (results.hasNext())  {
            final QuerySolution qs = results.nextSolution();
            if (DEBUG.SEARCH) {
                final String qss = qs.toString().replaceAll("<http://vue.tufts.edu", "..."); // shorten debug output
                Log.debug("qSol " + String.format("%.140s%s", qss, qss.length() > 140 ? ("...x"+qss.length()) : ""));
            }
            if (false) {
                // debug debug all vars from query
                //Util.dumpIterator(qs.varNames());
                Iterator<String> vn = qs.varNames(); 
                while (vn.hasNext()) {
                    String v = vn.next();
                    Log.debug("\t" + Util.tags(v) + "=" + Util.tags(qs.get(v)));
                }
            }
            try {
                resultSet.add(new URI(qs.getResource("rid").getURI()));
            } catch (Throwable t) {
                Log.warn("handling QuerySolution " + qs, t);
            }
        }
        qe.close();
        return resultSet;
    }
                
            // This was tried only by searchAllValues: It appears to originally have been to
            // ignored certain keys, or maybe even values, but I can't see how it ever worked.
            // ACTUALLY, it might have been to ignore jena Resouce id's in the results, as at one
            // time I can see that was a field added to the index, tho it would have eliminated
            // other text values that happened to have '#' in them.
            //
            // try {
            //     if (substring != NO_KEYWORD) {
            //         // What the hell does this code do??  It's trying to examine the VALUE for
            //         // what looks like KEYWORD transformations...
            //         //final String fullKeyword = qs.getLiteral("keyword").toString();
            //         final String fullKeyword = qs.getLiteral("val").toString();
            //         // What cases is this code filtering out the result from?
            //         final int slashLocation = fullKeyword.indexOf("#");
            //         final int keywordLocation = fullKeyword.toString().toLowerCase().
            //             lastIndexOf(substring.toString().toLowerCase());
            //         if (keywordLocation <= slashLocation) {
            //             // ONE ACTUAL RESULT: IF WE ENCOUNTER ANY HIT VALUE STRINGS WITH '#' IN THEM,
            //             // A HIT IN THE STRING *BEFORE* THE '#" IS IGNORED!  Can't be right...
            //             Log.info("MYSTERIOUS FILTERING HAS OCCURRED on \"keyword\" " + Util.tags(fullKeyword), new Throwable("HERE"));
            //             continue;
            //         }
            //         // if (keywordLocation > slashLocation)
            //         //     resultSet.add(new URI(qs.getResource("resource").toString()));
            //     }
            //     //if (DEBUG.SEARCH) Log.debug("getURI: " + Util.tags(qs.getResource("rid").getURI()));
            //     resultSet.add(new URI(qs.getResource("rid").getURI()));
            //     //resultSet.add(new URI(qs.getResource("resource").toString()));
            // } catch (Throwable t) {
            //     Log.warn("handling QuerySolution " + qs, t);
            // }
    
    //private static final String NO_KEYWORD = "<no-keyword>";
    // private Collection<URI> searchWithQueryString(String queryString) {
    //     return searchWithSPARQL(NO_KEYWORD, queryString);
    // }
    
    /**
     * Note this does a search using *our* simple query object: edu.tufts.vue.rdf.Query, NOT
     * a com.hp.hpl.jena.query.Query.  Our Query object is really a SPARQL builder.
     */
    public Collection<URI> search(edu.tufts.vue.rdf.Query sparql_builder) {
        if (DEBUG.SEARCH) Log.debug("search; query=" + Util.tags(sparql_builder));
        final String sparqlQuery = sparql_builder.createSPARQLQuery();
        return searchWithSPARQL(sparqlQuery);
    }
    
    // private static final String DefaultVueQuery =
    //       "PREFIX vue: <"+VUE_ONTOLOGY+"> SELECT ?resource ?keyword WHERE { ?resource ?x ?keyword }";
    
    /** General search: search all values, ignoring keywords (a.k.a: search the values for every and any key) */
    public Collection<URI> searchAllValues(String substring)
    {
        if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("searchAllValues:   " + Util.tags(substring));

        // Newlines for diagnostic readability
        // This would be a the same as what we'd get from a single-criteria edu.tufts.vue.rdf.Query that had
        // not specific property it was looking for (just a value), if Query supported that.
        final String genericSubstringQuery =
            "PREFIX vue: <"+VUE_ONTOLOGY+">\n SELECT ?rid ?key ?val WHERE {\n\t?rid ?key ?val FILTER regex(?val, \""+substring+ "\", \"i\")\n}";

        // Note: the WHERE cause is what assigns local variable names to WHATEVER is in the RDF store
        // tuples -- e.g., we have the Jena Resource URI at position 0, the keyword/Jena Property at position 1, and
        // the value at position 2.  I suspect that's how it's always ordered in the jena model.
        // Note that we can order the results in the QuerySolution by differing the order
        // in the SELECT from the order in the WHERE.
        
        //if (DEBUG.SEARCH) Log.debug("searchAllResources " + Util.tags(keyword) + "  " + queryString);
        //return searchWithSPARQL(substring, genericSubstringQuery);
        return searchWithSPARQL(genericSubstringQuery);
    }
    
    public void save() { }
    public void read() { }
    
    /* was public "rdfize" */
    private void load_VUE_component_to_RDF_index(final LWComponent component, final com.hp.hpl.jena.rdf.model.Resource mapRoot) {
        load_VUE_component_to_RDF_index(component, mapRoot, true);
    }

    private static final boolean RDFIZE_COLOR = VueResources.getBool("rdf.rdfize.color");
    
    /**
     * Extract relevant data of interest from the given VUE component, loading it into a new
     * jena.rdf.model.Resource, and add that to the RDF property tree as a child of the given
     * mapRootResource
     */
    private void load_VUE_component_to_RDF_index
        (final tufts.vue.LWComponent component,
         final com.hp.hpl.jena.rdf.model.Resource mapRootResource,
         final boolean includeNodeData)
    {
        final URI uri = component.getURI();
        final com.hp.hpl.jena.rdf.model.Resource r = this.createResource(uri.toString());

        // We need to be able to look up the RDF URI result later to get back to the LWComponent.
        // Note it might be faster to keep a master index of all LWComponents created by sequential
        // ID (as this is a runtime need only) and then we wouldn't have create this map each time,
        // and we could look them up via the perfect hash (an array index).
        vueComponentMap.put(uri, component);
        
        if (includeNodeData) {
            //------------------------------------------------------------------
            // Load Node info (label, notes, etc) plus any tufts.vue.Resource
            // meta-data to the index.
            //------------------------------------------------------------------
                
            // addProperty(r,idOf,component.getID());

            if (component.hasLabel())
                addProperty(r, labelOf, component.getLabel());

            if (component.hasNotes())
                addProperty(r, notesOf, component.getNotes());

            if (RDFIZE_COLOR && component.getXMLfillColor() != null) 
                addProperty(r, colorOf, component.getXMLfillColor());

            final tufts.vue.Resource res = component.getResource();
            if (res != null) {
                String nameSpace = null;
                
                if (INDEX_VUE_RESOURCE_KEYWORDS) {
                    final String osidProp = res.getProperty("@osid.impl");
                    String osid = osidProp;
                    if (osid != null) {
                        if (osid.endsWith(".Repository")) {
                            osid = osid.substring(0, osid.length() - 11);
                            nameSpace = VUE_OSID_PREFIX + osid.substring(osid.lastIndexOf('.')+1) + "#";
                        } else {
                            // note: could use provider id...  probably even more unique than impl class
                            Log.warn("Couldn't understand @osid.impl: " + Util.tags(osidProp));
                        }
                        //nameSpace = osid.replace(".", "/") + "#";
                        // Need to look into what'd be more appropriate here.
                        // ?E.g., http://vue.tufts.edu/osid/nytimes#title
                        // ?E.g., http://vue.tufts.edu/osid/nytimes/vue.rdfs#title
                    }
                    if (nameSpace == null) {
                        nameSpace = VUE_OSID_UNKNOWN_NAMESPACE;
                    }
                }
                
                for (Map.Entry e : res.getProperties().entries()) {
                    final Object key = e.getKey();

                    if (tufts.vue.Resource.isInternalPropertyKey(key.toString())) {
                        // todo: would be better to keep these props in a differnet map
                        // and not have to filter them out everywhere. 
                        continue;
                    }
                    
                    final Object value = e.getValue();
                    if (value != null) {
                        final String strValue = value.toString();
                        if (strValue == null || strValue.length() <= 0) {
                            // as all of these properties will have the same key, there's
                            // no point in allowing us to check for presence of an empty value
                            continue;
                        }
                        if (INDEX_VUE_RESOURCE_KEYWORDS) {
                            //addProperty(r, createProperty(VUE_ONTOLOGY, key.toString()), strValue);
                            // This will make sure a separate property exists for every resource property
                            // key found, which would enable us to search on specific resource properties.
                            // (E.g., author, creator, etc).
                            if (!tufts.vue.Resource.isHiddenPropertyKey(key.toString())) {
                                addProperty(r, createProperty(nameSpace, key.toString()), strValue);
                            }
                        } else {
                            // Using contentPropertyOf means that all resource properties have the same keyword name,
                            // and thus we can never do a searchs such as "author=bob" amongst resource properties...
                            addProperty(r, contentPropertyOf, strValue);
                        }
                    }
                }
            }
        }

        if (DEBUG.SEARCH && DEBUG.RDF) Log.debug("processing " + component);
            
        addStatement(createStatement(r, childOf, mapRootResource));
            
        final List<VueMetadataElement> metadata = component.getMetadataList().getMetadata();
        for (VueMetadataElement vme : metadata) {
            if (DEBUG.SEARCH && DEBUG.RDF) Log.debug("scan " + vme);
            if (vme.getObject() != null) {
                // BAD SEMANTICS: we check "getObject" then just go ahead and check key & value?
                final String key = vme.getKey();
                final String strValue = vme.getValue();
                if (vme.getKey() == VueTermOntologyNone) {
                    // This is just an optimization, so checking object identity is okay (should be used that way)
                    if (strValue != null && strValue.length() > 0) {
                        // Optimization: this being a "none" term (no keyword), don't bother with empty values
                        addStatement(createStatement(r, _propertyNone, strValue));
                    }
                } else {
                    // todo: kind of waste to create/fetch these constantly: if we keep this RDF indexing,
                    // someday we could just go ahead and put the RDF property right in the VME object.
                    // (And pre-encode all keys and/or only allow encoded keys in all meta-data
                    // data-structures)  Note that in current superclass impl, createProperty will
                    // return the existing Property object if the name matches.
                        
                    // Note that we do NOT want to skip empty values, as we have a non-empty key, and a
                    // search for a key with an empty value might be a valid search type someday (if we had
                    // the UI to support it).
                        
                    final String encodedKey = getEncodedKey(key);
                    addStatement(createStatement(r, getPropertyFromKey(encodedKey), strValue));
                }
            } else {
                Log.warn(r + ": null element object: no statement");
            }
            //statement = this.createStatement(r,createPropertyFromKey(element.getKey()),element.getObject().toString());
            //addStatement(statement);
        }
    }
    
    public void addStatement(com.hp.hpl.jena.rdf.model.Statement statement) {
        if (size() < MAX_SIZE) {
            if (DEBUG.SEARCH && DEBUG.META) Log.debug("addStatement: " + statement);
            super.add(statement);
        } else {
            throw new RuntimeException("Size of index: "+size()+ " exceeds MAX_SIZE: "+MAX_SIZE);
        }
    }
    
    public void addProperty(com.hp.hpl.jena.rdf.model.Resource r, com.hp.hpl.jena.rdf.model.Property p,String value)  {
        if (size() <MAX_SIZE) {
            r.addProperty(p, value);
        } else {
            throw new RuntimeException("Size of index: "+size()+ " exceeds MAX_SIZE: "+MAX_SIZE);
        }
    }
    public static String getUniqueId() {
        return Constants.RESOURCE_URL+edu.tufts.vue.util.GUID.generate();
        // This might better be called something else, but leaving for now just in case of any
        // backward compatability issue.  These are permanently persisted in save files,
        // tho at the moment there's really no point in doing so.
        // Constants.NODE_URL+edu.tufts.vue.util.GUID.generate();
    }
    
    public static RDFIndex getDefaultIndex() {
        if(defaultIndex == null) {
            return createDefaultIndex();
        } else {
            return defaultIndex;
        }
    }
    
    // A concept of auto-indexing was never implemented -- SMF 2012-06-25
    // public void startAutoIndexing() {
    //     isAutoIndexing = true;
    // }
    // public void stopAutoIndexing() {
    //     isAutoIndexing = false;
    // }
    
    // don't see this called anywhere...
    public void regenerate() {
        Log.info("regenerate: before Indexing size: "+size());
        List stmtList  = listStatements().toList();
        for(int i = 0;i<stmtList.size();i++) {
            com.hp.hpl.jena.rdf.model.Statement statementI = (com.hp.hpl.jena.rdf.model.Statement)stmtList.get(i);
            for(int j = i+1;j<stmtList.size();j++) {
                com.hp.hpl.jena.rdf.model.Statement statementJ = (com.hp.hpl.jena.rdf.model.Statement) stmtList.get(j);
                if(compareStatements(statementI,statementJ)) {
                    remove(statementJ);
                }
            }
        }
        Log.info("regenerate: after Indexing size: "+size());
    }
    // don't see this called anywhere except above...
    private boolean compareStatements(com.hp.hpl.jena.rdf.model.Statement stmt1,com.hp.hpl.jena.rdf.model.Statement stmt2) {
             if (!stmt1.getSubject().toString().equals(stmt2.getSubject().toString()))
            return false;
        else if (!stmt1.getObject().toString().equals(stmt2.getObject().toString()))
            return false;
        else if (!stmt1.getPredicate().toString().equals(stmt2.getPredicate().toString()))
            return false;
        else
            return true;
    }

    /** This is overriden to add debugging only */
    @Override public com.hp.hpl.jena.rdf.model.Property createProperty(final String nameSpace, final String localName)
    {
        final com.hp.hpl.jena.rdf.model.Property property = super.createProperty(nameSpace, localName);
        
        if (DEBUG.SEARCH && DEBUG.RDF) {
            final String propName;
            if (property instanceof com.hp.hpl.jena.rdf.model.impl.PropertyImpl)
                propName = "PropertyImpl";
            else
                propName = property.getClass().getName();
            Log.debug("createProperty " + Util.tags(nameSpace)
                      + String.format("+%-18s= %s@%08X[%s]",
                                      Util.tags(localName), // note need extra padding for escape codes here:
                                      propName,
                                      System.identityHashCode(property),
                                      property.toString()));
        }
        return property;
    }

    // /** Auto-create the property if not already there -- [not possible: super.createProperty already does this] */
    // public com.hp.hpl.jena.rdf.model.Property findProperty(final String nameSpace, final String localName) {
    //     final com.hp.hpl.jena.rdf.model.Property property = super.getProperty(nameSpace, localName);

    //     if (property == null)
    //         return createProperty(nameSpace, localName);
    //     else
    //         return property;
    // }
    
    
    /** Get/create a property.  If the key isn't domain/ns qualified, default to the VUE_ONTOLOGY name space */
    public com.hp.hpl.jena.rdf.model.Property getPropertyFromKey(String key) {
        //if (DEBUG.SEARCH && DEBUG.RDF) Log.debug("createPropertyFromKey " + Util.tags(key));
        final com.hp.hpl.jena.rdf.model.Property p;
        final String words[] = key.split(ONT_SEPARATOR);
        if (words.length == 1) {
            p = createProperty(VUE_ONTOLOGY, key);
        } else if (words.length < 1) {
            // Is this case even possible? Empty key?
            throw new RuntimeException("createPropertyFromKey: The key format is wrong. key - "+key);
        } else {
            // Note this means anything after a *second* '#' in the string will be entirely ignored
            final String nameSpace = words[0] + ONT_SEPARATOR;
            p = createProperty(nameSpace, words[1]);
        }
        //if (DEBUG.SEARCH) Log.debug("created jena property " + Util.tags(p));
        return p;
    }

    /** Note that while VUE.java currently has code to call getDefaultIdex, it's never used.
     * This attemps to read a defeault index file.  SMF 2012-06-25 */
    private static RDFIndex createDefaultIndex() {
        defaultIndex = new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
        try {
            File indexFile = new File(INDEX_FILE);
            if(indexFile.exists()) {
                defaultIndex.read(new FileReader(indexFile),Constants.RESOURCE_URL);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return defaultIndex;
    }
    
    /**
     * E.g., lets say a key has the name "Clubhouse Name" -- this will turn it into "Clubhouse+Name"
     * or "Total Time Online (mm)" to "Total+Time+Online+%28mm%29"
     *
     * NOTE: tho in the second case, jena will let us create a Property at runtime with that name,
     * but if we attempt to export it, we get com.hp.hpl.jena.shared.InvalidPropertyURIException at
     * com.hp.hpl.jena.xmloutput.impl.BaseXMLWriter.splitTag(BaseXMLWriter.java:345), so apparently
     * URLencoder isn't a full-proof encoding strategy.  Oh, and also, "Clubhouse+Name" is converted
     * to prefix declaration that includes "...#Clubhouse+", and then just "Name" is used when
     * declaring the property value.
     *
     * Any qualifying namespace prefix, if present, is left untouched.
     */
    public static String getEncodedKey(final String key)
    {
        String encodedKey = key;
       
        try {
            final int ontEnd = key.indexOf(ONT_SEPARATOR);
            if (ontEnd != -1) {    
                final String prefix = key.substring(0, ontEnd + 1);
                final String name = key.substring(ontEnd + 1, key.length());
                final String encodedName = java.net.URLEncoder.encode(name, "UTF-8");
                if (name != encodedName) {
                    encodedKey = prefix + encodedName;
                    if (DEBUG.RDF) Log.debug("encoded " + Util.tags(key) + " to " + Util.tags(encodedKey));
                }
            } else {
                encodedKey = java.net.URLEncoder.encode(key, "UTF-8");
                if (DEBUG.RDF && encodedKey != key) Log.debug("encoded " + Util.tags(key) + " to " + Util.tags(encodedKey));
            }
        }
        catch (java.io.UnsupportedEncodingException e) {
            Log.warn("encoding key " + Util.tags(key), e);  
        }
        
        return encodedKey;
    }
    
    
}
