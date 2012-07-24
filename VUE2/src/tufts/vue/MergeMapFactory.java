
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

/*
 * MergeMapFactory.java
 *
 * @created 2012-07-17 
 * @author Scott Fraize
 * @author dhelle01 (original LWMergeMap)
 */

package tufts.vue;

import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

import edu.tufts.vue.compare.*;
import edu.tufts.vue.style.*;

import tufts.vue.LWComponent.Flag;
import tufts.vue.LWComponent.ChildKind;
import edu.tufts.vue.metadata.VueMetadataElement;


/**
 * A parameterized factory for constructing merged LWMaps.  Each factory can only be used to create
 * a single LWMap.  The constructed LWMap will contain LWComponent clientData of type
 * MergeMapFactory.class, which for the remainder of the runtime can be tested to see if the map is
 * a merge-map, or queried to view the generating params.
 *
 * Note that the "base map", if it is active (and not excluded) is always the first map we process,
 * which means any nodes found there will be the first encountered with a given merge key, and thus
 * will be the nodes that are duplicated to the final merge map.  Thus, the base-map also serves as
 * a kind of priority template map.
 *
 */

public class MergeMapFactory {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MergeMapFactory.class);

    public static final int THRESHOLD_DEFAULT = 20;
    
    private int nodeThresholdSliderValue = THRESHOLD_DEFAULT;
    private int linkThresholdSliderValue = THRESHOLD_DEFAULT;

    // these two bools represent 3 possible design configs -- would be better as single case tags:
    // all/similarity/difference

    /** default true -- include all the non-primary (base) maps */
    private boolean includeSecondaryMaps = true;
    /** default false  -- filter out nodes (keys) that exist on the base map */
    private boolean excludeNodesOnBaseMap = false;

    public final LWMap baseMap;
    public final boolean baseMapInactive;
    
    public final LWMap outputMap;
    
    /** Actual maps used to generate the most recent merge. **/
    public final List<LWMap> mapList;
    /** to declare inactive status of individual maps -- if empty or shorter, status defaults to active */
    public final List<Boolean> activeStatus;
    public final List<LWMap> activeMaps;
    
    private List<Double> nodeIntervalBoundaries;
    private List<Double> linkIntervalBoundaries;

    /** keysMerged will be used during construction to track what's already been merged */
    private final Map<Object,LWComponent> keysMerged = new HashMap();
    
    /** if we will be excluding all keys found on the base map from the output, this will be filled */
    private Set baseMapKeys;
    
    private boolean alreadyUsed = false;

    private static int CreationCount = 0;

    public MergeMapFactory(LWMap baseMap, List<LWMap> mapList, List<Boolean> actives) {
        this.baseMap = baseMap;
        this.mapList = new ArrayList(mapList);
        this.activeStatus = new ArrayList(actives);
        this.outputMap = new LWMap(getNextTitle());
        this.activeMaps = computeActiveMaps();
        boolean bma = false;
        for (LWMap m : activeMaps) if (m == baseMap) { bma = true; break; }
        this.baseMapInactive = !bma;
    }

    public LWMap createAsVoteMerge() { return createMergeMap(true); }
    public LWMap createAsWeightMerge() { return createMergeMap(false); }

    private LWMap createMergeMap(boolean asVote) {
        if (alreadyUsed)
            throw new Error(getClass() + ": already used");
        alreadyUsed = true;

        if (excludeNodesOnBaseMap && !includeSecondaryMaps)
            throw new Error("merge-map would have no contents in this configuration");

        if (asVote)
            createAggregateAndFillMap(VoteAggregate.class);
        else
            createAggregateAndFillMap(WeightAggregate.class);

        annotateMap(asVote);
        // Make sure to layout the new map 1st time so, for instance, any node-icons will be properly displayed:
        outputMap.layoutAndValidateNewMap();
        // Someday, VUE.displayMap should check for special clientData that
        // indicates if the map has had an initializing layout, and automatically
        // do one if a layout key isn't found.

        Log.info("created: " + outputMap + "; " + outputMap.getChild(0));

        baseMapKeys = null; // gc
        keysMerged.clear(); // gc
        for (LWMap m : activeMaps) m.setClientData(LinksCache.class, null); // gc
        
        // we only allow one creation per factory in case anyone ever wants
        // to pull the params out of this client data later.
        outputMap.putClientData(this);
        
        return outputMap;
    }

    public void         setFilterOnBaseMap(boolean doFilter) { includeSecondaryMaps = !doFilter; }
    
    public void         setExcludeNodesFromBaseMap(boolean doExclude) { excludeNodesOnBaseMap = doExclude; }
    
    public void         setNodeThresholdSliderValue(int value) { nodeThresholdSliderValue = value; }
    public int          getNodeThresholdSliderValue() { return nodeThresholdSliderValue; }
    
    public void         setLinkThresholdSliderValue(int value) { linkThresholdSliderValue = value; }
    public int          getLinkThresholdSliderValue() { return linkThresholdSliderValue; }
    
    public boolean      isBaseMapActive() { return !baseMapInactive; }
    
    
    public void         setNodeIntervalBoundaries(List<Double> nib) {
        if (DEBUG.MERGE) { Log.debug("nodeIntervals:"); tufts.Util.dump(nib); }
        nodeIntervalBoundaries = new ArrayList(nib);
    }

    public void         setLinkIntervalBoundaries(List<Double> lib) {
        if (DEBUG.MERGE) { Log.debug("linkIntervals:"); tufts.Util.dump(lib); }
        linkIntervalBoundaries = new ArrayList(lib);
    }
    public List<LWMap> getMapList() { return mapList; }
    
    private static String getNextTitle() {
        return "Merge Map " + (++CreationCount);
    }
    
    /** @return the list of currently active maps (the maps list adjusted by activeStatus)
     * Note that the baseMap is not special in this regard: in can be inactive and not in this list.*/
    private List<LWMap> computeActiveMaps()
    {
        if (activeStatus == null || activeStatus.size() == 0) {
            // no status list specified, thus, all maps are active:
            return mapList;
        }
        final List<LWMap> maps = getMapList();
        final List<LWMap> actives = new ArrayList(maps.size());
        final Iterator<Boolean> statusIter = activeStatus.iterator();

        for (LWMap map : maps) {
            if (statusIter.hasNext()) {
                if (statusIter.next().booleanValue())
                    actives.add(map);
                else
                    /* leave this map out */;
            } else {
                actives.add(map);
            }
        }
        return actives;
    }
    
    private void createAggregateAndFillMap(final Class clazz)
    {
        // Only one of these two will be set to non-null
        final VoteAggregate voteAggregate;
        final WeightAggregate weightAggregate;
        
        final ConnectivityMatrixList cms = new ConnectivityMatrixList();

        //-----------------------------------------------------------------------------
        // Create a connectivity matrix for each active map to be fed to the Aggregate
        //-----------------------------------------------------------------------------
        if (excludeNodesOnBaseMap) {
            // generate the key set that will be used to exclude any keys
            // that were found on the base map.
            this.baseMapKeys = hashMergeKeys(baseMap);
        }
        
        for (LWMap map : activeMaps) {
            // if (map != getBaseMap()) // TODO: check -- really add baseMap matrix if ignoring baseMap?
            // old comment had commented out check to skip baseMap...
            cms.add(new ConnectivityMatrix(map));
        }
        //-----------------------------------------------------------------------------
        // Create the desired aggregate
        //-----------------------------------------------------------------------------
        if (clazz == VoteAggregate.class) {
            double nodeThresh = (double) getNodeThresholdSliderValue() / 100.0;
            double linkThresh = (double) getLinkThresholdSliderValue() / 100.0;
            weightAggregate = null;
            voteAggregate = VoteAggregate.create(cms, nodeThresh, linkThresh);
        } else {
            weightAggregate = WeightAggregate.create(cms);
            voteAggregate = null;
        }
        //-----------------------------------------------------------------------------


        // The base-map is the first map we process, which means any nodes found there with a given
        // key will be the nodes that are duplicated to the final merge map.  Thus, the base-map
        // also serves as a kind of priority template map.
        
        if (excludeNodesOnBaseMap || baseMapInactive) {
            if (DEBUG.MERGE) Log.debug("excluding base-map nodes from merge; active=" + !baseMapInactive);
        } else
            mergeInNodes(baseMap, voteAggregate);
        
        if (includeSecondaryMaps) {
            // When we do NOT do this, it means we will look at the votes or weights from the
            // aggregate merge-key analysis of all maps, but only ever take nodes from the base map
            // -- nodes and links from all other maps will be ignored except for the presence of
            // their merge keys in the matrix.
            for (LWMap map : activeMaps) {
                if (map == baseMap) {
                    Log.info("    loop; skipping baseMap " + baseMap);
                } else {
                    mergeInNodes(map, voteAggregate);
                }
            }
        }
        
        // Ah: note that CHILDREN OF A MERGED NODE CAN BE ANYTHING... WE ONLY WANT THE TOP-LEVEL-CHILDREN!
        //final Collection<LWComponent> allMergedNodes = outputMap.getAllDescendents(ChildKind.PROPER);

        // Fetch all the top level children of the single default layer in the merge-map.  Note
        // we're about to add links, so be sure to pass down and iterate a copy of the live
        // list.
        final Collection<LWComponent> allMergedNodes = new ArrayList(outputMap.getChild(0).getChildren());
        
        for (LWComponent c : allMergedNodes) 
            if (isMergeSkipped(c)) Log.warn("unexpected content in merge results: " + c);
        
        Log.info("pre-link content: " + tufts.Util.tags(allMergedNodes));
        
        if (voteAggregate != null)
            installLinksForVotes(allMergedNodes, cms, voteAggregate);
        else
            installLinksAndStylesForWeights(allMergedNodes, cms, weightAggregate);
    }

    private static boolean isMergeSkipped(LWComponent c)
    {
        if (c == null || c.hasFlag(Flag.ICON))
            return true;
        
        final Class cc = c.getClass();
        // to NOT allow LWPortal, which is a subclass of LWNode
        return ! (cc == LWNode.class || cc == LWImage.class);
    }
    

    
    private void mergeInNodes(final LWMap sourceMap, final VoteAggregate voteAggregate)
    {
        Log.info("mergeInNodes;   adding map " + sourceMap
                 + (sourceMap == baseMap ? " (BASE-MAP)" : "")
                 + "; voter=" + voteAggregate);

        final boolean isVoting = (voteAggregate != null);
                             
        for (LWComponent srcNode : sourceMap.getAllDescendents(ChildKind.PROPER)) {
            if (isMergeSkipped(srcNode))
                continue;

            final Object mergeKey = getMergeKey(srcNode);
            if (mergeKey == null)
                continue;
            final LWComponent alreadyMerged = keysMerged.get(mergeKey);
            if (alreadyMerged != null) {
                annotateNodeSource(sourceMap, srcNode, alreadyMerged);
                continue;
            }
            //-----------------------------------------------------------------------------
            // weightAggregate was ignored here
            //-----------------------------------------------------------------------------
            // Note: UI is misleading: vote "style" does't just refer to coloring -- it
            // also refers to what goes in the map.
            //-----------------------------------------------------------------------------
            if (isVoting && !voteAggregate.isNodeVotedIn(mergeKey))
                continue;

            if (excludeNodesOnBaseMap && baseMapKeys.contains(mergeKey))
                ; // skip this srcNode
            else
                copyInNode(sourceMap, srcNode, mergeKey);
        }
    }

    /** duplicate the source, annotate with it's source, and copy it to the
     * output map, also recording the mergeKey in the keysMerged map */
    private void copyInNode(LWMap sourceMap, LWComponent sourceNode, Object mergeKey) {
        final LWComponent node = sourceNode.duplicate();
        annotateNodeSource(sourceMap, sourceNode, node);
        keysMerged.put(mergeKey, node);
        outputMap.addChild(node);
    }
    
    /** takes an already duplicated link, annotates it, and adds it to the map */
    private void copyInLink(ConnectivityMatrixList cms, Object headKey, Object tailKey, LWLink link) {
        annotateLinkSources(cms, headKey, tailKey, link);
        outputMap.addChild(link);
    }
        
    private void installLinksForVotes(
         final Collection<LWComponent> allMergedNodes,
         final ConnectivityMatrixList cms,
         final VoteAggregate voteAggregate)
    {
        // This often used to create a seprate link for each direction: I presume that
        // was a bug and it's been fixed.

        for (LWComponent head : allMergedNodes) {
            final Object headKey = getMergeKey(head);
            for (LWComponent tail : allMergedNodes) {
                if (head != tail) {
                    final Object tailKey = getMergeKey(tail);
                    if (voteAggregate.testAndConsumeOppositeLinkVote(headKey, tailKey)) {
                        // This link relationship appeared on enough maps to make the vote
                        final LWLink link = new LWLink(head, tail);
                        link.setArrowState(LWLink.ARROW_NONE); // default is tail
                        copyInLink(cms, headKey, tailKey, link);
                    }
                }
            }
        }
    }
    
    private void installLinksAndStylesForWeights(
         final Collection<LWComponent> allMergedNodes,
         final ConnectivityMatrixList cms,
         final WeightAggregate weightAggregate)
    {
        final List<Style> nodeStyles = new ArrayList<Style>();
        final List<Style> linkStyles = new ArrayList<Style>();
        for(int si=0;si<5;si++) 
            nodeStyles.add(StyleMap.getStyle("node.w" + (si +1)));
        for(int lsi=0;lsi<5;lsi++)
            linkStyles.add(StyleMap.getStyle("link.w" + (lsi +1)));

        for (LWComponent node : allMergedNodes) {
            // In using our new Resource-but-default-to-Label merge, it would still might be nice
            // if later label hits would hit the resource label as well.  They'd probably
            // have to map to the same index, however, which would make ConnectivityMatrix
            // much more complicated, as well as the code that uses it.
            
            final double score = weightAggregate.getPercentFound(node);
            final Style style = nodeStyles.get(getIntervalForNode(score)-1);
            final String styleColor = style.getAttribute("font-color");
            
            node.setFillColor(Style.hexToColor(style.getAttribute("background")));
            if (styleColor != null)
                node.setTextColor(Style.hexToColor(styleColor));
        }

        for (LWComponent head : allMergedNodes) {
            final Object headKey = getMergeKey(head);
            for (LWComponent tail : allMergedNodes) {
                if (head == tail)
                    continue;
                final Object tailKey = getMergeKey(tail);  
                // weight: the number of maps that reflect this connection.  Note that multiple
                // connections between the two node keys on the SAME map to NOT increase the
                // weight.
                final int weightAlpha = weightAggregate.getConnection(headKey, tailKey);
                if (weightAlpha <= 0)
                    continue;
                
                double score = 100 * weightAlpha / weightAggregate.getCount();
                // [DAN] are either of these ever happenning? If so, why? [SMF: if merge on 0 maps...]
                if (score > 100) score = 100; else if (score < 0) score = 0;
                final Style linkStyle = linkStyles.get(getIntervalForLink(score)-1);
                final LWLink link = new LWLink(head, tail);

                // If the revese (omega) connection exists, zero it out in aggregate so we don't
                // add another link for it.

                final int weightOmega = weightAggregate.getConnection(tailKey, headKey);
                if (weightOmega > 0 && includeSecondaryMaps) { // todo: flag check looks wrong: what's base-map filter have to do with this?
                    link.setArrowState(LWLink.ARROW_BOTH);
                    weightAggregate.setConnection(tailKey, headKey, 0);
                }
                link.setStrokeColor(Style.hexToColor(linkStyle.getAttribute("background")));
                link.setStrokeWidth(weightAlpha); // weight can never be > number of maps merged
                copyInLink(cms, headKey, tailKey, link);
            }
        }
    }

    private static Set hashMergeKeys(LWMap map) {
        final Set hashedKeys = new HashSet();
        for (LWComponent c : map.getAllDescendents(ChildKind.PROPER)) {
            if (!isMergeSkipped(c)) {
                final Object key = getMergeKey(c);
                if (key != null)
                    hashedKeys.add(key);
            }
        }
        return hashedKeys;
    }
    
    private int getIntervalForNode(double score)
    {
        int count = 0;
        for (Double d : nodeIntervalBoundaries) {
            if (score < d.doubleValue())
                return count;
            count++;
        }
        return 0;
    }
    
    private int getIntervalForLink(double score)
    {
        int count = 0;
        for (Double d : linkIntervalBoundaries) {
            if (score < d.doubleValue())
                return count;
            count++;
        }
        return 0;
    }
    
    private static Object getMergeKey(LWComponent c) { return edu.tufts.vue.compare.Util.getMergeProperty(c); }

    /* for LWComponent.putclientData */ private static final class Counter { int count = 0; }
    // Note that since clientData is runtime *instance* information, and not copied over on
    // duplication, we don't have to worry about cleaning these up / having them propagate.

    private void annotateNodeSource(LWMap sourceMap, LWComponent sourceNode, LWComponent newMergeNode)
    {
        annotate(new StringBuilder(annotationPartForMap(sourceMap)),
                 sourceNode,
                 newMergeNode);
    }

    private final StringBuilder _cachedBuilder = new StringBuilder();

    private void annotate(StringBuilder mapPart, LWComponent source, LWComponent target) {
        final StringBuilder anno = _cachedBuilder;

        anno.setLength(0);
        
        // a problem with putting id in a "natural" place such as after the map name is then we
        // can't search on "map-name/node-name" as ID is in way, as in "map-name/12345/node-name".

        // this string is designed based on the ease of search options it provides e.g.,
        // "i/some-map" will mean all images from that map, "some-map/bob" means any node whose
        // name begins with "bob" from some-map.

        final String id = source.getID();

        anno.append(id); 
        anno.append('/').append(source.getComponentTypeLabel().charAt(0));
        anno.append('/').append(mapPart);
        anno.append('/').append(source.getDisplayLabel()); // todo: truncate

        putMergeAnnotation(target, anno.toString());

        if (target instanceof LWImage || DEBUG.TEST) { // images have no rollovers to show the merge data
            
            // We could store a list of all the actual source nodes in the client data, and annotate
            // at the end.  That could even allow us to design a fancier "merge" that somehow lets us
            // combine the properties of the merged nodes, instead of the random "duplicate the 1st
            // found" method we have now.

             // FYI: the first time this method sees this component during a merge, it happens to
             // be the actual duplicate of source (unless it's a link), and it wont have any
             // counter set yet.  Each time we see it after that, source is another node with the
             // same merge-key, possibly from a different source map.  (BTW, the choice of which
             // merge-key matching node to use as the actual duplication source is somewhat random:
             // the merge process simply uses the first it comes across in the order we merge the
             // maps.)

            Counter counter = target.getClientData(Counter.class);
            if (counter == null)
                counter = target.putClientData(new Counter());
            counter.count++;
            final String notesAnno = String.format("[in:%d:%s]", counter.count, anno.toString());
            if (target.hasNotes()) {
                target.setNotes(target.getNotes()
                                + (counter.count > 1 ? "\n" : "\n----\n")
                                + notesAnno);
            } else
                target.setNotes(notesAnno);
        }
    }

    private static String annotationPartForMap(LWMap m) {
        final String name = m.getDisplayLabel();
        if (name.endsWith(".vue"))
            return name.substring(0, name.length()-4);
        else
            return name;
    }
    
    /** cache for list of LWLink's used as a clientData key */
    private static final class LinksCache extends ArrayList<LWLink> {
        static List<LWLink> getLinksList(LWMap map) {
            final List<LWLink> list = map.getClientData(LinksCache.class);
            return list == null ? new LinksCache(map) : list;
        }
        /**/private LinksCache(LWMap map) {
            // getDescendentsOfType actually generates a filtering Iterator on the whole list, not
            // a list of the actual types desired.
            for (LWLink link : map.getDescendentsOfType(LWLink.class))
                add(link);
            map.putClientData(this);
        }
    }
    // todo: above generic "ListCache" w/parameterized type could be handy

    /* Todo: this could be slow when merging large similar maps. It iterates every link in each and
    * every input map that has a connection bewteen the two input keys (there was an LWLink between
    * those two keys).  Possible changes: the call context could provide some of the source info,
    * and/or the matrix could store a list of links at the [head][tail] / [tail][head] connection
    * sites.  Or, we could do this once per map and create a new kind of matrix to hold it. Note
    * that this works differently than node annotation because we do not copy actual links: we
    * create new ones based on the presence of a link between merge-key nodes on the source
    * maps. */
    private void annotateLinkSources(final ConnectivityMatrixList sources, final Object key1, final Object key2, final LWLink newLink)
    {
     // final StringBuilder anno = new StringBuilder("#source:");
        final StringBuilder annoBuf = new StringBuilder("");
        final int tagPrefix = annoBuf.length();

        int matchCount = 0;

        for (ConnectivityMatrix matrix : sources) {
            
            if (matrix.getConnection(key1, key2) <= 0)
                continue;

            final LWMap map = matrix.getMap();

            annoBuf.setLength(tagPrefix);
            annoBuf.append(annotationPartForMap(map));

            final int mapPrefix = annoBuf.length();

            for (final LWLink link : LinksCache.getLinksList(map)) {
                       
                final LWComponent head = link.getHead(); // note: affected by pruning
                final LWComponent tail = link.getTail(); // note: affected by pruning
                       
                if (head == null || tail == null) {
                    // currently shouldn't be happening -- connectivity matrix only counts
                    // links with both, but do nothing, just in case
                } else {
                    final Object headMP = Util.getMergeProperty(head);
                    final Object tailMP = Util.getMergeProperty(tail);
                    final boolean matches =
                        (headMP.equals(key2) && tailMP.equals(key1)) ||
                        (headMP.equals(key1) && tailMP.equals(key2)) ;
                    // we ignore directionality on purpose
                    if (matches) {
                        annoBuf.setLength(mapPrefix);
                        annotate(annoBuf, link, newLink);
                        matchCount++;
                    }
                }
            }
        }
        if (matchCount > 1)
            newLink.setStrokeWidth(matchCount);
        else if (matchCount <= 0)
            Log.warn("no sources for: " + newLink);
    }
    
    private static void putMergeAnnotation(final LWComponent mergeComponent, final String annotation) 
    {
        // Note that this meta-data does NOT show up in the "Keywords" InspectorPane tab -- it's
        // just used in rollovers (would be better to show up somehow, uneditable, in Keywords,
        // however).  This data will also appear in the RDFIndex, and this can be searched on, tho
        // the user won't have a place to see where the hit came from except the rollover.
            
        // final VueMetadataElement vme = new VueMetadataElement();
        // // vme.setType(VueMetadataElement.OTHER); // impl has always ended up ignoring this
        // vme.setObject(annotation); // VME IMPL HAS ALWAYS OVERWRITTEN TYPE TO TAG WHEN DOING THIS
        // mergeComponent.getMetadataList().getMetadata().add(vme);
        
        mergeComponent.getMetadataList().getMetadata()
            .add(VueMetadataElement.createSourceTag(annotation));
    }
    

    private void annotateMap(boolean asVote)
    {
        final StringWriter buf = new StringWriter(64);
        final PrintWriter p = new PrintWriter(buf);

        p.println("Merge map: " + new Date());
        p.println("Merge property: " + edu.tufts.vue.compare.Util.getMergeProperty());
        p.println("Merge style: by " +  (asVote ? "vote" : "weight"));
        if (asVote) {
            p.printf("Node vote threshold: %d%%\n", nodeThresholdSliderValue);
            p.printf("Link vote threshold: %d%%\n", nodeThresholdSliderValue);
        } // could include weight intervals../
        p.println("Sources: " + activeMaps.size());
        p.println("Source: " + baseMap.getDisplayLabel() + " (base)");

        for (LWMap map : activeMaps) {
            if (map != baseMap)
                p.println("Source: " + map.getDisplayLabel());
        }

        outputMap.setNotes(buf.toString());
    }
        
    // [DAN] old method for recording source nodes code currently does not compile
    // (its commented out below) - needs adjustment to 
    // LWComponent from LWNode (for both LWImage and LWNode)
    // a relatively minor fix and probably
    // also needs to be stored in special metadata of a new type...
    // (since otherwise this info always appears in the notes and possibly
    // out of sight)
    // **new system is to use VueMetadataElement.OTHER (see below)**
    // public static final boolean RECORD_SOURCE_NODES = false;
    // edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
    // vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
    // vme.setObject("source: " + node.getMap().getLabel() + "," + sourceLabel);
    // c.getMetadataList().getMetadata().add(vme);
        
    // // todo: this should become only the default initialization method
    // // for interval boundaries -- in fact: implementing 4/1/2008
    // private void setIntervalBoundaries()
    // {
    //     nodeIntervalBoundaries = new ArrayList<Double>();
    //     for(int vai = 0;vai<6;vai++) {
    //         double va =  20*vai + 0.5;
    //        nodeIntervalBoundaries.add(new Double(va));
    //     } 
    //     linkIntervalBoundaries = new ArrayList<Double>();
    //     for(int vai = 0;vai<6;vai++) {
    //         double va =  20*vai + 0.5;
    //         linkIntervalBoundaries.add(new Double(va));
    //     } 
    // }
    
    public String toString() { return getClass().getSimpleName() + "=" + outputMap.getLabel(); }

}
